package com.tryangle.domain.feedback

import com.tryangle.data.model.Feedback
import com.tryangle.data.model.FeedbackCategory
import com.tryangle.data.model.FeedbackSeverity
import com.tryangle.data.model.GateEvaluation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GuideEngine - 가이드 메시지 생성 엔진
 * iOS의 GuideEngine.swift에 해당
 * 
 * Gate 평가 결과를 기반으로 사용자에게 가이드 메시지 제공
 */
@Singleton
class GuideEngine @Inject constructor() {
    
    /**
     * Gate 평가 결과를 기반으로 우선순위가 가장 높은 피드백 반환
     */
    fun getPriorityFeedback(evaluations: List<GateEvaluation>): Feedback? {
        // 실패한 Gate 중 점수가 가장 낮은 것 찾기
        val failedGate = evaluations
            .filter { !it.passed }
            .minByOrNull { it.score }
        
        // 실패한 Gate의 가장 심각한 피드백 반환
        return failedGate?.feedback
            ?.maxByOrNull { it.severity.ordinal }
    }
    
    /**
     * 모든 피드백을 카테고리별로 그룹화
     */
    fun groupFeedbackByCategory(evaluations: List<GateEvaluation>): Map<FeedbackCategory, List<Feedback>> {
        val allFeedback = evaluations.flatMap { it.feedback }
        return allFeedback.groupBy { it.category }
    }
    
    /**
     * 진행 상황 메시지 생성
     */
    fun getProgressMessage(evaluations: List<GateEvaluation>): String {
        val passedCount = evaluations.count { it.passed }
        val totalCount = evaluations.size
        
        return when {
            passedCount == 0 -> "시작하기 - 카메라 위치를 조정해주세요"
            passedCount == totalCount -> "완벽합니다! 사진을 촬영하세요"
            passedCount >= totalCount * 0.8 -> "거의 다 됐습니다! ${totalCount - passedCount}단계 남음"
            passedCount >= totalCount * 0.5 -> "$passedCount/$totalCount 단계 완료"
            else -> "조금씩 조정해주세요 ($passedCount/$totalCount)"
        }
    }
    
    /**
     * 실패한 Gate들의 요약 메시지
     */
    fun getFailedGatesSummary(evaluations: List<GateEvaluation>): List<String> {
        return evaluations
            .filter { !it.passed }
            .map { evaluation ->
                val gateName = when (evaluation.gateType) {
                    com.tryangle.data.model.GateType.ASPECT_RATIO -> "화면 비율"
                    com.tryangle.data.model.GateType.FRAMING -> "프레이밍"
                    com.tryangle.data.model.GateType.POSITION -> "위치"
                    com.tryangle.data.model.GateType.LENS_DISTANCE -> "거리"
                    com.tryangle.data.model.GateType.POSE -> "포즈"
                }
                "$gateName 조정 필요"
            }
    }
}
