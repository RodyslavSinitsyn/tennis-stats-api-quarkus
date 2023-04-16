package org.rsinitsyn.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchType;

public class StatsUtils {

    public static double divide(int val, int divideOn) {
        return BigDecimal.valueOf(val)
                .divide(BigDecimal.valueOf(
                        NumberUtils.max(divideOn, 1)), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static int median(double[] values) {
        return (int) new DescriptiveStatistics(values).getPercentile(50);
    }

    public static int longestStreak(List<MatchResult> matches,
                                    Predicate<MatchResult> predicate) {
        AtomicInteger currStreak = new AtomicInteger(0);
        AtomicInteger maxStreak = new AtomicInteger(-1);
        matches.stream()
                .sorted(Comparator.comparing(mr -> mr.getMatch().date))
                .forEach(mr -> {
                    if (predicate.test(mr)) {
                        currStreak.incrementAndGet();
                    } else {
                        currStreak.set(0);
                    }
                    if (currStreak.get() > maxStreak.get()) {
                        maxStreak.set(currStreak.get());
                    }
                });
        return maxStreak.get();
    }

    public static <T> Map<String, T> linkedHashMapMatchType(T allVal, T shortVal, T longVal) {
        Map<String, T> map = new LinkedHashMap<>();
        map.put("ALL", allVal);
        map.put(MatchType.SHORT.name(), shortVal);
        map.put(MatchType.LONG.name(), longVal);
        return map;
    }

    public static <T> Map<String, T> linkedHashMapMatchType(T shortVal, T longVal) {
        Map<String, T> map = new LinkedHashMap<>();
        map.put(MatchType.SHORT.name(), shortVal);
        map.put(MatchType.LONG.name(), longVal);
        return map;
    }
}
