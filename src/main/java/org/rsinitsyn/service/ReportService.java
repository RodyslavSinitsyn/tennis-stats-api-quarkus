package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import org.rsinitsyn.dto.response.PlayerHistoryResponse;
import org.rsinitsyn.dto.response.PlayerStatsResponse;

public interface ReportService {
    ByteArrayInputStream generateStatsReport(PlayerStatsResponse playerStats);

    ByteArrayInputStream generateHistoryReport(PlayerHistoryResponse.PlayerStatsHistoryListDto shortHistory,
                                               PlayerHistoryResponse.PlayerStatsHistoryListDto longHistory);
}
