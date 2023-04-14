package org.rsinitsyn.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.PlayerStatsFilters;
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
                .collect(Collectors.toMap(MatchResult::getMatch, Function.identity(), (matchResult, matchResult2) -> matchResult));


        List<PlayerMatchesDto.PlayerMatchDetailsDto> matches = groupedByMatch.values().stream()
                .map(mr -> PlayerMatchesDto.PlayerMatchDetailsDto.builder()
                        .matchType(mr.getMatch().type)
                        .name(mr.getPlayer().name)
                        .score(mr.getScored())
                        .opponentName(mr.getOpponent().name)
                        .opponentScore(mr.getMissed())
                        .build())
                .toList();


        return matches.stream().map(PlayerMatchesDto.PlayerMatchDetailsDto::getRepresentation).collect(Collectors.toList());
    }

    public Match saveMatch(CreateMatchDto dto) {
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
            predicates.add(matchResult -> matchResult.getOpponent().name.equals(filters.getOpponent()));
        }
        if (StringUtils.isNotEmpty(filters.getTournament())) {
            predicates.add(matchPlayer -> Optional.ofNullable(matchPlayer.getMatch().tournament)
                    .map(tournament -> tournament.name.equals(filters.getTournament()))
                    .orElse(Boolean.FALSE));
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
                .pointsMissed(missed)
                .avgPointsMissed(divide(missed, matches.size()))
                .pointsRate(divide(scored, missed))
                .extra((int) matches.stream().filter(MatchResult::isExtraRound).count())
                .build();
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
                .toList();

        List<PlayerMatchesDto.PlayerMatchDetailsDto> playerMatchDetailsDtos = filtered.stream()
                .map(mr -> PlayerMatchesDto.PlayerMatchDetailsDto.builder()
                        .matchType(mr.getMatch().type)
                        .name(mr.getPlayer().name)
                        .score(mr.getScored())
                        .opponentName(mr.getOpponent().name)
                        .opponentScore(mr.getMissed())
                        .build())
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
}
