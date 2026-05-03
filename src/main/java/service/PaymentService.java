package service;

import db.SubscriptionDAO;
import model.Subscription;

import java.time.LocalDateTime;

/**
 * Simulated payment processing.
 * No real payment gateway — validates card format locally and always succeeds.
 */
public class PaymentService {

    private static PaymentService instance;
    private final SubscriptionDAO subscriptionDAO;

    private PaymentService() {
        subscriptionDAO = SubscriptionDAO.getInstance();
    }

    public static PaymentService getInstance() {
        if (instance == null)
            instance = new PaymentService();
        return instance;
    }

    // ── Plan metadata ─────────────────────────────────────────────────────

    public static final String PLAN_MONTHLY = "MONTHLY";
    public static final String PLAN_LIFETIME = "LIFETIME";

    public static String getDisplayPrice(String plan) {
        return switch (plan) {
            case PLAN_MONTHLY -> "Rs.199 / month";
            case PLAN_LIFETIME -> "$Rs.1999 one-time";
            default -> "Free";
        };
    }

    // ── Validation ────────────────────────────────────────────────────────

    /** Lightweight card-number validation (16 digits). For simulation only. */
    public boolean isCardNumberValid(String rawNumber) {
        String digits = rawNumber.replaceAll("[\\s-]", "");
        return digits.matches("\\d{16}");
    }

    public boolean isExpiryValid(String expiry) {
        return expiry != null && expiry.matches("(0[1-9]|1[0-2])/\\d{2}");
    }

    public boolean isCvvValid(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }

    /**
     * Validates all card fields.
     * 
     * @return null on success, or an error message string on failure.
     */
    public String validate(String cardHolder, String cardNumber, String expiry, String cvv) {
        if (cardHolder == null || cardHolder.trim().length() < 2)
            return "Please enter the cardholder name.";
        if (!isCardNumberValid(cardNumber))
            return "Card number must be 16 digits.";
        if (!isExpiryValid(expiry))
            return "Expiry must be MM/YY format.";
        if (!isCvvValid(cvv))
            return "CVV must be 3 or 4 digits.";
        return null;
    }

    // ── Processing ────────────────────────────────────────────────────────

    /**
     * Simulates a successful payment and creates an ACTIVE subscription record.
     * 
     * @param username the user being upgraded
     * @param plan     PLAN_MONTHLY or PLAN_LIFETIME
     */
    public Subscription processPayment(String username, String plan) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = PLAN_LIFETIME.equals(plan) ? null : now.plusMonths(1);

        Subscription sub = new Subscription(username, plan, "ACTIVE", now, end);
        subscriptionDAO.save(sub);
        return sub;
    }

    /** Fetches the current subscription for a user (may be null). */
    public Subscription getSubscription(String username) {
        return subscriptionDAO.getLatest(username);
    }

    public boolean isPremium(String username) {
        return subscriptionDAO.isPremium(username);
    }
}