package com.tryangle.domain.gate

import com.tryangle.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * FramingGate - 프레이밍 검증
 * iOS의 FramingGate.swift에 해당
 * 
 * 피사체가 적절하게 프레이밍되었는지 확인
 */
@Singleton
class FramingGate @Inject constructor() : Gate {
    
    companion object {
        private const val IDEAL_HEADROOM = 0.15f       // 상단 여백 15%
        private const val MIN_HEADROOM = 0.05f
        private const val MAX_HEADROOM = 0.25f
        
        private const val IDEAL_LEADING_ROOM = 0.1f    // 좌우 여백 10%
        private const val MIN_LEADING_ROOM = 0.05f
        
        private const val MIN_SCORE = 0.6f
    }
    
    override suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation {
        val framingInfo = analysis.framingInfo
        
        if (framingInfo == null) {
            return GateEvaluation(
                gateType = GateType.FRAMING,
                passed = false,
                score = 0f,
                feedback = listOf(
                    Feedback(
                        message = "프레이밍 정보를 찾을 수 없습니다",
                        severity = FeedbackSeverity.ERROR,
                        category = FeedbackCategory.FRAMING
                    )
                )
            )
        }
        
        // Headroom 점수 계산
        val headroomScore = calculateHeadroomScore(framingInfo.headroom)
        
        // Leading room 점수 계산
        val leadingRoomScore = calculateLeadingRoomScore(framingInfo.leadingRoom)
        
        // 전체 점수
        val score = (headroomScore + leadingRoomScore) / 2f
        val passed = score >= MIN_SCORE
        
        val feedback = buildFeedback(framingInfo, headroomScore, leadingRoomScore, passed)
        
        return GateEvaluation(
            gateType = GateType.FRAMING,
            passed = passed,
            score = score,
            feedback = feedback
        )
    }
    
    private fun calculateHeadroomScore(headroom: Float): Float {
        return when {
            headroom < MIN_HEADROOM -> 0.3f
            headroom > MAX_HEADROOM -> 0.5f
            else -> {
                val diff = abs(headroom - IDEAL_HEADROOM)
                (1f - diff / (IDEAL_HEADROOM * 2)).coerceIn(0f, 1f)
            }
        }
    }
    
    private fun calculateLeadingRoomScore(leadingRoom: Float): Float {
        return when {
            leadingRoom < MIN_LEADING_ROOM -> 0.4f
            else -> {
                val diff = abs(leadingRoom - IDEAL_LEADING_ROOM)
                (1f - diff / (IDEAL_LEADING_ROOM * 2)).coerceIn(0f, 1f)
            }
        }
    }
    
    private fun buildFeedback(
        framingInfo: FramingInfo,
        headroomScore: Float,
        leadingRoomScore: Float,
        passed: Boolean
    ): List<Feedback> {
        return buildList {
            // Headroom 피드백
            when {
                framingInfo.headroom < MIN_HEADROOM -> add(
                    Feedback(
                        message = "상단 여백이 부족합니다. 카메라를 아래로 내려주세요",
                        severity = FeedbackSeverity.WARNING,
                        category = FeedbackCategory.FRAMING
                    )
                )
                framingInfo.headroom > MAX_HEADROOM -> add(
                    Feedback(
                        message = "상단 여백이 너무 많습니다. 카메라를 위로 올려주세요",
                        severity = FeedbackSeverity.WARNING,
                        category = FeedbackCategory.FRAMING
                    )
                )
            }
            
            // Leading room 피드백
            if (framingInfo.leadingRoom < MIN_LEADING_ROOM) {
                add(
                    Feedback(
                        message = "좌우 여백이 부족합니다. 피사체를 중앙으로 이동해주세요",
                        severity = FeedbackSeverity.WARNING,
                        category = FeedbackCategory.FRAMING
                    )
                )
            }
            
            // 통과 시 긍정 피드백
            if (passed && isEmpty()) {
                add(
                    Feedback(
                        message = "프레이밍이 적절합니다",
                        severity = FeedbackSeverity.INFO,
                        category = FeedbackCategory.FRAMING
                    )
                )
            }
        }
    }
}
