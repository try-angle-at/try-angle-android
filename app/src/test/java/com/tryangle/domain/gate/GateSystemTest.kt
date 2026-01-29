package com.tryangle.domain.gate

import android.graphics.PointF
import com.tryangle.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * GateSystem 단위 테스트
 * iOS와 동일한 동작을 검증
 */
class GateSystemTest {
    
    private lateinit var gateSystem: GateSystem
    
    @Before
    fun setup() {
        gateSystem = GateSystem(
            AspectRatioGate(),
            FramingGate(),
            PositionGate(),
            LensDistanceGate(),
            PoseGate()
        )
    }
    
    @Test
    fun `test gate system evaluates all gates`() = runTest {
        // Given: 완벽한 분석 결과
        val analysis = createPerfectAnalysis()
        
        // When: 모든 Gate 평가
        val evaluations = gateSystem.evaluateAll(analysis)
        
        // Then: 5개의 평가 결과
        assertEquals(5, evaluations.size)
    }
    
    @Test
    fun `test all gates pass with perfect analysis`() = runTest {
        // Given: 완벽한 분석 결과
        val analysis = createPerfectAnalysis()
        
        // When
        val evaluations = gateSystem.evaluateAll(analysis)
        
        // Then: 모든 Gate 통과
        assertTrue(gateSystem.allPassed(evaluations))
        assertEquals(5, gateSystem.passedCount(evaluations))
        assertEquals(1.0f, gateSystem.progress(evaluations), 0.01f)
    }
    
    @Test
    fun `test progress calculation`() = runTest {
        // Given: 부분적 성공
        val analysis = createPartialAnalysis()
        
        // When
        val evaluations = gateSystem.evaluateAll(analysis)
        
        // Then: 진행률 0~1 사이
        val progress = gateSystem.progress(evaluations)
        assertTrue(progress >= 0f && progress <= 1f)
    }
    
    @Test
    fun `test aspect ratio gate passes with 16-9 ratio`() = runTest {
        // Given: 16:9 비율
        val analysis = FrameAnalysis(
            imageWidth = 1920,
            imageHeight = 1080,
            poseResult = null,
            framingInfo = null
        )
        
        // When
        val gate = AspectRatioGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then
        assertTrue(evaluation.passed)
        assertTrue(evaluation.score > 0.8f)
    }
    
    @Test
    fun `test framing gate fails without framing info`() = runTest {
        // Given: 프레이밍 정보 없음
        val analysis = FrameAnalysis(
            imageWidth = 1920,
            imageHeight = 1080,
            poseResult = null,
            framingInfo = null
        )
        
        // When
        val gate = FramingGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then: 실패
        assertFalse(evaluation.passed)
        assertEquals(0f, evaluation.score, 0.01f)
    }
    
    @Test
    fun `test pose gate fails without pose result`() = runTest {
        // Given: 포즈 정보 없음
        val analysis = FrameAnalysis(
            imageWidth = 1920,
            imageHeight = 1080,
            poseResult = null,
            framingInfo = null
        )
        
        // When
        val gate = PoseGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then: 실패
        assertFalse(evaluation.passed)
    }

    @Test
    fun `test lens distance gate with valid distance`() = runTest {
        // Given: 적절한 크기의 피사체 (화면의 약 60%)
        val analysis = FrameAnalysis(
            imageWidth = 1000,
            imageHeight = 1000,
            poseResult = null,
            framingInfo = FramingInfo(
                subjectBounds = BoundingBox(200f, 200f, 800f, 800f), // height = 600 (60%)
                headroom = 0.15f,
                leadingRoom = 0.1f,
                isCentered = true,
                aspectRatio = 1f
            )
        )
        
        // When
        val gate = LensDistanceGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then
        assertTrue(evaluation.passed)
    }

