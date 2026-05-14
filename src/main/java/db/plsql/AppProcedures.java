package db.plsql;

/*
 * ════════════════════════════════════════════════════════════════════════════
 *  PL/SQL EQUIVALENTS  (Oracle syntax — reference only)
 *
 *  ── PROCEDURE 1 ───────────────────────────────────────────────────────────
 *  CREATE OR REPLACE PROCEDURE sp_process_game_completion (
 *    p_username    IN  VARCHAR2,
 *    p_wpm         IN  NUMBER,
 *    p_accuracy    IN  NUMBER,
 *    p_mode        IN  VARCHAR2,
 *    p_difficulty  IN  VARCHAR2,
 *    p_duration    IN  INTEGER,
 *    p_errors      IN  INTEGER,
 *    p_words       IN  INTEGER,
 *    p_xp_gained   OUT INTEGER,
 *    p_coins_out   OUT INTEGER,
 *    p_leveled_up  OUT BOOLEAN
 *  ) AS
 *  BEGIN
 *    INSERT INTO game_results
 *      (username, wpm, accuracy, game_mode, difficulty,
 *       duration, error_count, words_typed, played_at)
 *    VALUES
 *      (p_username, p_wpm, p_accuracy, p_mode, p_difficulty,
 *       p_duration, p_errors, p_words, SYSDATE);
 *    -- AFTER INSERT trigger trg_after_game_insert fires here automatically
 *    -- Update aggregate stats in users table
 *    UPDATE users
 *       SET total_games   = total_games + 1,
 *           best_wpm      = GREATEST(best_wpm, p_wpm),
 *           average_wpm   = (average_wpm * (total_games) + p_wpm) / (total_games + 1),
 *           best_accuracy = GREATEST(best_accuracy, p_accuracy)
 *     WHERE username = p_username;
 *    -- Gamification (XP / coins / achievements)
 *    pkg_gamification.process(p_username, p_wpm, p_accuracy,
 *                             p_xp_gained, p_coins_out, p_leveled_up);
 *    COMMIT;
 *  EXCEPTION
 *    WHEN OTHERS THEN ROLLBACK; RAISE;
 *  END sp_process_game_completion;
 *  /
 *
 *  ── PROCEDURE 2 ───────────────────────────────────────────────────────────
 *  CREATE OR REPLACE PROCEDURE sp_reset_user_progress (
 *    p_username   IN VARCHAR2,
 *    p_reset_type IN VARCHAR2  -- 'STATS_ONLY' | 'ACHIEVEMENTS_ONLY' | 'FULL'
 *  ) AS
 *    v_affected INTEGER := 0;
 *  BEGIN
 *    IF p_reset_type IN ('STATS_ONLY', 'FULL') THEN
 *      DELETE FROM game_results WHERE username = p_username;
 *      v_affected := v_affected + SQL%ROWCOUNT;
 *      UPDATE users
 *         SET total_games = 0, best_wpm = 0,
 *             average_wpm = 0, best_accuracy = 0
 *       WHERE username = p_username;
 *      UPDATE user_stats
 *         SET xp = 0, level = 1, streak = 0, coins = 0
 *       WHERE username = p_username;
 *      v_affected := v_affected + 2;
 *    END IF;
 *    IF p_reset_type IN ('ACHIEVEMENTS_ONLY', 'FULL') THEN
 *      DELETE FROM achievements WHERE username = p_username;
 *      v_affected := v_affected + SQL%ROWCOUNT;
 *    END IF;
 *    INSERT INTO audit_log (event, username, detail, occurred_at)
 *    VALUES ('PROGRESS_RESET', p_username,
 *            'resetType=' || p_reset_type || ' affected=' || v_affected,
 *            SYSDATE);
 *    COMMIT;
 *  END sp_reset_user_progress;
 *  /
 *
 *  ── PROCEDURE 3 ───────────────────────────────────────────────────────────
 *  CREATE OR REPLACE PROCEDURE sp_grant_subscription (
 *    p_username  IN  VARCHAR2,
 *    p_plan      IN  VARCHAR2,   -- 'MONTHLY' | 'YEARLY'
 *    p_months    IN  INTEGER,
 *    p_status    OUT VARCHAR2    -- 'SUCCESS' | 'USER_NOT_FOUND' | 'INVALID_PLAN'
 *  ) AS
 *    v_count INTEGER;
 *  BEGIN
 *    SELECT COUNT(*) INTO v_count FROM users WHERE username = p_username;
 *    IF v_count = 0 THEN
 *      p_status := 'USER_NOT_FOUND'; RETURN;
 *    END IF;
 *    IF p_plan NOT IN ('MONTHLY', 'YEARLY') THEN
 *      p_status := 'INVALID_PLAN'; RETURN;
 *    END IF;
 *    -- Expire any currently active subscription
 *    UPDATE subscriptions SET status = 'EXPIRED'
 *     WHERE username = p_username AND status = 'ACTIVE';
 *    -- Insert new subscription record
 *    INSERT INTO subscriptions
 *      (username, plan, status, start_date, end_date, payment_method, payment_detail)
 *    VALUES
 *      (p_username, p_plan, 'ACTIVE', SYSDATE,
 *       ADD_MONTHS(SYSDATE, p_months), 'ADMIN_GRANT', 'Granted via sp_grant_subscription');
 *    -- Update the user's subscription_type field
 *    UPDATE users SET subscription_type = p_plan WHERE username = p_username;
 *    COMMIT;
 *    p_status := 'SUCCESS';
 *  EXCEPTION
 *    WHEN OTHERS THEN ROLLBACK; p_status := 'ERROR: ' || SQLERRM; RAISE;
 *  END sp_grant_subscription;
 *  /
 * ════════════════════════════════════════════════════════════════════════════
 */

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import db.MongoDBManager;
import db.SubscriptionDAO;
import db.UserStatsDAO;
import model.GameResult;
import model.Subscription;
import model.User;
import model.UserStats;
import service.GamificationService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Application-level stored procedures for the TypeMaster game.
 * <p>
 * Each static method encapsulates a multi-step, logically atomic operation —
 * the Java equivalent of a PL/SQL {@code PROCEDURE}.  The methods accept
 * IN-style parameters and return a result record that carries OUT-style data.
 * </p>
 */
