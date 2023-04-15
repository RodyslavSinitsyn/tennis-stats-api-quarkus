package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;
import org.rsinitsyn.dto.request.BaseStatsFilter;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.PlayerStatsFilters;
import org.rsinitsyn.dto.response.MatchRecordsDto;
import org.rsinitsyn.dto.response.PlayerMatchesDto;
import org.rsinitsyn.dto.response.PlayerStatsDto;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchResultRepo;

import static org.rsinitsyn.domain.MatchType.LONG;
import static org.rsinitsyn.domain.MatchType.SHORT;

@ApplicationScoped
@Transactional
public class TennisService {

    @Inject
    MatchResultRepo matchResultRepo;

    public List<String> getAllMatchesRepresentations() {
        Map<Match, MatchResult> groupedByMatch = matchResultRepo.streamAll()
                .sorted(Comparator.comparing(mr -> mr.getMatch().date))
                .collect(Collectors.toMap(MatchResult::getMatch, Function.identity(), (matchResult, matchResult2) -> matchResult));
        List<PlayerMatchesDto.PlayerMatchDetailsDto> matches = groupedByMatch.values().stream()
                .map(this::getMatchDetails)
                .toList();
        return matches.stream().map(PlayerMatchesDto.PlayerMatchDetailsDto::getRepresentation).collect(Collectors.toList());
    }

    public Match saveMatch(CreateMatchDto dto) {
        validateMatchDto(dto);
        Match match = new Match();
        match.type = dto.type();
        match.date = LocalDateTime.now();
        if (dto.tournamentInfo() != null) {
            Tournament.findByName(dto.tournamentInfo().name())
                    .ifPresentOrElse(tournament -> {
                        match.tournament = tournament;
                        match.stage = dto.tournamentInfo().stage().orElseThrow(() -> new TennisApiException("Stage not set", 400));
                    }, () -> {
                        match.stage = TournamentStage.FRIENDLY;
                    });
        }
        match.persist();

        saveMatchPlayer(match, dto.player(), dto.opponentPlayer());
        saveMatchPlayer(match, dto.opponentPlayer(), dto.player());

        return match;
    }

    private void validateMatchDto(CreateMatchDto dto) {
        int points = dto.type().getPoints();
        if (dto.player().score() < 0 || dto.opponentPlayer().score() < 0) {
            throw new TennisApiException("Score < 0", 400);
        }
        if (dto.player().score() > points || dto.opponentPlayer().score() > points) {
            throw new TennisApiException("Max score is " + points + " for type " + dto.type(), 400);
        }
        if (dto.player().score() == points && dto.opponentPlayer().score() == points) {
            throw new TennisApiException("Both can not have max points", 400);
        }
        if (dto.player().score() != points && dto.opponentPlayer().score() != points) {
            throw new TennisApiException("At least one player should rich max points", 400);
        }
    }

    public MatchResult saveMatchPlayer(Match match,
                                       CreateMatchDto.PlayerResultDto player,
                                       CreateMatchDto.PlayerResultDto opponent) {
        MatchResult matchResult = new MatchResult();
        matchResult.setMatch(match);
        matchResult.setScored(player.score());
        matchResult.setMissed(opponent.score());
        matchResult.setExtraRound(Math.abs(player.score() - opponent.score()) == 1);
        matchResult.setWinner(player.score() > opponent.score());
        matchResult.setPlayer(Player.findByName(player.name()));
        matchResult.setOpponent(Player.findByName(opponent.name()));

        matchResultRepo.persist(matchResult);
        return matchResult;
    }

    public Player savePlayer(String name, String firstName, String lastName, int age) {
        Player player = new Player();
        player.name = name;
        player.firstName = firstName;
        player.lastName = lastName;
        player.age = age;
        player.persist();
        return player;
    }

    public Player savePlayer(CreatePlayerDto dto) {
        Player player = Player.ofDto(dto);
        player.persist();
        return player;
    }

