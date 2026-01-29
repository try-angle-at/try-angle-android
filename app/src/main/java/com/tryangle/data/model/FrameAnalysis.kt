package com.tryangle.data.model

import android.graphics.PointF

/**
 * 프레임 분석 결과를 담는 데이터 클래스
 * iOS의 FrameAnalysis에 해당
 */
data class FrameAnalysis(
    val timestamp: Long = System.currentTimeMillis(),
    val imageWidth: Int,
    val imageHeight: Int,
    
    // 포즈 정보
    val poseResult: PoseResult?,
    
    // 프레이밍 정보
    val framingInfo: FramingInfo?,
    
    // 깊이 정보 (선택적)
    val depthInfo: DepthInfo? = null
)

/**
 * 포즈 검출 결과
 */
data class PoseResult(
    val keypoints: List<Keypoint>,
    val boundingBox: BoundingBox?,
    val confidence: Float
)

/**
 * 키포인트 (관절점)
 */
data class Keypoint(
    val position: PointF,
    val confidence: Float,
    val type: KeypointType
)

/**
 * 키포인트 타입 (RTMPose 17개 관절)
 */
enum class KeypointType {
    NOSE,
    LEFT_EYE, RIGHT_EYE,
    LEFT_EAR, RIGHT_EAR,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE
}

/**
 * 바운딩 박스
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * 프레이밍 정보
 */
data class FramingInfo(
    val subjectBounds: BoundingBox,
    val headroom: Float,            // 상단 여백
    val leadingRoom: Float,         // 좌측 여백
    val isCentered: Boolean,
    val aspectRatio: Float
)

/**
 * 깊이 정보 (추후 구현)
 */
data class DepthInfo(
    val estimatedDistance: Float,   // 추정 거리 (미터)
    val depthMap: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this !== other) return false
        if (javaClass != other?.javaClass) return false

        other as DepthInfo

        if (estimatedDistance != other.estimatedDistance) return false
        if (depthMap != null) {
            if (other.depthMap == null) return false
            if (!depthMap.contentEquals(other.depthMap)) return false
        } else if (other.depthMap != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = estimatedDistance.hashCode()
        result = 31 * result + (depthMap?.contentHashCode() ?: 0)
        return result
    }
}
