package com.manas.regfiling.domain;

/**
 * The respondent submission workflow. A validity edit blocks the submission outright
 * (REJECTED_VALIDITY is terminal). A quality edit does not block it, it holds the filing for an
 * analyst explanation instead. Once every quality edit that fired has an explanation attached,
 * the filing is accepted. A filing with no edits at all goes straight to ACCEPTED.
 */
public enum FilingStatus {
    DRAFT,
    SUBMITTED,
    REJECTED_VALIDITY,
    HELD_FOR_EXPLANATION,
    ACCEPTED
}
