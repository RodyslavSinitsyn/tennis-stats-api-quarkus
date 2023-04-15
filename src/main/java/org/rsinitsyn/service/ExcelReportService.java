package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rsinitsyn.dto.response.PlayerStatsResponse;
import org.rsinitsyn.exception.TennisApiException;

@ApplicationScoped
public class ExcelReportService implements ReportService {
    @SneakyThrows
    @Override
    public ByteArrayInputStream generateStatsReport(PlayerStatsResponse playerStats) {
        var workbook = new XSSFWorkbook();
        var allStatsSheet = createSheet(workbook, "Все матчи");
        createHeaderRow(allStatsSheet);
        appendRow(allStatsSheet, 1, "ALL", "ALL", playerStats.getOverallStats());

        var typeSheet = createSheet(workbook, "Шорт Лонг");
        createHeaderRow(typeSheet);
        int rowCounter = 1;
        for (Map.Entry<String, PlayerStatsResponse.PlayerStatsDto> typeEntry : playerStats.getTypeStats().entrySet()) {
            appendRow(typeSheet, rowCounter++, typeEntry.getKey(), "ALL", typeEntry.getValue());
        }

        for (Map.Entry<String, Map<String, PlayerStatsResponse.PlayerStatsDto>> opponentEntry : playerStats.getVersusPlayersStats().entrySet()) {
            var perPlayerSheet = createSheet(workbook, "Против " + opponentEntry.getKey());
            createHeaderRow(perPlayerSheet);
            int subCounter = 1;
            for (Map.Entry<String, PlayerStatsResponse.PlayerStatsDto> subTypeEntry : opponentEntry.getValue().entrySet()) {
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
            PlayerStatsResponse.PlayerStatsDto dto) {
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
