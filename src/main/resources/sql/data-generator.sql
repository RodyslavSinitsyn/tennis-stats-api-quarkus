create or replace function generatematches(playerName text, opponentName text, matchType text, count int) returns void
    language plpgsql
as
$$
DECLARE
    randomScored int    := 0;
    missed       int    := 0;
    matchId      bigint := 0;
    playerId     bigint;
    opponentId   bigint;
    points       int    := 11;
BEGIN

    SELECT id FROM player WHERE name = playerName INTO playerId;
    SELECT id FROM player WHERE name = opponentName INTO opponentId;

    IF matchType = 'LONG' THEN
        points := 21;
    end if;

    FOR i IN 1..count
        LOOP
            INSERT INTO match(id, type, tournament_id) VALUES ((SELECT nextval('hibernate_sequence')), matchType, null);

            SELECT floor(random() * (2 - 0 + 1) + 0) INTO randomScored;

            IF randomScored = 2 THEN
                SELECT points INTO randomScored;
                SELECT floor(random() * (points) + 0) INTO missed;
            ELSE
                SELECT points INTO missed;
                SELECT floor(random() * (points) + 0) INTO randomScored;
            END IF;

            SELECT MAX(id) FROM match INTO matchId;

            INSERT INTO match_player(matchid, playerid, missed, scored, extraround, winner)
            VALUES (matchId, playerid, missed, randomScored, false, randomScored > missed);
            INSERT INTO match_player(matchid, playerid, missed, scored, extraround, winner)
            VALUES (matchId, opponentId, randomScored, missed, false, missed > randomScored);
        END LOOP;
END ;
$$;