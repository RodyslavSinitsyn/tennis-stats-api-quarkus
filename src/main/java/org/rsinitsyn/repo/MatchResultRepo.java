package org.rsinitsyn.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import javax.enterprise.context.ApplicationScoped;
import org.rsinitsyn.domain.MatchResult;
import org.rsinitsyn.domain.MatchResultId;

@ApplicationScoped
public class MatchResultRepo implements PanacheRepositoryBase<MatchResult, MatchResultId> {
}