public class AppProcedures {

    // ── OUT-parameter types ───────────────────────────────────────────────

    /**
     * OUT parameters for {@link #spProcessGameCompletion}.
     * Mirrors the p_xp_gained / p_coins_out / p_leveled_up OUT parameters.
     */
    public record CompletionResult(
            GameResult savedResult,
            GamificationService.GamificationResult gamification) {}

    /**
     * Scope selector for {@link #spResetUserProgress}.
     * Equivalent to the p_reset_type IN parameter in PL/SQL.
     */
    public enum ResetType { STATS_ONLY, ACHIEVEMENTS_ONLY, FULL }

    /**
     * Status code returned by {@link #spGrantSubscription} — equivalent to
     * the p_status OUT VARCHAR2 parameter.
     */
    public enum GrantStatus { SUCCESS, USER_NOT_FOUND, INVALID_PLAN }

    private AppProcedures() {}

    // ─────────────────────────────────────────────────────────────────────────
    // PROCEDURE 1 — sp_process_game_completion
    //
    //   Atomically: saves the game result (fires AFTER_GAME_INSERT trigger for audit),
    //   updates the aggregate user stats, runs the gamification pipeline,
    //   and returns the combined outcome as a CompletionResult record.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a completed game session, updates user aggregate stats, and runs
     * the full gamification pipeline (XP, coins, achievements, streak).
     *
     * @param username player's username (IN)
     * @param result   fully populated GameResult — ID is assigned inside (IN)
     * @return {@link CompletionResult} carrying the persisted result and the
     *         XP / achievement outcome (analogous to OUT parameters)
     */
    public static CompletionResult spProcessGameCompletion(String username, GameResult result) {
        // INSERT INTO game_results ... — trg_after_game_insert fires here (audit log)
        MongoDBManager.getInstance().saveResult(result);

        // UPDATE users SET best_wpm = GREATEST(...), average_wpm = ..., total_games += 1
        MongoDBManager.getInstance().updateUserStats(
                username, result.getWpm(), result.getAccuracy());

        // pkg_gamification.process(...) — XP, coins, level-up, achievements
        GamificationService.GamificationResult gamResult =
                GamificationService.getInstance().processGameResult(username, result);

        writeAudit("GAME_COMPLETION", username,
                String.format("xp=%d coins=%d levelUp=%b newLevel=%d",
                        gamResult.xpGained(), gamResult.coinsGained(),
                        gamResult.leveledUp(), gamResult.newLevel()));

        return new CompletionResult(result, gamResult);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCEDURE 2 — sp_reset_user_progress
    //
    //   Resets player progress scoped by ResetType:
    //     STATS_ONLY        → wipes game_results + resets user stats + clears XP/coins
    //     ACHIEVEMENTS_ONLY → removes all earned achievements
    //     FULL              → all of the above combined
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resets a user's progress to baseline according to the specified scope.
     *
     * @param username target user's username (IN)
     * @param type     reset scope — analogous to p_reset_type IN VARCHAR2 (IN)
     * @return total number of documents deleted / updated across collections
     *         (analogous to SQL%ROWCOUNT in PL/SQL)
     */
    public static int spResetUserProgress(String username, ResetType type) {
        int affected = 0;
        var db = MongoDBManager.getInstance().getDatabase();

        if (type == ResetType.STATS_ONLY || type == ResetType.FULL) {
            // DELETE FROM game_results WHERE username = p_username
            var del = db.getCollection("game_results")
                    .deleteMany(Filters.eq("username", username));
            affected += (int) del.getDeletedCount();

            // UPDATE users SET total_games=0, best_wpm=0, average_wpm=0, best_accuracy=0
            db.getCollection("users").updateOne(
                    Filters.eq("username", username),
                    Updates.combine(
                            Updates.set("totalGames",   0),
                            Updates.set("bestWpm",      0.0),
                            Updates.set("averageWpm",   0.0),
                            Updates.set("bestAccuracy", 0.0)));

            // UPDATE user_stats SET xp=0, level=1, streak=0, coins=0
            UserStatsDAO.getInstance().save(new UserStats(username));
            affected += 2;
        }

        if (type == ResetType.ACHIEVEMENTS_ONLY || type == ResetType.FULL) {
            // DELETE FROM achievements WHERE username = p_username
            var del = db.getCollection("achievements")
                    .deleteMany(Filters.eq("username", username));
            affected += (int) del.getDeletedCount();
        }

        // INSERT INTO audit_log ...  (COMMIT equivalent — changes already persisted)
        writeAudit("PROGRESS_RESET", username,
                "resetType=" + type.name() + " affected=" + affected);
        return affected;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCEDURE 3 — sp_grant_subscription
    //
    //   Grants a new subscription to an existing user:
    //     1. Validates user exists and plan is recognised.
    //     2. Expires any currently ACTIVE subscription.
    //     3. Inserts a new ACTIVE subscription record.
    //     4. Updates the user's subscriptionType field.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Grants a subscription plan to the named user.
     *
     * @param username target user (IN)
     * @param plan     "MONTHLY" or "YEARLY" (IN)
     * @param months   duration in months used to set the end-date (IN)
     * @return {@link GrantStatus} — analogous to the p_status OUT VARCHAR2 parameter
     */
    public static GrantStatus spGrantSubscription(String username, String plan, int months) {
        // IF v_count = 0 THEN p_status := 'USER_NOT_FOUND'; RETURN; END IF;
        User user = MongoDBManager.getInstance().getUserByUsername(username);
        if (user == null) return GrantStatus.USER_NOT_FOUND;

        // Validate plan — p_status := 'INVALID_PLAN'; RETURN;
        String planUpper = plan == null ? "" : plan.toUpperCase();
        if (!"MONTHLY".equals(planUpper) && !"YEARLY".equals(planUpper)) {
            return GrantStatus.INVALID_PLAN;
        }

        // UPDATE subscriptions SET status='EXPIRED' WHERE username=p_username AND status='ACTIVE'
        var existing = SubscriptionDAO.getInstance().getLatest(username);
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            SubscriptionDAO.getInstance().updateStatus(existing.getId(), "EXPIRED");
        }

        // INSERT INTO subscriptions (username, plan, status, start_date, end_date, ...)
        Subscription sub = new Subscription();
        sub.setUsername(username);
        sub.setPlan(planUpper);
        sub.setStatus("ACTIVE");
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(LocalDateTime.now().plusMonths(months));
        sub.setPaymentMethod("ADMIN_GRANT");
        sub.setPaymentDetail("Granted via sp_grant_subscription");
        SubscriptionDAO.getInstance().save(sub);

        // UPDATE users SET subscription_type = p_plan WHERE username = p_username
        MongoDBManager.getInstance().getDatabase()
                .getCollection("users")
                .updateOne(Filters.eq("username", username),
                        Updates.set("subscriptionType", planUpper));

        writeAudit("SUBSCRIPTION_GRANTED", username,
                String.format("plan=%s months=%d subId=%s", planUpper, months, sub.getId()));
        return GrantStatus.SUCCESS;
    }

    // ── Shared audit helper ───────────────────────────────────────────────

    static void writeAudit(String event, String username, String detail) {
        try {
            MongoDBManager.getInstance().getDatabase()
                    .getCollection("audit_log")
                    .insertOne(new org.bson.Document()
                            .append("event",      event)
                            .append("username",   username)
                            .append("detail",     detail)
                            .append("occurredAt", Date.from(
                                    LocalDateTime.now()
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant())));
        } catch (Exception ignored) {}
    }
}
