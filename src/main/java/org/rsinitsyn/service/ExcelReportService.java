package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrossBetween;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rsinitsyn.domain.MatchType;
import org.rsinitsyn.dto.response.PlayerHistoryResponse.PlayerStatsHistoryListDto;
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

    @Override
    public ByteArrayInputStream generateHistoryReport(PlayerStatsHistoryListDto shortHistory,
                                                      PlayerStatsHistoryListDto longHistory) {
        try (var workbook = new XSSFWorkbook();
             var out = new ByteArrayOutputStream()) {
            appendAllChartSheets(workbook, shortHistory, MatchType.SHORT);
            appendAllChartSheets(workbook, longHistory, MatchType.LONG);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new TennisApiException(e.getMessage());
        }
    }

    private void appendAllChartSheets(XSSFWorkbook workbook, PlayerStatsHistoryListDto history, MatchType matchType) throws IOException {
        appendChartSheet(workbook, List.of(new ChartValuesDto(history.getWinRate(), "Win Rate")),
                matchType.name() + " Win Rate", "Games Timeline", "Win Rate", 10, 110);
        appendChartSheet(workbook, List.of(new ChartValuesDto(history.getPointsRate(), "Points Rate")),
                matchType.name() + " Points Rate", "Games Timeline", "Points Rate", 0.1,
                history.getPointsRate().stream().max(Comparator.comparingDouble(value -> value)).orElse(2.0) + 0.2);
        appendChartSheet(workbook,
                List.of(new ChartValuesDto(history.getPointsScored(), "Scored", false),
                        new ChartValuesDto(history.getPointsMissed(), "Missed", false)),
                matchType.name() + " Scored & missed points", "Games Timeline", "Scored & missed", 1, matchType.getPoints() + 1);
        appendChartSheet(workbook,
                List.of(new ChartValuesDto(history.getAvgPointsScored(), "Scored"),
                        new ChartValuesDto(history.getAvgPointsMissed(), "Missed")),
                matchType.name() + " Avg scored & missed points", "Games Timeline", "Avg scored & missed", 1, matchType.getPoints() + 1);
    }

    private void appendChartSheet(XSSFWorkbook workbook,
                                  List<ChartValuesDto> valuesList,
                                  String sheetName,
                                  String bottomAxisName,
                                  String leftAxisName,
                                  double majorUnit,
                                  double maximumValue) throws IOException {
        if (valuesList.isEmpty() || valuesList.get(0).values.isEmpty()) {
            throw new TennisApiException("Source list is empty");
        }
        int desiredValuesCount = valuesList.get(0).values.size();
        if (!valuesList.stream().allMatch(dto -> dto.values.size() == desiredValuesCount)) {
            throw new TennisApiException("The size of values list for chart is different, should be " + desiredValuesCount);
        }

        var sheet = createSheet(workbook, sheetName);
        var drawing = sheet.createDrawingPatriarch();
        var anchor = drawing.createAnchor(0, 0, 0, 0, 0,
                valuesList.size() + 2, 20, valuesList.size() + 32);

        var chart = drawing.createChart(anchor);
        chart.setTitleText(sheetName);
        chart.setTitleOverlay(false);

        var legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);

        var bottomDataRow = sheet.createRow(0);
        for (int i = 0; i < desiredValuesCount; i++) {
            appendNumericCell(bottomDataRow, i, i + 1);
        }

        // Axis config
        var bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(bottomAxisName);
        var timelineDataSource = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(0, 0, 0, desiredValuesCount));

        var leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(leftAxisName);
        leftAxis.setMinimum(0);
        leftAxis.setMajorUnit(majorUnit);
        leftAxis.setMaximum(maximumValue);
        leftAxis.setCrossBetween(AxisCrossBetween.BETWEEN);

        // Series
        var chartData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        AtomicInteger rowNum = new AtomicInteger(1);
        valuesList.forEach(dto -> {
            var valuesDataRow = sheet.createRow(rowNum.get());
            AtomicInteger cellNum = new AtomicInteger(0);
            dto.values.forEach(val -> {
                appendNumericCell(valuesDataRow, cellNum.getAndIncrement(), val);
            });

            var valuesDataSource = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(rowNum.get(), rowNum.get(), 0, cellNum.get()));

            var series = (XDDFLineChartData.Series) chartData.addSeries(timelineDataSource, valuesDataSource);
            series.setTitle(dto.axisName, null);
            series.setSmooth(dto.smooth);
            series.setMarkerSize((short) 2);
            series.setMarkerStyle(MarkerStyle.DOT);
            series.setShowLeaderLines(true);
            rowNum.incrementAndGet();
        });

        chart.plot(chartData);
        chart.getCTChart()
                .getPlotArea()
                .getLineChartList().forEach(
                        chartLine -> {
                            chartLine.getSerList().forEach(serList -> serList.getDLbls().addNewShowVal().setVal(true));
                        });
    }

    private XSSFSheet createSheet(Workbook workbook, String text) {
        XSSFSheet sheet = (XSSFSheet) workbook.createSheet(text);
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
        appendNumericCell(row, 2, dto.getMatches());
        appendNumericCell(row, 3, dto.getWins());
        appendNumericCell(row, 4, dto.getLoses());
        appendNumericCell(row, 5, dto.getOvertimes());
        appendNumericCell(row, 6, dto.getWinRate());
        appendNumericCell(row, 7, dto.getPointsScored());
        appendNumericCell(row, 8, dto.getPointsMissed());
        appendNumericCell(row, 9, dto.getPointsRate());
        appendNumericCell(row, 10, dto.getAvgPointsScored());
        appendNumericCell(row, 11, dto.getAvgPointsMissed());
        appendNumericCell(row, 12, dto.getMedianPointsScored());
        appendNumericCell(row, 13, dto.getMedianPointsMissed());
    }

    private void appendCell(Row row, int cellNum, String cellValue) {
        Cell cell = row.createCell(cellNum, CellType.STRING);
        cell.setCellValue(cellValue);
    }

    private void appendNumericCell(Row row, int cellNum, double cellValue) {
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

    @AllArgsConstructor
    private static class ChartValuesDto {
        private List<Double> values;
        private String axisName;
        private boolean smooth;

        public ChartValuesDto(List<Double> values, String axisName) {
            this(values, axisName, true);
        }
    }
}
