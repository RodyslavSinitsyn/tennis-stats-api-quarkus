package org.rsinitsyn.service;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;
import org.rsinitsyn.dto.request.BaseFilter;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.OpponentFilter;
import org.rsinitsyn.dto.response.MatchPredictionResponse;
import org.rsinitsyn.dto.response.PlayerHistoryResponse;
import org.rsinitsyn.dto.response.PlayerMatchesResponse;
import org.rsinitsyn.dto.response.PlayerProgressResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse.PlayerStatsDto;
import org.rsinitsyn.dto.response.RatingsResponse;
import org.rsinitsyn.dto.response.RecordsResponse;
import org.rsinitsyn.dto.response.RecordsResponse.RecordListDto.PlayerValueDto;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchResultRepo;
import org.rsinitsyn.utils.ConverterUtils;
import org.rsinitsyn.utils.StatsUtils;

import static org.rsinitsyn.domain.MatchType.LONG;
import static org.rsinitsyn.domain.MatchType.SHORT;
import static org.rsinitsyn.dto.response.MatchPredictionResponse.MatchPredictDto;
import static org.rsinitsyn.utils.ConverterUtils.getPlayerStatisticDto;

@ApplicationScoped
@Transactional
public class TennisService {

    MatchResultRepo matchResultRepo;
    CsvReportService csvReportService;
    ExcelReportService excelReportService;

    @Inject
    public TennisService(MatchResultRepo matchResultRepo,
                         CsvReportService csvReportService,
                         ExcelReportService excelReportService) {
        this.matchResultRepo = matchResultRepo;
        this.csvReportService = csvReportService;
        this.excelReportService = excelReportService;
    }

    public List<String> getAllMatchesRepresentations() {
        List<PlayerMatchesResponse.PlayerMatchDetailsDto> matches = matchResultRepo.findAllDistinct()
                .stream()
                .sorted(Comparator.comparing(mr -> mr.getMatch().date))
                .map(ConverterUtils::getMatchDetailsDto)
                .toList();
        return matches.stream().map(PlayerMatchesResponse.PlayerMatchDetailsDto::getRepresentation).collect(Collectors.toList());
    }

