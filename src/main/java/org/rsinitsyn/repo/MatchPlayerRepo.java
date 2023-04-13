package org.rsinitsyn.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import javax.enterprise.context.ApplicationScoped;
import org.rsinitsyn.domain.MatchPlayer;
import org.rsinitsyn.domain.MatchPlayerId;

@ApplicationScoped
public class MatchPlayerRepo implements PanacheRepositoryBase<MatchPlayer, MatchPlayerId> {
}