    public PlayerStatsDto getPlayerStats(String name, PlayerStatsFilters filtersDto) {
        var player = Player.findByName(name);
        List<MatchResult> filtered = player.matches
                .stream()
                .filter(matchResult -> getFilters(filtersDto).stream().allMatch(p -> p.test(matchResult)))
                .toList();
        return new PlayerStatsDto(
                name,
                filtersDto,
                getPlayerStatistic(filtered),
                Map.of(
                        SHORT, getPlayerStatistic(filtered.stream().filter(mr -> mr.getMatch().type.equals(SHORT)).toList()),
                        LONG, getPlayerStatistic(filtered.stream().filter(mr -> mr.getMatch().type.equals(LONG)).toList())
                ),
                filtered.stream().map(MatchResult::getOpponent)
                        .collect(Collectors.toSet())
                        .stream().collect(Collectors.toMap(
                                opponent -> opponent.name,
                                opponent -> Map.of(
                                        "ALL", getPlayerStatistic(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name)).toList()),
                                        SHORT.name(), getPlayerStatistic(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name) && mr.getMatch().type.equals(SHORT)).toList()),
                                        LONG.name(), getPlayerStatistic(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name) && mr.getMatch().type.equals(LONG)).toList()))))
        );
    }

    private List<Predicate<MatchResult>> getFilters(PlayerStatsFilters filters) {
        List<Predicate<MatchResult>> predicates = new ArrayList<>();
        if (StringUtils.isNotEmpty(filters.getOpponent())) {
            predicates.add(mr -> mr.getOpponent().name.equals(filters.getOpponent()));
        }
        if (StringUtils.isNotEmpty(filters.getTournament())) {
            predicates.add(mr -> Optional.ofNullable(mr.getMatch().tournament)
                    .map(tournament -> tournament.name.equals(filters.getTournament()))
                    .orElse(Boolean.FALSE));
        }
        if (filters.getStage() != null) {
            predicates.add(mr -> mr.getMatch().stage.equals(filters.getStage()));
        }
        return predicates;
    }

    private PlayerStatsDto.PlayerScoreStatsDto getPlayerStatistic(List<MatchResult> matches) {
        int wins = (int) matches.stream().filter(MatchResult::isWinner).count();
        int scored = matches.stream().mapToInt(MatchResult::getScored).sum();
        int missed = matches.stream().mapToInt(MatchResult::getMissed).sum();

        return PlayerStatsDto.PlayerScoreStatsDto.builder()
                .matches(matches.size())
                .wins(wins)
                .loses(matches.size() - wins)
                .winRate(divide(wins * 100, matches.size()))
                .pointsScored(scored)
                .avgPointsScored(divide(scored, matches.size()))
                .medianPointsScored(getMedianValue(matches, MatchResult::getScored))
                .pointsMissed(missed)
                .avgPointsMissed(divide(missed, matches.size()))
                .medianPointsMissed(getMedianValue(matches, MatchResult::getMissed))
                .pointsRate(divide(scored, missed))
                .overtimes((int) matches.stream().filter(MatchResult::isExtraRound).count())
                .build();
    }

    private int getMedianValue(List<MatchResult> matches,
                               ToDoubleFunction<? super MatchResult> valueExtractor) {
        var values = matches.stream()
                .mapToDouble(valueExtractor)
                .sorted()
                .toArray();
        return (int) new DescriptiveStatistics(values).getPercentile(50);
    }

    private double divide(int val, int divideOn) {
        return BigDecimal.valueOf(val)
                .divide(BigDecimal.valueOf(
                        NumberUtils.max(divideOn, 1)), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public PlayerMatchesDto getPlayerMatches(String name, PlayerStatsFilters filters, boolean growSort, boolean formatted) {
        Player player = Player.findByName(name);
        List<MatchResult> filtered = player.matches.stream()
                .filter(matchResult -> getFilters(filters).stream().allMatch(p -> p.test(matchResult)))
                .sorted(Comparator.comparing(mr -> mr.getMatch().date, Comparator.reverseOrder()))
                .toList();

        List<PlayerMatchesDto.PlayerMatchDetailsDto> playerMatchDetailsDtos = filtered.stream()
                .map(this::getMatchDetails)
                .sorted(Comparator.comparing(PlayerMatchesDto.PlayerMatchDetailsDto::getScoreDifference,
                        growSort ? Comparator.naturalOrder() : Comparator.reverseOrder()))
                .toList();

        if (formatted) {
            return new PlayerMatchesDto(
                    null,
                    playerMatchDetailsDtos.stream().map(PlayerMatchesDto.PlayerMatchDetailsDto::getRepresentation).toList());
        } else {
            return new PlayerMatchesDto(
                    playerMatchDetailsDtos,
                    null);
        }
    }

    private PlayerMatchesDto.PlayerMatchDetailsDto getMatchDetails(MatchResult mr) {
        return PlayerMatchesDto.PlayerMatchDetailsDto.builder()
                .matchType(mr.getMatch().type)
                .name(mr.getPlayer().name)
                .score(mr.getScored())
                .opponentName(mr.getOpponent().name)
                .opponentScore(mr.getMissed())
                .stage(mr.getMatch().stage.getDetails())
                .tournamentName(Optional.ofNullable(mr.getMatch().tournament).map(t -> t.fullName).orElse(""))
                .build();
    }

    public MatchRecordsDto getRecords(String playerName) {
//        Player.findByName(playerName);
        List<MatchResult> allMatches = matchResultRepo.listAll()
                .stream()
                .filter(mr -> StringUtils.isNotEmpty(playerName) ? mr.getPlayer().name.equals(playerName) : Boolean.TRUE)
                .toList();
        return new MatchRecordsDto(
                Map.of(
                        "ALL", getRecordListDto(filterByType(allMatches, SHORT, LONG)),
                        SHORT.name(), getRecordListDto(filterByType(allMatches, SHORT)),
                        LONG.name(), getRecordListDto(filterByType(allMatches, LONG))
                )
        );
    }

    private List<MatchResult> filterByType(List<MatchResult> matchResults, MatchType... types) {
        return matchResults.stream()
                .filter(mr -> Arrays.asList(types).contains(mr.getMatch().type))
                .toList();
    }

    private MatchRecordsDto.RecordListDto getRecordListDto(List<MatchResult> matchResults) {
        var playerToStats = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getPlayer))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getPlayerStatistic(e.getValue())));

        return MatchRecordsDto.RecordListDto.builder()
                .matches(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getMatches),
                                PlayerStatsDto.PlayerScoreStatsDto::getMatches)
                )
                .winRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getWinRate),
                                PlayerStatsDto.PlayerScoreStatsDto::getWinRate)
                )
                .wins(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getWins),
                                PlayerStatsDto.PlayerScoreStatsDto::getWins)
                )
                .loses(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getLoses),
                                PlayerStatsDto.PlayerScoreStatsDto::getLoses)
                )
                .pointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getPointsScored),
                                PlayerStatsDto.PlayerScoreStatsDto::getPointsScored)
                )
                .avgPointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getAvgPointsScored),
                                PlayerStatsDto.PlayerScoreStatsDto::getAvgPointsScored)
                )
                .pointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getPointsMissed),
                                PlayerStatsDto.PlayerScoreStatsDto::getPointsMissed)
                )
                .avgPointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getAvgPointsMissed),
                                PlayerStatsDto.PlayerScoreStatsDto::getAvgPointsMissed)
                )
                .pointsRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto.PlayerScoreStatsDto::getPointsRate),
                                PlayerStatsDto.PlayerScoreStatsDto::getPointsRate)
                )
                .build();
    }

    private MatchRecordsDto.RecordListDto.RecordDto getRecordDto(
            Map<Player, PlayerStatsDto.PlayerScoreStatsDto> playersStats,
            Comparator<? super PlayerStatsDto.PlayerScoreStatsDto> sortComparator,
            Function<? super PlayerStatsDto.PlayerScoreStatsDto, Object> valueExtractor) {

        Map<String, String> best = playersStats.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(sortComparator.reversed()))
                .limit(1)
                .collect(Collectors.toMap(e -> e.getKey().name, e -> String.valueOf(valueExtractor.apply(e.getValue()))));

        Map<String, String> worst = playersStats.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(sortComparator))
                .limit(1)
                .collect(Collectors.toMap(e -> e.getKey().name, e -> String.valueOf(valueExtractor.apply(e.getValue()))));

        return new MatchRecordsDto.RecordListDto.RecordDto(
                new MatchRecordsDto.RecordListDto.RecordValueDto(best.entrySet().stream().findFirst().orElseThrow().getKey(),
                        best.entrySet().stream().findFirst().orElseThrow().getValue()),

                new MatchRecordsDto.RecordListDto.RecordValueDto(worst.entrySet().stream().findFirst().orElseThrow().getKey(),
                        worst.entrySet().stream().findFirst().orElseThrow().getValue())
        );
    }

    public ByteArrayInputStream getPlayerStatsCsv(String name, PlayerStatsFilters filters) {
        PlayerStatsDto playerStats = getPlayerStats(name, filters);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "Тип матча", "Оппонент", "Игр", "Выиграно", "Проиграно", "Экстратаймов", "Процент побед", "Забито", "Пропущено",
                        "Поинт рейт", "Забит средн", "Пропущено средн", "Забито медиана", "Пропущено медиана"
                )
                .setNullString("-")
                .build();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter =
                     new CSVPrinter(new PrintWriter(out), format)) {

            csvPrinter.printRecord(statsToTokens("ALL", "ALL", playerStats.getOverallStats()));
            for (Map.Entry<MatchType, PlayerStatsDto.PlayerScoreStatsDto> entry : playerStats.getTypeStats().entrySet()) {
                csvPrinter.printRecord(statsToTokens(entry.getKey().name(), "ALL", entry.getValue()));
            }
            for (Map.Entry<String, Map<String, PlayerStatsDto.PlayerScoreStatsDto>> entry : playerStats.getVersusPlayersStats().entrySet()) {
                for (Map.Entry<String, PlayerStatsDto.PlayerScoreStatsDto> subEntry : entry.getValue().entrySet()) {
                    csvPrinter.printRecord(statsToTokens(subEntry.getKey(), entry.getKey(), subEntry.getValue()));
                }
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new TennisApiException("Fail to import data to CSV file", e, 500);
        }
    }

    private List<String> statsToTokens(String matchType,
                                       String opponent,
                                       PlayerStatsDto.PlayerScoreStatsDto dto) {
        List<String> res = new ArrayList<>();
        res.add(matchType);
        res.add(opponent);
        res.add(String.valueOf(dto.getMatches()));
        res.add(String.valueOf(dto.getWins()));
        res.add(String.valueOf(dto.getLoses()));
        res.add(String.valueOf(dto.getOvertimes()));
        res.add(String.valueOf(dto.getWinRate()));
        res.add(String.valueOf(dto.getPointsScored()));
        res.add(String.valueOf(dto.getPointsMissed()));
        res.add(String.valueOf(dto.getPointsRate()));
        res.add(String.valueOf(dto.getAvgPointsScored()));
        res.add(String.valueOf(dto.getAvgPointsMissed()));
        res.add(String.valueOf(dto.getMedianPointsScored()));
        res.add(String.valueOf(dto.getMedianPointsMissed()));
        return res;
    }

    @SneakyThrows
    public ByteArrayInputStream getPlayerStatsExcel(String name, BaseStatsFilter filter) {
        PlayerStatsDto playerStats = getPlayerStats(name, new PlayerStatsFilters(
                filter.getTournament(),
                filter.getStage(),
                ""
        ));

        var workbook = new XSSFWorkbook();
        var allStatsSheet = createSheet(workbook, "Все матчи");
        createHeaderRow(allStatsSheet);
        appendRow(allStatsSheet, 1, "ALL", "ALL", playerStats.getOverallStats());

        var typeSheet = createSheet(workbook, "Шорт Лонг");
        createHeaderRow(typeSheet);
        int rowCounter = 1;
        for (Map.Entry<MatchType, PlayerStatsDto.PlayerScoreStatsDto> typeEntry : playerStats.getTypeStats().entrySet()) {
            appendRow(typeSheet, rowCounter++, typeEntry.getKey().name(), "ALL", typeEntry.getValue());
        }

        for (Map.Entry<String, Map<String, PlayerStatsDto.PlayerScoreStatsDto>> opponentEntry : playerStats.getVersusPlayersStats().entrySet()) {
            var perPlayerSheet = createSheet(workbook, "Против " + opponentEntry.getKey());
            createHeaderRow(perPlayerSheet);
            int subCounter = 1;
            for (Map.Entry<String, PlayerStatsDto.PlayerScoreStatsDto> subTypeEntry : opponentEntry.getValue().entrySet()) {
                appendRow(perPlayerSheet, subCounter++, subTypeEntry.getKey(), opponentEntry.getKey(), subTypeEntry.getValue());
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new TennisApiException("Fail to import data to xlsx", e, 500);
        } finally {
            workbook.close();
        }
    }

    private Sheet createSheet(Workbook workbook, String text) {
        Sheet sheet = workbook.createSheet(text);
        for (int i = 0; i <= 13; i++) {
            sheet.setColumnWidth(i, 3_000);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private void appendRow(
            Sheet sheet,
            int rowNum,
            String matchType,
            String opponent,
            PlayerStatsDto.PlayerScoreStatsDto dto) {
        Row row = sheet.createRow(rowNum);
        appendCell(row, 0, matchType);
        appendCell(row, 1, opponent);
        appendNumbericCell(row, 2, dto.getMatches());
        appendNumbericCell(row, 3, dto.getWins());
        appendNumbericCell(row, 4, dto.getLoses());
        appendNumbericCell(row, 5, dto.getOvertimes());
        appendNumbericCell(row, 6, dto.getWinRate());
        appendNumbericCell(row, 7, dto.getPointsScored());
        appendNumbericCell(row, 8, dto.getPointsMissed());
        appendNumbericCell(row, 9, dto.getPointsRate());
        appendNumbericCell(row, 10, dto.getAvgPointsScored());
        appendNumbericCell(row, 11, dto.getAvgPointsMissed());
        appendNumbericCell(row, 12, dto.getMedianPointsScored());
        appendNumbericCell(row, 13, dto.getMedianPointsMissed());
    }

    private void appendCell(Row row, int cellNum, String cellValue) {
        Cell cell = row.createCell(cellNum, CellType.STRING);
        cell.setCellValue(cellValue);
    }

    private void appendNumbericCell(Row row, int cellNum, double cellValue) {
        Cell cell = row.createCell(cellNum, CellType.NUMERIC);
        cell.setCellValue(cellValue);
    }

    private Row createHeaderRow(Sheet sheet) {
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0, CellType.STRING).setCellValue("Матч");
        headerRow.createCell(1, CellType.STRING).setCellValue("Оппонент");
        headerRow.createCell(2, CellType.NUMERIC).setCellValue("Матчей");
        headerRow.createCell(3, CellType.NUMERIC).setCellValue("Выиграно");
        headerRow.createCell(4, CellType.NUMERIC).setCellValue("Проиграно");
        headerRow.createCell(5, CellType.NUMERIC).setCellValue("Овертаймов");
        headerRow.createCell(6, CellType.NUMERIC).setCellValue("Процент побед");
        headerRow.createCell(7, CellType.NUMERIC).setCellValue("Забито");
        headerRow.createCell(8, CellType.NUMERIC).setCellValue("Пропущено");
        headerRow.createCell(9, CellType.NUMERIC).setCellValue("Поинт рейт");
        headerRow.createCell(10, CellType.NUMERIC).setCellValue("Забито средн");
        headerRow.createCell(11, CellType.NUMERIC).setCellValue("Пропущено средн");
        headerRow.createCell(12, CellType.NUMERIC).setCellValue("Забито медиана");
        headerRow.createCell(13, CellType.NUMERIC).setCellValue("Пропущено медиана");
        return headerRow;
    }

}
