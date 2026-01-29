package com.tryangle.domain.feedback

import com.tryangle.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * GuideEngine 단위 테스트
 */
class GuideEngineTest {
    
    private lateinit var guideEngine: GuideEngine
    
    @Before
    fun setup() {
        guideEngine = GuideEngine()
    }
    
    @Test
    fun `test priority feedback returns most severe`() {
        // Given: 여러 평가 결과
        val evaluations = listOf(
            GateEvaluation(
                gateType = GateType.ASPECT_RATIO,
                passed = true,
                score = 0.9f,
                feedback = listOf(
                    Feedback("Good", FeedbackSeverity.INFO, FeedbackCategory.ASPECT_RATIO)
                )
            ),
            GateEvaluation(
                gateType = GateType.FRAMING,
                passed = false,
                score = 0.3f,
                feedback = listOf(
                    Feedback("Bad framing", FeedbackSeverity.ERROR, FeedbackCategory.FRAMING)
                )
            )
        )
        
        // When
        val priorityFeedback = guideEngine.getPriorityFeedback(evaluations)
        
        // Then: 가장 심각한 피드백
        assertNotNull(priorityFeedback)
        assertEquals(FeedbackSeverity.ERROR, priorityFeedback?.severity)
    }
    
    @Test
    fun `test progress message for complete success`() {
        // Given: 모든 Gate 통과
        val evaluations = List(5) { index ->
            GateEvaluation(
                gateType = GateType.values()[index],
                passed = true,
                score = 0.9f,
                feedback = emptyList()
            )
        }
        
        // When
        val message = guideEngine.getProgressMessage(evaluations)
        
        // Then
        assertTrue(message.contains("완벽") || message.contains("촬영"))
    }
    
    @Test
    fun `test progress message for zero progress`() {
        // Given: 모든 Gate 실패
        val evaluations = List(5) { index ->
            GateEvaluation(
                gateType = GateType.values()[index],
                passed = false,
                score = 0.2f,
                feedback = emptyList()
            )
        }
        
        // When
        val message = guideEngine.getProgressMessage(evaluations)
        
        // Then
        assertTrue(message.contains("시작") || message.contains("조정"))
    }
    
    @Test
    fun `test group feedback by category`() {
        // Given
        val evaluations = listOf(
            GateEvaluation(
                gateType = GateType.FRAMING,
                passed = false,
                score = 0.3f,
                feedback = listOf(
                    Feedback("Issue 1", FeedbackSeverity.WARNING, FeedbackCategory.FRAMING),
                    Feedback("Issue 2", FeedbackSeverity.ERROR, FeedbackCategory.FRAMING)
                )
            ),
            GateEvaluation(
                gateType = GateType.POSE,
                passed = false,
                score = 0.4f,
                feedback = listOf(
                    Feedback("Pose issue", FeedbackSeverity.WARNING, FeedbackCategory.POSE)
                )
            )
        )
        
        // When
        val grouped = guideEngine.groupFeedbackByCategory(evaluations)
        
        // Then
        assertEquals(2, grouped[FeedbackCategory.FRAMING]?.size)
        assertEquals(1, grouped[FeedbackCategory.POSE]?.size)
    }
}
