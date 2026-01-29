package com.tryangle.domain.gate

import com.tryangle.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PoseGate - 포즈 검증
 * iOS의 PoseGate.swift에 해당
 * 
 * 피사체의 포즈가 적절한지 확인
 */
@Singleton
class PoseGate @Inject constructor() : Gate {
    
    companion object {
        private const val MIN_CONFIDENCE = 0.5f     // 최소 신뢰도
        private const val MIN_SCORE = 0.6f
        
        // 주요 키포인트들
        private val CRITICAL_KEYPOINTS = setOf(
            KeypointType.NOSE,
            KeypointType.LEFT_SHOULDER,
            KeypointType.RIGHT_SHOULDER,
            KeypointType.LEFT_HIP,
            KeypointType.RIGHT_HIP
        )
    }
    
    override suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation {
        val poseResult = analysis.poseResult
        
        if (poseResult == null) {
            return GateEvaluation(
                gateType = GateType.POSE,
                passed = false,
                score = 0f,
                feedback = listOf(
                    Feedback(
                        message = "포즈를 감지할 수 없습니다",
                        severity = FeedbackSeverity.ERROR,
                        category = FeedbackCategory.POSE
                    )
                )
            )
        }
        
        // 전체 신뢰도 확인
        if (poseResult.confidence < MIN_CONFIDENCE) {
            return GateEvaluation(
                gateType = GateType.POSE,
                passed = false,
                score = poseResult.confidence,
                feedback = listOf(
                    Feedback(
                        message = "포즈 감지 신뢰도가 낮습니다. 조명을 확인해주세요",
                        severity = FeedbackSeverity.WARNING,
                        category = FeedbackCategory.POSE
                    )
                )
            )
        }
        
        // 주요 키포인트 검증
        val criticalKeypoints = poseResult.keypoints.filter { 
            it.type in CRITICAL_KEYPOINTS 
        }
        
        val visibleCriticalCount = criticalKeypoints.count { 
            it.confidence >= MIN_CONFIDENCE 
        }
        
        // 점수 계산
        val keypointScore = visibleCriticalCount.toFloat() / CRITICAL_KEYPOINTS.size.toFloat()
        val score = (poseResult.confidence + keypointScore) / 2f
        val passed = score >= MIN_SCORE
        
        val feedback = buildFeedback(poseResult, visibleCriticalCount, passed)
        
        return GateEvaluation(
            gateType = GateType.POSE,
            passed = passed,
            score = score,
            feedback = feedback
        )
    }
    
    private fun buildFeedback(
        poseResult: PoseResult,
        visibleCriticalCount: Int,
        passed: Boolean
    ): List<Feedback> {
        return buildList {
            when {
                visibleCriticalCount < 3 -> {
                    add(
                        Feedback(
                            message = "주요 신체 부위가 가려져 있습니다. 전신이 보이도록 조정해주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.POSE
                        )
                    )
                }
                poseResult.confidence < 0.7f -> {
                    add(
                        Feedback(
                            message = "포즈 감지가 불안정합니다. 움직임을 최소화해주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.POSE
                        )
                    )
                }
                passed -> {
                    add(
                        Feedback(
                            message = "포즈가 적절합니다",
                            severity = FeedbackSeverity.INFO,
                            category = FeedbackCategory.POSE
                        )
                    )
                }
            }
        }
    }
}
