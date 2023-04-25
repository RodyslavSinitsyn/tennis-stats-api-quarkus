package org.rsinitsyn.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.dto.response.PlayerMatchesResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse;

import static org.rsinitsyn.utils.StatsUtils.divide;
import static org.rsinitsyn.utils.StatsUtils.longestStreak;

public class ConverterUtils {

    public static PlayerMatchesResponse.PlayerMatchDetailsDto getMatchDetailsDto(MatchResult mr) {
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

    public static PlayerStatsResponse.PlayerStatsDto getPlayerStatisticDto(List<MatchResult> matches) {
        int wins = (int) matches.stream().filter(MatchResult::isWinner).count();
        int scored = matches.stream().mapToInt(MatchResult::getScored).sum();
        int missed = matches.stream().mapToInt(MatchResult::getMissed).sum();

        return PlayerStatsResponse.PlayerStatsDto.builder()
                .matches(matches.size())
                .wins(wins)
                .loses(matches.size() - wins)
                .winRate(divide(wins * 100, matches.size()))
                .winStreak(longestStreak(matches, MatchResult::isWinner))
                .loseStreak(longestStreak(matches, mr -> !mr.isWinner()))
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

    private static int getMedianValue(List<MatchResult> matches,
                                      ToDoubleFunction<? super MatchResult> valueExtractor) {
        return StatsUtils.median(
                matches.stream()
                        .mapToDouble(valueExtractor)
                        .distinct()
                        .sorted()
                        .toArray());
    }
}
