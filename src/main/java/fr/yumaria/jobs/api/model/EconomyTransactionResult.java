package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (EconomyTransactionResult).

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class EconomyTransactionResult {
    private final boolean success;
    private final YumariaActionFailureReason failureReason;
    private final double amount;
    private final String message;

    // Annotation YumariaJobs: Gere la partie argent en passant par la couche economie centrale.
    private EconomyTransactionResult(boolean success, YumariaActionFailureReason failureReason, double amount, String message) {
        this.success = success;
        this.failureReason = failureReason == null ? YumariaActionFailureReason.NONE : failureReason;
        this.amount = Math.max(0.0D, amount);
        this.message = message == null ? "" : message;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static EconomyTransactionResult success(double amount) {
        return new EconomyTransactionResult(true, YumariaActionFailureReason.NONE, amount, "");
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static EconomyTransactionResult failure(YumariaActionFailureReason reason, double amount, String message) {
        return new EconomyTransactionResult(false, reason, amount, message);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean success() {
        return success;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public YumariaActionFailureReason failureReason() {
        return failureReason;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double amount() {
        return amount;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String message() {
        return message;
    }
}
