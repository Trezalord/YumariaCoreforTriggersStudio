package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (ProgressionFailureReason).

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
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
