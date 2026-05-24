package fr.yumaria.jobs.api.model;

public final class EconomyTransactionResult {
    private final boolean success;
    private final YumariaActionFailureReason failureReason;
    private final double amount;
    private final String message;

    private EconomyTransactionResult(boolean success, YumariaActionFailureReason failureReason, double amount, String message) {
        this.success = success;
        this.failureReason = failureReason == null ? YumariaActionFailureReason.NONE : failureReason;
        this.amount = Math.max(0.0D, amount);
        this.message = message == null ? "" : message;
    }

    public static EconomyTransactionResult success(double amount) {
        return new EconomyTransactionResult(true, YumariaActionFailureReason.NONE, amount, "");
    }

    public static EconomyTransactionResult failure(YumariaActionFailureReason reason, double amount, String message) {
        return new EconomyTransactionResult(false, reason, amount, message);
    }

    public boolean success() {
        return success;
    }

    public YumariaActionFailureReason failureReason() {
        return failureReason;
    }

    public double amount() {
        return amount;
    }

    public String message() {
        return message;
    }
}
