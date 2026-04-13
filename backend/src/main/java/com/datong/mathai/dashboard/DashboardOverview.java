package com.datong.mathai.dashboard;

public record DashboardOverview(
    int questionBankTotal,
    int totalMistakes,
    int mastered,
    int pendingReview,
    int todayCompleted,
    int totalSolved,
    double accuracyRate,
    int criticalReviewCount,
    String weakSpotHint
) {
}
