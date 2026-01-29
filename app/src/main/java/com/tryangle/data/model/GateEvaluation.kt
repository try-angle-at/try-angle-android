package com.tryangle.data.model

/**
 * Gate 평가 결과를 나타내는 데이터 클래스
 * iOS의 GateTypes.swift의 GateEvaluation에 해당
 */
data class GateEvaluation(
    val gateType: GateType,
    val passed: Boolean,
    val score: Float,           // 0.0 ~ 1.0
    val feedback: List<Feedback>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Gate 타입
 * iOS의 5개 Gate에 해당
 */
enum class GateType {
    ASPECT_RATIO,       // 화면 비율 검증
    FRAMING,            // 프레이밍 검증
    POSITION,           // 위치 검증
    LENS_DISTANCE,      // 렌즈/거리 검증
    POSE                // 포즈 검증
}
