package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
import org.apache.commons.lang3.StringUtils;
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
import org.rsinitsyn.dto.response.PlayerMatchesResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse;
import org.rsinitsyn.dto.response.RatingsResponse;
import org.rsinitsyn.dto.response.RecordsResponse;
import org.rsinitsyn.dto.response.TournamentHistoryResponse;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchResultRepo;
import org.rsinitsyn.utils.StatsUtils;

import static org.rsinitsyn.domain.MatchType.LONG;
import static org.rsinitsyn.domain.MatchType.SHORT;
import static org.rsinitsyn.utils.StatsUtils.divide;

@ApplicationScoped
@Transactional
public class TennisService {

    MatchResultRepo matchResultRepo;
    CsvReportService csvReportService;
    ExcelReportService excelReportService;

    @Inject
    public TennisService(MatchResultRepo matchResultRepo, CsvReportService csvReportService, ExcelReportService excelReportService) {
        this.matchResultRepo = matchResultRepo;
        this.csvReportService = csvReportService;
        this.excelReportService = excelReportService;
    }

    public List<String> getAllMatchesRepresentations() {
        List<PlayerMatchesResponse.PlayerMatchDetailsDto> matches = matchResultRepo.findAllDistinct()
                .stream()
                .sorted(Comparator.comparing(mr -> mr.getMatch().date))
                .map(this::getMatchDetails)
                .toList();
        return matches.stream().map(PlayerMatchesResponse.PlayerMatchDetailsDto::getRepresentation).collect(Collectors.toList());
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

    public PlayerStatsResponse getPlayerStats(String name, PlayerStatsFilters filtersDto) {
        var player = Player.findByName(name);
        List<MatchResult> filtered = player.matches
                .stream()
                .filter(matchResult -> getFilters(filtersDto).stream().allMatch(p -> p.test(matchResult)))
                .toList();
        return new PlayerStatsResponse(
                name,
                filtersDto,
                getPlayerStatisticDto(filtered),
                StatsUtils.linkedHashMapMatchType(
                        getPlayerStatisticDto(filtered.stream().filter(mr -> mr.getMatch().type.equals(SHORT)).toList()),
                        getPlayerStatisticDto(filtered.stream().filter(mr -> mr.getMatch().type.equals(LONG)).toList())
                ),
                filtered.stream().map(MatchResult::getOpponent)
                        .collect(Collectors.toSet())
                        .stream().collect(Collectors.toMap(
                                opponent -> opponent.name,
                                opponent -> StatsUtils.linkedHashMapMatchType(
                                        getPlayerStatisticDto(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name)).toList()),
                                        getPlayerStatisticDto(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name) && mr.getMatch().type.equals(SHORT)).toList()),
                                        getPlayerStatisticDto(filtered.stream().filter(mr -> mr.getOpponent().name.equals(opponent.name) && mr.getMatch().type.equals(LONG)).toList()))))
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

    private PlayerStatsResponse.PlayerStatsDto getPlayerStatisticDto(List<MatchResult> matches) {
        int wins = (int) matches.stream().filter(MatchResult::isWinner).count();
        int scored = matches.stream().mapToInt(MatchResult::getScored).sum();
        int missed = matches.stream().mapToInt(MatchResult::getMissed).sum();

        return PlayerStatsResponse.PlayerStatsDto.builder()
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
        return StatsUtils.median(
                matches.stream()
                        .mapToDouble(valueExtractor)
                        .sorted()
                        .toArray());
    }


    public PlayerMatchesResponse getPlayerMatches(String name, PlayerStatsFilters filters, boolean growSort, boolean formatted) {
        Player player = Player.findByName(name);
        List<MatchResult> filtered = player.matches.stream()
                .filter(matchResult -> getFilters(filters).stream().allMatch(p -> p.test(matchResult)))
                .sorted(Comparator.comparing(mr -> mr.getMatch().date, Comparator.reverseOrder()))
                .toList();

        List<PlayerMatchesResponse.PlayerMatchDetailsDto> playerMatchDetailsDtos = filtered.stream()
                .map(this::getMatchDetails)
                .sorted(Comparator.comparing(PlayerMatchesResponse.PlayerMatchDetailsDto::getScoreDifference,
                        growSort ? Comparator.naturalOrder() : Comparator.reverseOrder()))
                .toList();

        if (formatted) {
            return new PlayerMatchesResponse(
                    null,
                    playerMatchDetailsDtos.stream().map(PlayerMatchesResponse.PlayerMatchDetailsDto::getRepresentation).toList());
        } else {
            return new PlayerMatchesResponse(
                    playerMatchDetailsDtos,
                    null);
        }
    }

    private PlayerMatchesResponse.PlayerMatchDetailsDto getMatchDetails(MatchResult mr) {
        return PlayerMatchesResponse.PlayerMatchDetailsDto.builder()
                .matchType(mr.getMatch().type)
                .name(mr.getPlayer().name)
                .score(mr.getScored())
                .opponentName(mr.getOpponent().name)
                .opponentScore(mr.getMissed())
                .stage(mr.getMatch().stage.getDetails())
                .tournamentName(Optional.ofNullable(mr.getMatch().tournament).map(t -> t.fullName).orElse(""))
                .build();
    }

    public RecordsResponse getRecords() {
        List<MatchResult> allMatches = matchResultRepo.listAll();
        return new RecordsResponse(
                StatsUtils.linkedHashMapMatchType(
                        getRecordListDto(filterByType(allMatches, SHORT, LONG)),
                        getRecordListDto(filterByType(allMatches, SHORT)),
                        getRecordListDto(filterByType(allMatches, LONG))
                )
        );
    }

    private List<MatchResult> filterByType(List<MatchResult> matchResults, MatchType... types) {
        return matchResults.stream()
                .filter(mr -> Arrays.asList(types).contains(mr.getMatch().type))
                .toList();
    }

    private RecordsResponse.RecordListDto getRecordListDto(List<MatchResult> matchResults) {
        var playerToStats = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getPlayer))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getPlayerStatisticDto(e.getValue())));

        return RecordsResponse.RecordListDto.builder()
                .matches(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getMatches),
                                PlayerStatsResponse.PlayerStatsDto::getMatches)
                )
                .winRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getWinRate),
                                PlayerStatsResponse.PlayerStatsDto::getWinRate)
                )
                .wins(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getWins),
                                PlayerStatsResponse.PlayerStatsDto::getWins)
                )
                .loses(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getLoses),
                                PlayerStatsResponse.PlayerStatsDto::getLoses)
                )
                .pointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getPointsScored),
                                PlayerStatsResponse.PlayerStatsDto::getPointsScored)
                )
                .avgPointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getAvgPointsScored),
                                PlayerStatsResponse.PlayerStatsDto::getAvgPointsScored)
                )
                .pointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getPointsMissed),
                                PlayerStatsResponse.PlayerStatsDto::getPointsMissed)
                )
                .avgPointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getAvgPointsMissed),
                                PlayerStatsResponse.PlayerStatsDto::getAvgPointsMissed)
                )
                .pointsRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getPointsRate),
                                PlayerStatsResponse.PlayerStatsDto::getPointsRate)
                )
                .build();
    }

    private RecordsResponse.RecordListDto.RecordDto getRecordDto(
            Map<Player, PlayerStatsResponse.PlayerStatsDto> playersStats,
            Comparator<? super PlayerStatsResponse.PlayerStatsDto> sortComparator,
            Function<? super PlayerStatsResponse.PlayerStatsDto, Object> valueExtractor) {

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

        return new RecordsResponse.RecordListDto.RecordDto(
                new RecordsResponse.RecordListDto.PlayerValueDto(best.entrySet().stream().findFirst().orElseThrow().getKey(),
                        best.entrySet().stream().findFirst().orElseThrow().getValue()),

                new RecordsResponse.RecordListDto.PlayerValueDto(worst.entrySet().stream().findFirst().orElseThrow().getKey(),
                        worst.entrySet().stream().findFirst().orElseThrow().getValue())
        );
    }

    public ByteArrayInputStream getPlayerStatsExcel(String name, BaseStatsFilter filters) {
        return excelReportService.generateStatsReport(
                getPlayerStats(name,
                        new PlayerStatsFilters(filters.getTournament(), filters.getStage(), null)));
    }

    public ByteArrayInputStream getPlayerStatsCsv(String name, PlayerStatsFilters filters) {
        return csvReportService.generateStatsReport(
                getPlayerStats(name, filters));
    }

    public TournamentHistoryResponse getTournamentHistory(Long id) {
        var tournament = (Tournament)
                Tournament.findByIdOptional(id).orElseThrow(() -> new TennisApiException("Not found tournament", 404));
        var allMatches = matchResultRepo.findAllDistinct().stream()
                .filter(mr -> mr.getMatch().tournament != null)
                .filter(mr -> mr.getMatch().tournament.id.equals(id))
                .sorted(Comparator.comparing(mr -> mr.getMatch().stage.ordinal()))
                .toList();

        LinkedHashMap<TournamentStage, List<String>> history = allMatches.stream()
                .collect(Collectors.groupingBy(
                        mr -> mr.getMatch().stage,
                        LinkedHashMap::new,
                        Collectors.mapping(mr -> getMatchDetails(mr).getRepresentation(), Collectors.toList())));

        return new TournamentHistoryResponse(
                tournament,
                null,
                history
        );
    }

    public RatingsResponse getRatings() {
        List<MatchResult> allMatches = matchResultRepo.listAll();

        return new RatingsResponse(
                StatsUtils.linkedHashMapMatchType(
                        getRatingListDto(filterByType(allMatches, SHORT, LONG)),
                        getRatingListDto(filterByType(allMatches, SHORT)),
                        getRatingListDto(filterByType(allMatches, LONG))
                )
        );
    }

    private RatingsResponse.RatingsListDto getRatingListDto(List<MatchResult> matchResults) {
        var playerToStats = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getPlayer))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getPlayerStatisticDto(e.getValue())));


        return RatingsResponse.RatingsListDto.builder()
                .matches(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getMatches), PlayerStatsResponse.PlayerStatsDto::getMatches))
                .winRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getWinRate), PlayerStatsResponse.PlayerStatsDto::getWinRate))
                .pointsRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getPointsRate), PlayerStatsResponse.PlayerStatsDto::getPointsRate))
                .avgScored(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getAvgPointsScored), PlayerStatsResponse.PlayerStatsDto::getAvgPointsScored))
                .avgMissed(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsResponse.PlayerStatsDto::getAvgPointsMissed), PlayerStatsResponse.PlayerStatsDto::getAvgPointsMissed))
                .build();
    }

    private List<RecordsResponse.RecordListDto.PlayerValueDto> getRatingsList(Map<Player, PlayerStatsResponse.PlayerStatsDto> map,
                                                                              Comparator<? super PlayerStatsResponse.PlayerStatsDto> sortComparator,
                                                                              Function<? super PlayerStatsResponse.PlayerStatsDto, Object> valueExtractor) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(sortComparator.reversed()))
                .map(e -> new RecordsResponse.RecordListDto.PlayerValueDto(
                        e.getKey().name,
                        String.valueOf(valueExtractor.apply(e.getValue()))
                ))
                .toList();
    }
}
