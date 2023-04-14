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
            INSERT INTO match(id, type, tournamentid, stage, date)
            VALUES ((SELECT nextval('hibernate_sequence')), matchType, null, 'FRIENDLY', now());

            SELECT floor(random() * (2 - 0 + 1) + 0) INTO randomScored;

            IF randomScored = 2 THEN
                SELECT points INTO randomScored;
                SELECT floor(random() * (points) + 0) INTO missed;
            ELSE
                SELECT points INTO missed;
                SELECT floor(random() * (points) + 0) INTO randomScored;
            END IF;

            SELECT MAX(id) FROM match INTO matchId;

            INSERT INTO match_result(matchid, playerid, missed, scored, extraround, winner, opponentid)
            VALUES (matchId, playerId, missed, randomScored, false, randomScored > missed, opponentId);

            INSERT INTO match_result(matchid, playerid, missed, scored, extraround, winner, opponentid)
            VALUES (matchId, opponentId, randomScored, missed, false, missed > randomScored, playerId);
        END LOOP;
END ;
$$;