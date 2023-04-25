package org.rsinitsyn.service;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
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
import org.rsinitsyn.dto.response.PlayerHistoryResponse;
import org.rsinitsyn.dto.response.PlayerMatchesResponse;
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


    public PlayerMatchesResponse getPlayerMatches(String name, PlayerStatsFilters filters, boolean growSort, boolean formatted) {
        Player player = Player.findByName(name);
        List<MatchResult> filtered = player.matches.stream()
                .filter(matchResult -> getFilters(filters).stream().allMatch(p -> p.test(matchResult)))
                .sorted(Comparator.comparing(mr -> mr.getMatch().date, Comparator.reverseOrder()))
                .toList();

        List<PlayerMatchesResponse.PlayerMatchDetailsDto> playerMatchDetailsDtos = filtered.stream()
                .map(ConverterUtils::getMatchDetailsDto)
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


    @CacheResult(cacheName = "records-cache")
    public RecordsResponse getRecords() {
        Log.info("#getRecords()");
        List<MatchResult> allMatches = matchResultRepo.listAll();
        return new RecordsResponse(
                StatsUtils.linkedHashMapMatchType(
                        getRecordListDto(filterByType(allMatches, SHORT, LONG)),
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

    public ByteArrayInputStream getPlayerStatsExcel(String name, BaseStatsFilter filters) {
        return excelReportService.generateStatsReport(
                getPlayerStats(name,
                        new PlayerStatsFilters(filters.getTournament(), filters.getStage(), null)));
    }

    public ByteArrayInputStream getPlayerStatsCsv(String name, PlayerStatsFilters filters) {
        return csvReportService.generateStatsReport(
                getPlayerStats(name, filters));
    }


    @CacheResult(cacheName = "ratings-cache")
    public RatingsResponse getRatings() {
        Log.info("#getRatings()");
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
                .matches(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getMatches), PlayerStatsDto::getMatches))
                .winRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getWinRate), PlayerStatsDto::getWinRate))
                .winStreak(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getWinStreak), PlayerStatsDto::getWinStreak))
                .loseStreak(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getLoseStreak), PlayerStatsDto::getLoseStreak))
                .pointsRate(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getPointsRate), PlayerStatsDto::getPointsRate))
                .avgScored(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getAvgPointsScored), PlayerStatsDto::getAvgPointsScored))
                .avgMissed(getRatingsList(playerToStats, Comparator.comparing(PlayerStatsDto::getAvgPointsMissed), PlayerStatsDto::getAvgPointsMissed))
                .build();
    }

    private List<PlayerValueDto> getRatingsList(Map<Player, PlayerStatsDto> map,
                                                Comparator<? super PlayerStatsDto> sortComparator,
                                                Function<? super PlayerStatsDto, Object> valueExtractor) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(sortComparator.reversed()))
                .map(e -> new PlayerValueDto(
                        e.getKey().name,
                        String.valueOf(valueExtractor.apply(e.getValue()))
                ))
                .toList();
    }

    public PlayerHistoryResponse getPlayerHistory(String playerName) {
        Set<MatchResult> allMatches = Player.findByName(playerName).matches;

        return new PlayerHistoryResponse(
                getHistoryDtoList(new ArrayList<>(allMatches)),
                getHistoryDtoList(filterByType(allMatches, SHORT)),
                getHistoryDtoList(filterByType(allMatches, LONG))
        );
    }

    private PlayerHistoryResponse.PlayerStatsHistoryListDto getHistoryDtoList(List<MatchResult> matches) {
        return PlayerHistoryResponse.PlayerStatsHistoryListDto.builder()
                .winRate(getHistoryOfSpecificStatsFromMatches(matches, PlayerStatsDto::getWinRate))
                .avgPointsScored(getHistoryOfSpecificStatsFromMatches(matches, PlayerStatsDto::getAvgPointsScored))
                .avgPointsMissed(getHistoryOfSpecificStatsFromMatches(matches, PlayerStatsDto::getAvgPointsMissed))
                .pointsRate(getHistoryOfSpecificStatsFromMatches(matches, PlayerStatsDto::getPointsRate))
                .build();
    }

    private <T> List<T> getHistoryOfSpecificStatsFromMatches(List<MatchResult> matches,
                                                             Function<PlayerStatsDto, T> valueExtractor) {
        List<MatchResult> sortedList = matches.stream().sorted(Comparator.comparing(mr -> mr.getMatch().date)).toList();
        List<T> result = new ArrayList<>();
        int totalMatchesCount = matches.size();
        int limiter = 1;
        while (limiter < totalMatchesCount) {
            List<MatchResult> matchesChunk =
                    sortedList.stream()
                            .limit(limiter)
                            .toList();
            result.add(valueExtractor.apply(getPlayerStatisticDto(matchesChunk)));
            limiter++;
        }
        return result;
    }
}
