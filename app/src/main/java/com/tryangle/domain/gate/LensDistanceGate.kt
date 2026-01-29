package com.tryangle.domain.gate

import com.tryangle.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * LensDistanceGate - 렌즈/거리 검증
 * iOS의 LensDistanceGate.swift에 해당
 * 
 * 피사체와의 거리가 적절한지 확인
 */
@Singleton
class LensDistanceGate @Inject constructor() : Gate {
    
    companion object {
        private const val IDEAL_DISTANCE_MIN = 1.5f    // 최소 1.5m
        private const val IDEAL_DISTANCE_MAX = 3.0f    // 최대 3.0m
        private const val MIN_SCORE = 0.5f
        
        // 바운딩 박스 크기 기반 거리 추정
        private const val IDEAL_SUBJECT_HEIGHT_RATIO = 0.6f  // 이미지 높이의 60%
        private const val MIN_SUBJECT_HEIGHT_RATIO = 0.3f
        private const val MAX_SUBJECT_HEIGHT_RATIO = 0.9f
    }
    
    override suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation {
        val framingInfo = analysis.framingInfo
        val depthInfo = analysis.depthInfo
        
        if (framingInfo == null) {
            return GateEvaluation(
                gateType = GateType.LENS_DISTANCE,
                passed = false,
                score = 0f,
                feedback = listOf(
                    Feedback(
                        message = "거리 정보를 찾을 수 없습니다",
                        severity = FeedbackSeverity.ERROR,
                        category = FeedbackCategory.LENS
                    )
                )
            )
        }
        
        // 깊이 정보가 있으면 실제 거리 사용, 없으면 바운딩 박스 크기로 추정
        val distance = depthInfo?.estimatedDistance 
            ?: estimateDistanceFromBounds(framingInfo.subjectBounds, analysis.imageHeight)
        
        // 점수 계산
        val score = calculateDistanceScore(distance, framingInfo.subjectBounds, analysis.imageHeight)
        val passed = score >= MIN_SCORE
        
        val feedback = buildFeedback(distance, framingInfo.subjectBounds, analysis.imageHeight, passed)
        
        return GateEvaluation(
            gateType = GateType.LENS_DISTANCE,
            passed = passed,
            score = score,
            feedback = feedback
        )
    }
    
    private fun estimateDistanceFromBounds(bounds: BoundingBox, imageHeight: Int): Float {
        val subjectHeightRatio = bounds.height / imageHeight
        
        // 피사체가 클수록 가까운 것으로 추정
        return when {
            subjectHeightRatio > 0.8f -> 1.0f
            subjectHeightRatio > 0.6f -> 2.0f
            subjectHeightRatio > 0.4f -> 3.0f
            else -> 4.0f
        }
    }
    
    private fun calculateDistanceScore(
        distance: Float,
        bounds: BoundingBox,
        imageHeight: Int
    ): Float {
        val subjectHeightRatio = bounds.height / imageHeight
        
        // 이상적인 크기 범위 내에 있는지 확인
        return when {
            subjectHeightRatio < MIN_SUBJECT_HEIGHT_RATIO -> 0.3f  // 너무 작음 (멀리 있음)
            subjectHeightRatio > MAX_SUBJECT_HEIGHT_RATIO -> 0.4f  // 너무 큼 (가까이 있음)
            else -> {
                val diff = abs(subjectHeightRatio - IDEAL_SUBJECT_HEIGHT_RATIO)
                (1f - diff / IDEAL_SUBJECT_HEIGHT_RATIO).coerceIn(0f, 1f)
            }
        }
    }
    
    private fun buildFeedback(
        distance: Float,
        bounds: BoundingBox,
        imageHeight: Int,
        passed: Boolean
    ): List<Feedback> {
        val subjectHeightRatio = bounds.height / imageHeight
        
        return buildList {
            when {
                subjectHeightRatio < MIN_SUBJECT_HEIGHT_RATIO -> {
                    add(
                        Feedback(
                            message = "피사체와의 거리가 너무 멉니다. 더 가까이 다가가주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.LENS
                        )
                    )
                }
                subjectHeightRatio > MAX_SUBJECT_HEIGHT_RATIO -> {
                    add(
                        Feedback(
                            message = "피사체와의 거리가 너무 가깝습니다. 뒤로 물러나주세요",
                            severity = FeedbackSeverity.WARNING,
                            category = FeedbackCategory.LENS
                        )
                    )
                }
                passed -> {
                    add(
                        Feedback(
                            message = "거리가 적절합니다",
                            severity = FeedbackSeverity.INFO,
                            category = FeedbackCategory.LENS
                        )
                    )
                }
            }
        }
    }
}
