package org.rsinitsyn.service;

import io.quarkus.runtime.util.StringUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.rsinitsyn.domain.Match;
import org.rsinitsyn.domain.MatchPlayer;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.dto.request.CreateMatchDto;
import org.rsinitsyn.dto.request.CreatePlayerDto;
import org.rsinitsyn.dto.request.PlayerStatsFilters;
import org.rsinitsyn.dto.response.MatchRepresentationDto;
import org.rsinitsyn.dto.response.PlayerStatsDto;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchPlayerRepo;

@ApplicationScoped
@Transactional
public class TennisService {

    @Inject
    MatchPlayerRepo matchPlayerRepo;

    public List<MatchRepresentationDto> getAllMatchesRepresentations() {
        Map<Match, List<MatchPlayer>> groupedByMatch = matchPlayerRepo.streamAll()
                .collect(Collectors.groupingBy(MatchPlayer::getMatch));

        List<MatchRepresentationDto> response = new ArrayList<>();
        groupedByMatch.values().stream()
                .filter(matchPlayers -> matchPlayers.size() == 2)
                .forEach(matchPlayers -> {
                    String leftVal = extractPlayerScore(
                            matchPlayers.get(0).getPlayer().name,
                            matchPlayers.get(0).getScored(),
                            true
                    );
                    String rightVal = extractPlayerScore(
                            matchPlayers.get(1).getPlayer().name,
                            matchPlayers.get(1).getScored(),
                            false
                    );

                    response.add(new MatchRepresentationDto(
                            leftVal + " - " + rightVal,
                            ""));
                });

        return response;
    }

    private String extractPlayerScore(String playerName, int score, boolean left) {
        return left
                ? playerName + " " + score
                : score + " " + playerName;
    }

    public List<MatchPlayer> saveMatch(CreateMatchDto dto) {
        Match match = new Match();
        match.type = dto.type();
        if (!StringUtil.isNullOrEmpty(dto.event())) {
            Tournament.findByName(dto.event())
                    .ifPresent(tournament -> match.tournament = tournament);
        }
        match.persist();

        return List.of(
                saveMatchPlayer(match, dto.player(), dto.opponentPlayer()),
                saveMatchPlayer(match, dto.opponentPlayer(), dto.player())
        );
    }

    public MatchPlayer saveMatchPlayer(Match match,
                                       CreateMatchDto.PlayerResultDto player,
                                       CreateMatchDto.PlayerResultDto opponent) {
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setScored(player.score());
        matchPlayer.setMissed(opponent.score());
        matchPlayer.setExtraRound(Math.abs(player.score()) - opponent.score() == 1);
        matchPlayer.setWinner(player.score() > opponent.score());

        Player persistedPlayer = (Player) Player.find("name", player.name()).firstResultOptional()
                .orElseThrow(() -> new TennisApiException("Player 'name' not found:" + player.name()));
        matchPlayer.setPlayer(persistedPlayer);

        matchPlayerRepo.persist(matchPlayer);
        return matchPlayer;
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
        List<MatchPlayer> playerMatches = matchPlayerRepo.streamAll()
                .filter(matchPlayer -> matchPlayer.getPlayer().name.equals(name))
                .toList();
        return new PlayerStatsDto(
                filtersDto,
                getPlayerStatistic(playerMatches, Collections.emptyList()),
                Map.of(
                        MatchType.SHORT, getPlayerStatistic(playerMatches, getFilters(filtersDto, MatchType.SHORT)),
                        MatchType.LONG, getPlayerStatistic(playerMatches, getFilters(filtersDto, MatchType.LONG))
                ),
                null
        );
    }

    private List<Predicate<MatchPlayer>> getFilters(PlayerStatsFilters filters, MatchType defaultType) {
        List<Predicate<MatchPlayer>> predicates = new ArrayList<>();
        if (StringUtils.isNotEmpty(filters.getOpponent())) {
            predicates.add(matchPlayer -> matchPlayerRepo.streamAll()
                    .filter(mp -> mp.getMatch().id.equals(matchPlayer.getMatch().id))
                    .anyMatch(mp -> mp.getPlayer().name.equals(filters.getOpponent()))
            );
        }
        if (StringUtils.isNotEmpty(filters.getTournament())) {
            predicates.add(matchPlayer -> Optional.ofNullable(matchPlayer.getMatch().tournament)
                    .map(tournament -> tournament.name.equals(filters.getTournament()))
                    .orElse(Boolean.FALSE));
        }
        if (filters.getType() == null) {
            predicates.add(matchPlayer -> matchPlayer.getMatch().type.equals(defaultType));
        }
        return predicates;
    }

    public PlayerStatsDto.PlayerScoreStatsDto getPlayerStatistic(List<MatchPlayer> playerMatches,
                                                                 List<Predicate<MatchPlayer>> filters) {
        List<MatchPlayer> matches = playerMatches.stream()
                .filter(matchPlayer -> filters.stream().allMatch(filter -> filter.test(matchPlayer)))
                .toList();

        int wins = (int) matches.stream().filter(MatchPlayer::isWinner).count();
        int scored = matches.stream().mapToInt(MatchPlayer::getScored).sum();
        int missed = matches.stream().mapToInt(MatchPlayer::getMissed).sum();
        return PlayerStatsDto.PlayerScoreStatsDto.builder()
                .matches(matches.size())
                .wins(wins)
                .loses(matches.size() - wins)
                .winRate(divide(wins * 100, matches.size()))
                .scored(scored)
                .missed(missed)
                .scoreRate(divide(scored, missed))
                .build();
    }

    private double divide(int val, int divideOn) {
        return BigDecimal.valueOf(val)
                .divide(BigDecimal.valueOf(
                        NumberUtils.max(divideOn, 1)), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
