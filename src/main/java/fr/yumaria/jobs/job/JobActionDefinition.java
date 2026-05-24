package fr.yumaria.jobs.job;

public record JobActionDefinition(boolean enabled, double progress, long cooldownMs) {
}
