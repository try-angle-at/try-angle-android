package com.tryangle.domain.gate

import com.tryangle.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * AspectRatioGate - 화면 비율 검증
 * iOS의 AspectRatioGate.swift에 해당
 * 
 * 이미지의 화면 비율이 적절한지 확인
 */
@Singleton
class AspectRatioGate @Inject constructor() : Gate {
    
    companion object {
        // 일반적인 사진 비율
        private val IDEAL_RATIOS = listOf(
            3f / 4f,    // 3:4 (세로)
            4f / 3f,    // 4:3 (가로)
            9f / 16f,   // 9:16 (세로)
            16f / 9f,   // 16:9 (가로)
            1f / 1f     // 1:1 (정사각형)
        )
        
        private const val RATIO_TOLERANCE = 0.1f
        private const val MIN_SCORE = 0.5f
    }
    
    override suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation {
        val currentRatio = analysis.imageWidth.toFloat() / analysis.imageHeight.toFloat()
        
        // 가장 가까운 이상적인 비율 찾기
        val closestRatio = IDEAL_RATIOS.minByOrNull { abs(it - currentRatio) }
            ?: IDEAL_RATIOS.first()
        
        val ratioDiff = abs(currentRatio - closestRatio)
        val score = (1f - (ratioDiff / RATIO_TOLERANCE)).coerceIn(0f, 1f)
        val passed = score >= MIN_SCORE
        
        val feedback = buildList {
            if (!passed) {
                add(
                    Feedback(
                        message = "화면 비율을 ${formatRatio(closestRatio)}로 조정해주세요",
                        severity = FeedbackSeverity.WARNING,
                        category = FeedbackCategory.ASPECT_RATIO
                    )
                )
            } else {
                add(
                    Feedback(
                        message = "화면 비율이 적절합니다",
                        severity = FeedbackSeverity.INFO,
                        category = FeedbackCategory.ASPECT_RATIO
                    )
                )
            }
        }
        
        return GateEvaluation(
            gateType = GateType.ASPECT_RATIO,
            passed = passed,
            score = score,
            feedback = feedback
        )
    }
    
    private fun formatRatio(ratio: Float): String {
        return when {
            abs(ratio - 3f/4f) < 0.01f -> "3:4"
            abs(ratio - 4f/3f) < 0.01f -> "4:3"
            abs(ratio - 9f/16f) < 0.01f -> "9:16"
            abs(ratio - 16f/9f) < 0.01f -> "16:9"
            abs(ratio - 1f) < 0.01f -> "1:1"
            else -> String.format("%.2f:1", ratio)
        }
    }
}
