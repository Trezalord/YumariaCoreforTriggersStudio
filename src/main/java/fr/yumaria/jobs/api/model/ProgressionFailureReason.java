package fr.yumaria.jobs.api.model;

public enum ProgressionFailureReason {
    NONE,
    PLAYER_NOT_FOUND,
    JOB_NOT_FOUND,
    JOB_NOT_ACTIVE,
    INVALID_AMOUNT,
    XP_DISABLED,
    SOURCE_BLOCKED,
    ANTI_ABUSE_REJECTED,
    PROFILE_NOT_LOADED,
    EVENT_CANCELLED,
    INTERNAL_ERROR
}
