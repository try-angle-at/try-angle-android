package com.tryangle.data.model

/**
 * 피드백 메시지를 나타내는 데이터 클래스
 * iOS의 Feedback.swift에 해당
 */
data class Feedback(
    val message: String,
    val severity: FeedbackSeverity,
    val category: FeedbackCategory,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 피드백 심각도
 */
enum class FeedbackSeverity {
    INFO,       // 정보성 메시지
    WARNING,    // 경고
    ERROR       // 오류
}

/**
 * 피드백 카테고리
 */
enum class FeedbackCategory {
    ASPECT_RATIO,   // 화면 비율
    FRAMING,        // 프레이밍
    POSITION,       // 위치
    LENS,           // 렌즈
    POSE            // 포즈
}
