package org.rsinitsyn.repo;

import com.google.cloud.firestore.Firestore;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.rsinitsyn.domain.Player;
import org.rsinitsyn.firestore.AbstractFirestoreRepository;

@ApplicationScoped
public class PlayersCollection extends AbstractFirestoreRepository<Player> {
    @Inject
    public PlayersCollection(Firestore firestore) {
        this(firestore, "players");
    }

    protected PlayersCollection(Firestore firestore, String collection) {
        super(firestore, collection);
    }
}
