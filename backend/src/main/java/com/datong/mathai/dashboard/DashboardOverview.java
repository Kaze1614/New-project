package com.datong.mathai.dashboard;

public record DashboardOverview(
    int totalMistakes,
    int mastered,
    int pendingReview,
    int todayCompleted
) {
}
