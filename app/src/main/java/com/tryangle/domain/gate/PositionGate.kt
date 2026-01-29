package com.tryangle.domain.gate

import com.tryangle.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * PositionGate - 위치 검증
 * iOS의 PositionGate.swift에 해당
 * 
 * 피사체가 적절한 위치에 있는지 확인
 */
@Singleton
class PositionGate @Inject constructor() : Gate {
    
    companion object {
        private const val CENTER_TOLERANCE = 0.15f  // 중앙에서 15% 이내
        private const val MIN_SCORE = 0.5f
    }
    
    override suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation {
        val framingInfo = analysis.framingInfo
        
        if (framingInfo == null) {
            return GateEvaluation(
                gateType = GateType.POSITION,
                passed = false,
                score = 0f,
                feedback = listOf(
                    Feedback(
                        message = "위치 정보를 찾을 수 없습니다",
                        severity = FeedbackSeverity.ERROR,
                        category = FeedbackCategory.POSITION
                    )
                )
            )
        }
        
        val bounds = framingInfo.subjectBounds
        val imageCenterX = analysis.imageWidth / 2f
        val imageCenterY = analysis.imageHeight / 2f
        
        // 피사체 중심과 이미지 중심 간의 거리
        val offsetX = abs(bounds.centerX - imageCenterX) / analysis.imageWidth
        val offsetY = abs(bounds.centerY - imageCenterY) / analysis.imageHeight
        
        // 점수 계산 (중앙에 가까울수록 높은 점수)
        val scoreX = (1f - (offsetX / CENTER_TOLERANCE)).coerceIn(0f, 1f)
        val scoreY = (1f - (offsetY / CENTER_TOLERANCE)).coerceIn(0f, 1f)
        val score = (scoreX + scoreY) / 2f
        
        val passed = score >= MIN_SCORE && framingInfo.isCentered
        
        val feedback = buildFeedback(offsetX, offsetY, passed)
        
        return GateEvaluation(
            gateType = GateType.POSITION,
            passed = passed,
            score = score,
            feedback = feedback
        )
    }
    
    private fun buildFeedback(
        offsetX: Float,
        offsetY: Float,
        passed: Boolean
    ): List<Feedback> {
        return buildList {
            when {
                offsetX > CENTER_TOLERANCE -> {
                    add(
                        Feedback(
                            message = "피사체를 좌우 중앙으로 이동해주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.POSITION
                        )
                    )
                }
                offsetY > CENTER_TOLERANCE -> {
                    add(
                        Feedback(
                            message = "피사체를 상하 중앙으로 이동해주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.POSITION
                        )
                    )
                }
                passed -> {
                    add(
                        Feedback(
                            message = "위치가 적절합니다",
                            severity = FeedbackSeverity.INFO,
                            category = FeedbackCategory.POSITION
                        )
                    )
                }
            }
        }
    }
}
