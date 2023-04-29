package org.rsinitsyn.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;
import org.rsinitsyn.domain.TournamentType;
import org.rsinitsyn.dto.response.PlayerStatsResponse;
import org.rsinitsyn.dto.response.TournamentHistoryResponse;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchResultRepo;
import org.rsinitsyn.utils.ConverterUtils;

import static org.rsinitsyn.dto.response.TournamentHistoryResponse.PlayerLeagueResultDto;
import static org.rsinitsyn.utils.ConverterUtils.getPlayerStatisticDto;

@ApplicationScoped
@Transactional
public class TournamentService {

    @Inject
    MatchResultRepo matchResultRepo;

    public TournamentHistoryResponse getTournamentHistory(String name) {
        var tournament = (Tournament)
                Tournament.findByName(name).orElseThrow(() -> new TennisApiException("Not found tournament", 404));
        var allMatches = matchResultRepo.findAllDistinct().stream()
                .filter(mr -> mr.getMatch().tournament != null)
                .filter(mr -> mr.getMatch().tournament.name.equals(name))
                .sorted(Comparator.comparing(mr -> mr.getMatch().stage.ordinal()))
                .toList();

        LinkedHashMap<TournamentStage, List<String>> history = allMatches.stream()
                .collect(Collectors.groupingBy(
                        mr -> mr.getMatch().stage,
                        LinkedHashMap::new,
                        Collectors.mapping(mr -> ConverterUtils.getMatchDetailsDto(mr).getRepresentation(), Collectors.toList())));

        List<String> table = null;
        if (tournament.type.equals(TournamentType.LEAGUE)) {
            table = getLeagueTable(matchResultRepo.streamAll()
                    .filter(mr -> mr.getMatch().tournament != null)
                    .filter(mr -> mr.getMatch().tournament.name.equals(name))
                    .sorted(Comparator.comparing(mr -> mr.getMatch().stage.ordinal()))
                    .toList());
        }

        return new TournamentHistoryResponse(
                tournament,
                table,
                history
        );
    }

    private List<String> getLeagueTable(List<MatchResult> matchResults) {
        String rowFormat = "%7s | %6s | %6s | %5s | %3s | %3s";
        List<String> tableRow = new ArrayList<>();
        tableRow.add(String.format(rowFormat, "Name", "Scored", "Missed", "Rate", "W", "L"));
        tableRow.add(String.format(rowFormat, "-".repeat(7), "-".repeat(6), "-".repeat(6), "-".repeat(5), "-".repeat(3), "-".repeat(3)));

        Map<Player, List<MatchResult>> playerMatches = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getPlayer));
        var results = playerMatches.entrySet().stream().map(e -> {
                    PlayerStatsResponse.PlayerStatsDto stats = getPlayerStatisticDto(e.getValue());
                    return new PlayerLeagueResultDto(
                            e.getKey().name,
                            stats.getPointsScored(),
                            stats.getPointsMissed(),
                            stats.getPointsRate(),
                            stats.getWins(),
                            stats.getLoses());
                })
                .sorted(PlayerLeagueResultDto::compareTo)
                .toList();
        results.forEach(pr -> {
            tableRow.add(
                    String.format(rowFormat,
                            pr.getName(), pr.getScored(), pr.getMissed(), pr.getRate(), pr.getWins(), pr.getLoses()));
        });
        return tableRow;
    }

    public List<Tournament> findAll() {
        return Tournament.listAll();
    }

    public Tournament save(String name, String fullname, String description, TournamentType type) {
        Tournament tournament = new Tournament();
        tournament.name = name;
        tournament.fullName = fullname;
        tournament.description = description;
        tournament.type = type;
        tournament.persist();
        return tournament;
    }
}