    @CacheInvalidateAll(cacheName = "ratings-cache")
    @CacheInvalidateAll(cacheName = "records-cache")
    public Match saveMatch(CreateMatchDto dto) {
        validateMatchDto(dto);
        Match match = new Match();
        match.type = dto.type();
        match.date = Instant.now();
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

    public void validateMatchDto(CreateMatchDto dto) {
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


    @CacheResult(cacheName = "players-cache")
    public List<Player> findAllPlayers() {
        Log.info("#findAllPlayers()");
        return Player.listAll();
    }

    @SneakyThrows
    @CacheInvalidateAll(cacheName = "players-cache")
    public Player savePlayer(CreatePlayerDto dto) {
        Player player = Player.ofDto(dto);
        player.persist();
        return player;
    }

    public PlayerStatsResponse getPlayerStats(String name, OpponentFilter filtersDto) {
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
                        getPlayerStatisticDto(filterByType(filtered, SHORT)),
                        getPlayerStatisticDto(filterByType(filtered, LONG))
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

    private List<Predicate<MatchResult>> getFilters(BaseFilter filters) {
        List<Predicate<MatchResult>> predicates = new ArrayList<>();
        if (StringUtils.isNotEmpty(filters.getOpponent())) {
            predicates.add(mr -> mr.getOpponent().name.equals(filters.getOpponent()));
        }
        if (StringUtils.isNotEmpty(filters.getTournament())) {
            predicates.add(mr -> Optional.ofNullable(mr.getMatch().tournament)
                    .map(tournament -> tournament.name.equals(filters.getTournament()))
                    .orElse(Boolean.FALSE));
        }
        if (CollectionUtils.isNotEmpty(filters.getStages())) {
            predicates.add(mr -> filters.getStages().contains(mr.getMatch().stage));
        }
        return predicates;
    }

    public PlayerMatchesResponse getPlayerMatches(String name, OpponentFilter filters, boolean sortField, boolean formatted) {
        Player player = Player.findByName(name);
        List<MatchResult> filtered = player.matches.stream()
                .filter(matchResult -> getFilters(filters).stream().allMatch(p -> p.test(matchResult)))
                .sorted(Comparator.comparing(mr -> mr.getMatch().date, Comparator.reverseOrder()))
                .toList();


        if (formatted) {
            return new PlayerMatchesResponse(
                    null, StatsUtils.linkedHashMapMatchType(
                    getPlayerMatchDetailsDtoList(filtered, sortField, SHORT).stream().map(PlayerMatchesResponse.PlayerMatchDetailsDto::getRepresentation).toList(),
                    getPlayerMatchDetailsDtoList(filtered, sortField, LONG).stream().map(PlayerMatchesResponse.PlayerMatchDetailsDto::getRepresentation).toList()
            ));
        } else {
            return new PlayerMatchesResponse(
                    StatsUtils.linkedHashMapMatchType(
                            getPlayerMatchDetailsDtoList(filtered, sortField, SHORT),
                            getPlayerMatchDetailsDtoList(filtered, sortField, LONG)
                    ), null);
        }
    }

    private List<PlayerMatchesResponse.PlayerMatchDetailsDto> getPlayerMatchDetailsDtoList(List<MatchResult> matches,
                                                                                           boolean growSort,
                                                                                           MatchType matchType) {
        return filterByType(matches, matchType)
                .stream()
                .map(ConverterUtils::getMatchDetailsDto)
                .sorted(growSort
                        ? Comparator.comparing(PlayerMatchesResponse.PlayerMatchDetailsDto::getScoreDifference, Comparator.reverseOrder())
                        : Comparator.comparingInt(value -> 1))
                .toList();
    }


    @CacheResult(cacheName = "records-cache")
    public RecordsResponse getRecords() {
        Log.info("#getRecords()");
        List<MatchResult> allMatches = matchResultRepo.listAll();
        return new RecordsResponse(
                StatsUtils.linkedHashMapMatchType(
                        getRecordListDto(allMatches),
                        getRecordListDto(filterByType(allMatches, SHORT)),
                        getRecordListDto(filterByType(allMatches, LONG))
                )
        );
    }

    private List<MatchResult> filterByType(Collection<MatchResult> matchResults, MatchType... types) {
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
                                Comparator.comparing(PlayerStatsDto::getMatches),
                                PlayerStatsDto::getMatches)
                )
                .wins(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getWins),
                                PlayerStatsDto::getWins)
                )
                .loses(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getLoses),
                                PlayerStatsDto::getLoses)
                )
                .winRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getWinRate),
                                PlayerStatsDto::getWinRate)
                )
                .winStreak(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getWinStreak),
                                PlayerStatsDto::getWinStreak)
                )
                .loseStreak(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getLoseStreak),
                                PlayerStatsDto::getLoseStreak)
                )
                .pointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getPointsScored),
                                PlayerStatsDto::getPointsScored)
                )
                .avgPointsScored(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getAvgPointsScored),
                                PlayerStatsDto::getAvgPointsScored)
                )
                .pointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getPointsMissed),
                                PlayerStatsDto::getPointsMissed)
                )
                .avgPointsMissed(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getAvgPointsMissed),
                                PlayerStatsDto::getAvgPointsMissed)
                )
                .pointsRate(
                        getRecordDto(playerToStats,
                                Comparator.comparing(PlayerStatsDto::getPointsRate),
                                PlayerStatsDto::getPointsRate)
                )
                .build();
    }

    private RecordsResponse.RecordListDto.RecordDto getRecordDto(
            Map<Player, PlayerStatsDto> playersStats,
            Comparator<? super PlayerStatsDto> sortComparator,
            Function<? super PlayerStatsDto, Object> valueExtractor) {
        List<PlayerValueDto> ratingsList =
                getRatingsList(playersStats, sortComparator, valueExtractor);
        return new RecordsResponse.RecordListDto.RecordDto(
                ratingsList.get(0),
                ratingsList.get(ratingsList.size() - 1)
        );
    }

    public ByteArrayInputStream getPlayerStatsExcel(String name, BaseFilter filters) {
        return excelReportService.generateStatsReport(
                getPlayerStats(name,
                        new OpponentFilter(filters.getTournament(), filters.getStages(), null)));
    }

    public ByteArrayInputStream getPlayerStatsCsv(String name, OpponentFilter filters) {
        return csvReportService.generateStatsReport(
                getPlayerStats(name, filters));
    }


    @CacheResult(cacheName = "ratings-cache")
    public RatingsResponse getRatings(BaseFilter filter) {
        Log.info("#getRatings()");
        List<MatchResult> allMatches = matchResultRepo.streamAll()
                .filter(mr -> getFilters(filter).stream().allMatch(p -> p.test(mr)))
                .toList();

        return new RatingsResponse(
                StatsUtils.linkedHashMapMatchType(
                        getRatingListDto(allMatches),
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
                .matches(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getMatches), PlayerStatsDto::getMatches))
                .winRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getWinRate), PlayerStatsDto::getWinRate))
                .winStreak(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getWinStreak), PlayerStatsDto::getWinStreak))
                .loseStreak(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getLoseStreak), PlayerStatsDto::getLoseStreak))
                .pointsRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getPointsRate), PlayerStatsDto::getPointsRate))
                .avgScored(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getAvgPointsScored), PlayerStatsDto::getAvgPointsScored))
                .avgMissed(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getAvgPointsMissed), PlayerStatsDto::getAvgPointsMissed))
                .build();
    }

    private List<PlayerValueDto> getRatingsList(Map<Player, PlayerStatsDto> playerAndStats,
                                                Comparator<? super PlayerStatsDto> sortComparator,
                                                Function<? super PlayerStatsDto, Object> valueExtractor) {
        return playerAndStats.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(sortComparator.reversed()))
                .map(e -> new PlayerValueDto(
                        e.getKey().name,
                        String.valueOf(valueExtractor.apply(e.getValue()))
                ))
                .toList();
    }

    public PlayerHistoryResponse getPlayerHistory(String playerName, BaseFilter filters, Integer chunkSize) {
        List<Predicate<MatchResult>> predicates = getFilters(filters);
        var allMatches = Player.findByName(playerName).matches.stream()
                .filter(matchResult -> predicates.stream().allMatch(p -> p.test(matchResult)))
                .toList();

        return new PlayerHistoryResponse(
                chunkSize,
                StatsUtils.linkedHashMapMatchType(
                        getHistoryDtoList(filterByType(allMatches, SHORT), chunkSize),
                        getHistoryDtoList(filterByType(allMatches, LONG), chunkSize))
        );
    }

    public ByteArrayInputStream getPlayerHistoryInExcel(String name, BaseFilter filters, Integer chunkSize) {
        List<Predicate<MatchResult>> predicates = getFilters(filters);
        var allMatches = Player.findByName(name).matches.stream()
                .filter(matchResult -> predicates.stream().allMatch(p -> p.test(matchResult)))
                .toList();

        return excelReportService.generateHistoryReport(
                getHistoryDtoList(filterByType(allMatches, SHORT), chunkSize),
                getHistoryDtoList(filterByType(allMatches, LONG), chunkSize));
    }

    private PlayerHistoryResponse.PlayerStatsHistoryListDto getHistoryDtoList(List<MatchResult> matches, int chunkSize) {
        List<MatchResult> sortedList = matches.stream().sorted(Comparator.comparing(mr -> mr.getMatch().date)).toList();
        return PlayerHistoryResponse.PlayerStatsHistoryListDto.builder()
                .matchesCount(matches.size())
                .winRate(getHistoryOfSpecificStatsFromMatches(sortedList, PlayerStatsDto::getWinRate, chunkSize))
                .pointsScored(sortedList.stream().mapToDouble(MatchResult::getScored).boxed().toList())
                .pointsMissed(sortedList.stream().mapToDouble(MatchResult::getMissed).boxed().toList())
                .avgPointsScored(getHistoryOfSpecificStatsFromMatches(sortedList, PlayerStatsDto::getAvgPointsScored, chunkSize))
                .avgPointsMissed(getHistoryOfSpecificStatsFromMatches(sortedList, PlayerStatsDto::getAvgPointsMissed, chunkSize))
                .pointsRate(getHistoryOfSpecificStatsFromMatches(sortedList, PlayerStatsDto::getPointsRate, chunkSize))
                .build();
    }

    private <T> List<T> getHistoryOfSpecificStatsFromMatches(List<MatchResult> matches,
                                                             Function<PlayerStatsDto, T> valueExtractor,
                                                             int chunkSize) {
        List<T> result = new ArrayList<>();
        int totalMatchesCount = matches.size();

        int chunkCounter = 1;
        int chunksSize = BigDecimal.valueOf(totalMatchesCount).divide(BigDecimal.valueOf(chunkSize), RoundingMode.UP).intValue();

        int currChunkSize = chunkSize;
        while (chunkCounter <= chunksSize) {
            List<MatchResult> matchesChunk =
                    matches.stream()
                            .limit(currChunkSize)
                            .toList();
            result.add(valueExtractor.apply(getPlayerStatisticDto(matchesChunk)));
            currChunkSize = Math.min(currChunkSize + chunkSize, totalMatchesCount);
            chunkCounter++;
        }
        return result;
    }

    public PlayerProgressResponse getPlayerProgressPerDay(String name, MatchType matchType) {
        var allMatches = Player.findByName(name).matches;
        var filtered = matchType == null
                ? allMatches
                : filterByType(allMatches, matchType);

        var groupedByDay = filtered.stream()
                .sorted(Comparator.comparing(mr -> mr.getMatch().date))
                .collect(Collectors.groupingBy(
                        mr -> LocalDate.ofInstant(mr.getMatch().date, ZoneId.systemDefault()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        if (groupedByDay.size() < 2) {
            throw new TennisApiException("Cannot get progress of player due to lack of matches");
        }

        List<PlayerProgressResponse.PlayerProgressIntervalDto> intervals = new ArrayList<>();
        Iterator<Map.Entry<LocalDate, List<MatchResult>>> iterator = groupedByDay.entrySet().iterator();

        Map.Entry<LocalDate, List<MatchResult>> beforeEntry;
        Map.Entry<LocalDate, List<MatchResult>> afterEntry = null;

        while (iterator.hasNext()) {
            if (afterEntry != null) {
                beforeEntry = afterEntry;
                afterEntry = iterator.next();
            } else {
                beforeEntry = iterator.next();
            }
            if (afterEntry == null) {
                afterEntry = iterator.next();
            }
            intervals.add(getProgressIntervalDto(beforeEntry, afterEntry));
        }

        return new PlayerProgressResponse(
                matchType,
                intervals.size(),
                intervals);
    }

    private PlayerProgressResponse.PlayerProgressIntervalDto getProgressIntervalDto(Map.Entry<LocalDate, List<MatchResult>> beforeEntry,
                                                                                    Map.Entry<LocalDate, List<MatchResult>> afterEntry) {
        return new PlayerProgressResponse.PlayerProgressIntervalDto(
                beforeEntry.getKey(),
                afterEntry.getKey(),
                getProgressListDto(beforeEntry.getValue(), afterEntry.getValue()));
    }

    private PlayerProgressResponse.PlayerProgressDifferenceListDto getProgressListDto(List<MatchResult> beforeMatches,
                                                                                      List<MatchResult> afterMatches) {
        PlayerStatsDto beforeStats = getPlayerStatisticDto(beforeMatches);
        PlayerStatsDto afterStats = getPlayerStatisticDto(afterMatches);

        return PlayerProgressResponse.PlayerProgressDifferenceListDto.builder()
                .winRate(getProgressDifferenceDto(beforeStats.getWinRate(), afterStats.getWinRate()))
                .avgPointsScored(getProgressDifferenceDto(beforeStats.getAvgPointsScored(), afterStats.getAvgPointsScored()))
                .avgPointsMissed(getProgressDifferenceDto(beforeStats.getAvgPointsMissed(), afterStats.getAvgPointsMissed()))
                .pointsRate(getProgressDifferenceDto(beforeStats.getPointsRate(), afterStats.getPointsRate()))
                .build();
    }

    private PlayerProgressResponse.PlayerProgressDifferenceDto getProgressDifferenceDto(double valBefore, double valAfter) {
        double doubleDiff = BigDecimal.valueOf(valAfter - valBefore).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double percentDiff = StatsUtils.divide((int) (doubleDiff * 100), (int) valBefore);
        return new PlayerProgressResponse.PlayerProgressDifferenceDto(
                valBefore,
                valAfter,
                appendPlusSymbolIfNeeded(doubleDiff),
                appendPlusSymbolIfNeeded(percentDiff) + "%"
        );
    }

    private String appendPlusSymbolIfNeeded(double val) {
        return val > 0
                ? "+" + val
                : "" + val;
    }

    public MatchPredictionResponse predictMatchWinner(String playerName, String opponentName, MatchType matchType) {
        List<MatchPredictDto> predicts = getMatchPredictDtoList(
                Player.findByName(playerName),
                Player.findByName(opponentName),
                matchType
        );

        return new MatchPredictionResponse(
                predicts.stream().mapToDouble(MatchPredictDto::getProbability).sum(),
                playerName,
                opponentName,
                predicts
        );
    }

    private List<MatchPredictDto> getMatchPredictDtoList(Player player,
                                                         Player opponent,
                                                         MatchType matchType) {
        double versusWeight = 2;
        double generalWeight = 0.5;

        Map<MatchPredictDto, List<Double>> allOutcomesAndPredictions = new LinkedHashMap<>();

        appendDifferentPredictsToResult(allOutcomesAndPredictions,
                filterByType(player.matches, matchType).stream()
                        .filter(mr -> mr.getOpponent().equals(opponent)).toList(),
                versusWeight,
                matchType,
                true);
        appendDifferentPredictsToResult(allOutcomesAndPredictions, filterByType(player.matches, matchType), generalWeight, matchType, true);
        appendDifferentPredictsToResult(allOutcomesAndPredictions, filterByType(opponent.matches, matchType), generalWeight, matchType, false);

        allOutcomesAndPredictions.forEach((matchPredictDto, doubles) -> {
            System.out.println(matchPredictDto.score() + " " + doubles);
        });

        return allOutcomesAndPredictions.entrySet()
                .stream()
                .map(e -> new MatchPredictDto(
                        e.getKey().getScored(),
                        e.getKey().getMissed(),
                        calculateAveragePredict(e.getValue())
                ))
                .sorted(MatchPredictDto::compareTo)
                .toList();
    }

    private double calculateAveragePredict(List<Double> values) {
        return BigDecimal.valueOf(values.stream()
                        .mapToDouble(value -> value)
                        .average()
                        .orElseThrow())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void appendDifferentPredictsToResult(Map<MatchPredictDto, List<Double>> data,
                                                 List<MatchResult> matchesToAnalyze,
                                                 double weight,
                                                 MatchType matchType,
                                                 boolean predictSelf) {
        int maxPoints = matchType.getPoints();
        // Win
        for (int i = 0; i < matchType.getPoints(); i++) {
            var key = new MatchPredictDto(maxPoints, i);
            double res = getMatchOutcomeRatio(matchesToAnalyze,
                    predictSelf ? maxPoints : i,
                    predictSelf ? i : maxPoints,
                    matchType
            );
            data.putIfAbsent(key, new ArrayList<>());
            data.get(key).add(res * weight);
        }
        // Lose
        for (int i = 0; i < matchType.getPoints(); i++) {
            var key = new MatchPredictDto(i, maxPoints);
            double res = getMatchOutcomeRatio(matchesToAnalyze,
                    predictSelf ? i : maxPoints,
                    predictSelf ? maxPoints : i,
                    matchType
            );
            data.putIfAbsent(key, new ArrayList<>());
            data.get(key).add(res * weight);
        }
    }

    private double getMatchOutcomeRatio(List<MatchResult> matches, int scored, int missed, MatchType type) {
        var matchOutcome = new MatchPredictDto(scored, missed);
        var outcomeCounts = matches.stream()
                .map(mr -> new MatchPredictDto(mr.getScored(), mr.getMissed()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        final int totalOutcomesCount = type.getPoints() * 2 + (outcomeCounts.values().stream().mapToInt(Math::toIntExact).sum());

        var outcomeHappenedTimes = outcomeCounts.entrySet().stream()
                .filter(e -> e.getKey().equals(matchOutcome))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(0L);

        PlayerStatsDto stats = getPlayerStatisticDto(matches);

        boolean outcomeWin = scored > missed;
        double winDiff = StatsUtils.divide((int) stats.getWinRate(), 100);

        double wrWeight = outcomeWin
                ? 1 + winDiff
                : 1 - winDiff;

        if (winDiff == 0 && outcomeWin) {
            wrWeight = 0;
        } else if (winDiff == 0) {
            wrWeight = 2.0;
        }

        return BigDecimal.valueOf(Math.toIntExact(outcomeHappenedTimes) + 1)
                .divide(BigDecimal.valueOf(totalOutcomesCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
//                .multiply(BigDecimal.valueOf(wrWeight))
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
