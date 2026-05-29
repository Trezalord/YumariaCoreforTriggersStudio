package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (RewardResult).

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public final class RewardResult {
    private final boolean success;
    private final String type;
    private final String detail;
    private final double amount;

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    private RewardResult(boolean success, String type, String detail, double amount) {
        this.success = success;
        this.type = type == null ? "" : type;
        this.detail = detail == null ? "" : detail;
        this.amount = amount;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static RewardResult success(String type, String detail, double amount) {
        return new RewardResult(true, type, detail, amount);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static RewardResult skipped(String type, String detail) {
        return new RewardResult(false, type, detail, 0.0D);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public static RewardResult failed(String type, String detail) {
        return new RewardResult(false, type, detail, 0.0D);
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public boolean success() {
        return success;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String type() {
        return type;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public String detail() {
        return detail;
    }

    // Annotation YumariaJobs: Repere methode: logique locale de cette classe.
    public double amount() {
        return amount;
    }
}
