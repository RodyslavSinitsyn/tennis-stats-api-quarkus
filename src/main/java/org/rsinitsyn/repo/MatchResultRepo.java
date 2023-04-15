package org.rsinitsyn.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchResultId;

@ApplicationScoped
public class MatchResultRepo implements PanacheRepositoryBase<MatchResult, MatchResultId> {

    public List<MatchResult> findAllDistinct() {
        return streamAll()
                .collect(Collectors.toMap(MatchResult::getMatch, Function.identity(), (mr1, mr2) -> mr1, LinkedHashMap::new))
                .values().stream().toList();
    }
}
