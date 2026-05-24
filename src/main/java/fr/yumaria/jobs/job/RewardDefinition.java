package fr.yumaria.jobs.job;

import java.util.List;

public record RewardDefinition(String moneyExpression, List<String> commands) {
    public static RewardDefinition empty() {
        return new RewardDefinition("0", List.of());
    }
}
