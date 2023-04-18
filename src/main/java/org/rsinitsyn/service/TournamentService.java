package org.rsinitsyn.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.rsinitsyn.domain.Tournament;
import org.rsinitsyn.domain.TournamentStage;
import org.rsinitsyn.dto.response.TournamentHistoryResponse;
import org.rsinitsyn.exception.TennisApiException;
import org.rsinitsyn.repo.MatchResultRepo;
import org.rsinitsyn.utils.ConverterUtils;

@ApplicationScoped
@Transactional
public class TournamentService {

    @Inject
    MatchResultRepo matchResultRepo;

    public TournamentHistoryResponse getTournamentHistory(Long id) {
        var tournament = (Tournament)
                Tournament.findByIdOptional(id).orElseThrow(() -> new TennisApiException("Not found tournament", 404));
        var allMatches = matchResultRepo.findAllDistinct().stream()
                .filter(mr -> mr.getMatch().tournament != null)
                .filter(mr -> mr.getMatch().tournament.id.equals(id))
                .sorted(Comparator.comparing(mr -> mr.getMatch().stage.ordinal()))
                .toList();

        LinkedHashMap<TournamentStage, List<String>> history = allMatches.stream()
                .collect(Collectors.groupingBy(
                        mr -> mr.getMatch().stage,
                        LinkedHashMap::new,
                        Collectors.mapping(mr -> ConverterUtils.getMatchDetailsDto(mr).getRepresentation(), Collectors.toList())));

        return new TournamentHistoryResponse(
                tournament,
                null,
                history
        );
    }

    public List<Tournament> findAll() {
        return Tournament.listAll();
    }

    public Tournament save(String name, String fullname, String description) {
        Tournament tournament = new Tournament();
        tournament.name = name;
        tournament.fullName = fullname;
        tournament.description = description;
        tournament.persist();
        return tournament;
    }
}
