package org.rsinitsyn.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.dto.response.MatchPredictionResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse;

import static org.rsinitsyn.utils.ConverterUtils.getPlayerStatisticDto;

@ApplicationScoped
public class PredictService {

    private static final int SCALE_DIGIT = 4;

    public List<MatchPredictionResponse.MatchPredictDto> getMatchPredictDtoList(List<MatchResult> commonMatches,
                                                                                List<MatchResult> playerMatches,
                                                                                List<MatchResult> opponentMatches,
                                                                                MatchType matchType) {
        double versusWeight = 2;
        double generalWeight = 0.5;

        Map<MatchPredictionResponse.MatchPredictDto, List<Double>> allOutcomesAndPredictions = new LinkedHashMap<>();

        appendDifferentPredictsToResult(allOutcomesAndPredictions,
                commonMatches,
                versusWeight,
                matchType,
                true);
        appendDifferentPredictsToResult(allOutcomesAndPredictions, playerMatches, generalWeight, matchType, true);
        appendDifferentPredictsToResult(allOutcomesAndPredictions, opponentMatches, generalWeight, matchType, false);

        allOutcomesAndPredictions.forEach((matchPredictDto, doubles) -> {
            System.out.println(matchPredictDto.score() + " " + doubles);
        });

        return allOutcomesAndPredictions.entrySet()
                .stream()
                .map(e -> new MatchPredictionResponse.MatchPredictDto(
                        e.getKey().getScored(),
                        e.getKey().getMissed(),
                        calculateAveragePredict(e.getValue())
                ))
                .sorted(MatchPredictionResponse.MatchPredictDto::compareTo)
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

    private void appendDifferentPredictsToResult(Map<MatchPredictionResponse.MatchPredictDto, List<Double>> data,
                                                 List<MatchResult> matchesToAnalyze,
                                                 double weight,
                                                 MatchType matchType,
                                                 boolean predictSelf) {
        int maxPoints = matchType.getPoints();
        // Win
        for (int i = 0; i < matchType.getPoints(); i++) {
            var key = new MatchPredictionResponse.MatchPredictDto(maxPoints, i);
            BigDecimal res = getMatchOutcomeRatio(matchesToAnalyze,
                    predictSelf ? maxPoints : i,
                    predictSelf ? i : maxPoints,
                    matchType
            );
            data.putIfAbsent(key, new ArrayList<>());
            data.get(key).add(
                    res.multiply(BigDecimal.valueOf(weight)).setScale(SCALE_DIGIT, RoundingMode.HALF_UP)
                            .doubleValue());
        }
        // Lose
        for (int i = 0; i < matchType.getPoints(); i++) {
            var key = new MatchPredictionResponse.MatchPredictDto(i, maxPoints);
            BigDecimal res = getMatchOutcomeRatio(matchesToAnalyze,
                    predictSelf ? i : maxPoints,
                    predictSelf ? maxPoints : i,
                    matchType
            );
            data.putIfAbsent(key, new ArrayList<>());
            data.get(key).add(
                    res.multiply(BigDecimal.valueOf(weight)).setScale(SCALE_DIGIT, RoundingMode.HALF_UP)
                            .doubleValue());
        }
    }

    private BigDecimal getMatchOutcomeRatio(List<MatchResult> matches, int scored, int missed, MatchType type) {
        var matchOutcome = new MatchPredictionResponse.MatchPredictDto(scored, missed);
        var outcomeCounts = matches.stream()
                .map(mr -> new MatchPredictionResponse.MatchPredictDto(mr.getScored(), mr.getMissed()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        final int totalOutcomesCount = type.getPoints() * 2 + (outcomeCounts.values().stream().mapToInt(Math::toIntExact).sum());

        var outcomeHappenedTimes = outcomeCounts.entrySet().stream()
                .filter(e -> e.getKey().equals(matchOutcome))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(0L);

        PlayerStatsResponse.PlayerStatsDto stats = getPlayerStatisticDto(matches);
        boolean outcomeWin = scored > missed;
        BigDecimal wrWeight = getWeightOfWinrate(stats.getWinRate(), outcomeWin, type.getPoints());

        return BigDecimal.valueOf(Math.toIntExact(outcomeHappenedTimes) + 1)
                .divide(BigDecimal.valueOf(totalOutcomesCount), SCALE_DIGIT, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .add(wrWeight)
                .setScale(SCALE_DIGIT, RoundingMode.HALF_UP);
    }

    private BigDecimal getWeightOfWinrate(double winRate, boolean outcomeWin, int outcomesCount) {
        BigDecimal winDiff = BigDecimal.valueOf(winRate)
                .divide(BigDecimal.valueOf(100), SCALE_DIGIT, RoundingMode.HALF_UP);
        BigDecimal wrWeight = outcomeWin
                ? BigDecimal.ONE.add(winDiff)
                : BigDecimal.ONE.min(winDiff);
        if (winDiff.intValue() == 0 && outcomeWin) {
            wrWeight = BigDecimal.ZERO;
        } else if (winDiff.intValue() == 0) {
            wrWeight = BigDecimal.valueOf(2);
        }
        return wrWeight.divide(BigDecimal.valueOf(outcomesCount), SCALE_DIGIT, RoundingMode.HALF_UP);
    }
}
