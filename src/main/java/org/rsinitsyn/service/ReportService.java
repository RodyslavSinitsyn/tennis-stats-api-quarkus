package org.rsinitsyn.service;

import java.io.ByteArrayInputStream;
import org.rsinitsyn.dto.response.PlayerStatsDto;

public interface ReportService {
    ByteArrayInputStream generateStatsReport(PlayerStatsDto playerStats);
}
