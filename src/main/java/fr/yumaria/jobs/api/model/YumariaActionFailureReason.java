package fr.yumaria.jobs.api.model;

// Repere fichier YumariaJobs: modele public immuable utilise par l API (YumariaActionFailureReason).

// Role YumariaJobs: Transporte des donnees API immuables entre plugins et services.
public enum YumariaActionFailureReason {
    NONE,
    PLAYER_NOT_FOUND,
    PROFILE_NOT_LOADED,
    ADDON_NOT_ALLOWED,
    ADDON_NOT_REGISTERED,
    PROFESSION_NOT_FOUND,
    PROFESSION_DISABLED,
    PROFESSION_NOT_ACTIVE,
    ACTION_TYPE_BLOCKED,
    INVALID_XP_AMOUNT,
    INVALID_MONEY_AMOUNT,
    ANTI_ABUSE_REJECTED,
    ECONOMY_DISABLED,
    EVENT_CANCELLED,
    INTERNAL_ERROR
}
