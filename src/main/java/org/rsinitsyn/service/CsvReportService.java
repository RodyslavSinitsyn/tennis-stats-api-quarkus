package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.rsinitsyn.dto.response.PlayerStatsResponse;
import org.rsinitsyn.exception.TennisApiException;

@ApplicationScoped
public class CsvReportService implements ReportService {
    @Override
    public ByteArrayInputStream generateStatsReport(PlayerStatsResponse playerStats) {

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
            for (Map.Entry<String, PlayerStatsResponse.PlayerStatsDto> entry : playerStats.getTypeStats().entrySet()) {
                csvPrinter.printRecord(statsToTokens(entry.getKey(), "ALL", entry.getValue()));
            }
            for (Map.Entry<String, Map<String, PlayerStatsResponse.PlayerStatsDto>> entry : playerStats.getVersusPlayersStats().entrySet()) {
                for (Map.Entry<String, PlayerStatsResponse.PlayerStatsDto> subEntry : entry.getValue().entrySet()) {
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
                                       PlayerStatsResponse.PlayerStatsDto dto) {
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
}