    @Test
    fun `test lens distance gate fails when subject is too small`() = runTest {
        // Given: 너무 작은 피사체 (화면의 5%)
        val analysis = FrameAnalysis(
            imageWidth = 1000,
            imageHeight = 1000,
            poseResult = null,
            framingInfo = FramingInfo(
                subjectBounds = BoundingBox(450f, 450f, 500f, 500f), // height = 50 (5%)
                headroom = 0.5f,
                leadingRoom = 0.1f,
                isCentered = true,
                aspectRatio = 1f
            )
        )
        
        // When
        val gate = LensDistanceGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then
        assertFalse(evaluation.passed)
    }

    @Test
    fun `test position gate with centered subject`() = runTest {
        // Given: 중앙에 위치한 피사체
        val analysis = FrameAnalysis(
            imageWidth = 1000,
            imageHeight = 1000,
            poseResult = null,
            framingInfo = FramingInfo(
                subjectBounds = BoundingBox(400f, 400f, 600f, 600f), // center is 500, 500
                headroom = 0.15f,
                leadingRoom = 0.1f,
                isCentered = true,
                aspectRatio = 1f
            )
        )
        
        // When
        val gate = PositionGate()
        val evaluation = gate.evaluate(analysis)
        
        // Then
        assertTrue(evaluation.passed)
    }

    @Test
    fun `test gate system failed when any gate fails`() = runTest {
        // Given: 하나만 실패하는 분석 결과
        val analysis = createPartialAnalysis()
        
        // When
        val evaluations = gateSystem.evaluateAll(analysis)
        
        // Then
        assertFalse(gateSystem.allPassed(evaluations))
        assertTrue(gateSystem.passedCount(evaluations) < 5)
    }
    
    // Helper 함수들
    
    private fun createPerfectAnalysis(): FrameAnalysis {
        return FrameAnalysis(
            imageWidth = 1920,
            imageHeight = 1080,
            poseResult = createGoodPoseResult(),
            framingInfo = createGoodFramingInfo()
        )
    }
    
    private fun createPartialAnalysis(): FrameAnalysis {
        return FrameAnalysis(
            imageWidth = 1920,
            imageHeight = 1080,
            poseResult = createWeakPoseResult(),
            framingInfo = createBadFramingInfo()
        )
    }
    
    private fun createGoodPoseResult(): PoseResult {
        return PoseResult(
            keypoints = createFullKeypoints(),
            boundingBox = BoundingBox(
                left = 500f,
                top = 200f,
                right = 1400f,
                bottom = 900f
            ),
            confidence = 0.9f
        )
    }
    
    private fun createWeakPoseResult(): PoseResult {
        return PoseResult(
            keypoints = createPartialKeypoints(),
            boundingBox = BoundingBox(
                left = 800f,
                top = 400f,
                right = 1100f,
                bottom = 600f
            ),
            confidence = 0.4f
        )
    }
    
    private fun createGoodFramingInfo(): FramingInfo {
        return FramingInfo(
            subjectBounds = BoundingBox(
                left = 500f,
                top = 200f,
                right = 1400f,
                bottom = 900f
            ),
            headroom = 0.15f,
            leadingRoom = 0.1f,
            isCentered = true,
            aspectRatio = 16f / 9f
        )
    }
    
    private fun createBadFramingInfo(): FramingInfo {
        return FramingInfo(
            subjectBounds = BoundingBox(
                left = 100f,
                top = 50f,
                right = 400f,
                bottom = 200f
            ),
            headroom = 0.5f,  // 너무 큼
            leadingRoom = 0.01f,  // 너무 작음
            isCentered = false,
            aspectRatio = 16f / 9f
        )
    }
    
    private fun createFullKeypoints(): List<Keypoint> {
        return KeypointType.values().map { type ->
            Keypoint(
                position = PointF(960f, 540f),
                confidence = 0.9f,
                type = type
            )
        }
    }
    
    private fun createPartialKeypoints(): List<Keypoint> {
        return listOf(
            Keypoint(PointF(960f, 300f), 0.8f, KeypointType.NOSE),
            Keypoint(PointF(900f, 400f), 0.3f, KeypointType.LEFT_SHOULDER),
            Keypoint(PointF(1020f, 400f), 0.3f, KeypointType.RIGHT_SHOULDER)
        )
    }
}
