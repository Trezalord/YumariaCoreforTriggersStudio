package fr.yumaria.jobs.api.model;

public final class RewardResult {
    private final boolean success;
    private final String type;
    private final String detail;
    private final double amount;

    private RewardResult(boolean success, String type, String detail, double amount) {
        this.success = success;
        this.type = type == null ? "" : type;
        this.detail = detail == null ? "" : detail;
        this.amount = amount;
    }

    public static RewardResult success(String type, String detail, double amount) {
        return new RewardResult(true, type, detail, amount);
    }

    public static RewardResult skipped(String type, String detail) {
        return new RewardResult(false, type, detail, 0.0D);
    }

    public static RewardResult failed(String type, String detail) {
        return new RewardResult(false, type, detail, 0.0D);
    }

    public boolean success() {
        return success;
    }

    public String type() {
        return type;
    }

    public String detail() {
        return detail;
    }

    public double amount() {
        return amount;
    }
}
