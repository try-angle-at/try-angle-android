package com.tryangle.domain.gate

import com.tryangle.data.model.FrameAnalysis
import com.tryangle.data.model.GateEvaluation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gate System - 5단계 평가 시스템
 * iOS의 GateSystem.swift에 해당
 * 
 * 5개의 Gate를 순차적으로 실행하여 프레임을 평가
 */
@Singleton
class GateSystem @Inject constructor(
    private val aspectRatioGate: AspectRatioGate,
    private val framingGate: FramingGate,
    private val positionGate: PositionGate,
    private val lensDistanceGate: LensDistanceGate,
    private val poseGate: PoseGate
) {
    private val gates: List<Gate> = listOf(
        aspectRatioGate,
        framingGate,
        positionGate,
        lensDistanceGate,
        poseGate
    )
    
    /**
     * 모든 Gate를 평가
     * @param analysis 프레임 분석 결과
     * @return 각 Gate의 평가 결과 리스트
     */
    suspend fun evaluateAll(analysis: FrameAnalysis): List<GateEvaluation> {
        return gates.map { gate ->
            gate.evaluate(analysis)
        }
    }
    
    /**
     * 모든 Gate가 통과했는지 확인
     */
    fun allPassed(evaluations: List<GateEvaluation>): Boolean {
        return evaluations.all { it.passed }
    }
    
    /**
     * 통과한 Gate 개수
     */
    fun passedCount(evaluations: List<GateEvaluation>): Int {
        return evaluations.count { it.passed }
    }
    
    /**
     * 전체 진행률 (0.0 ~ 1.0)
     */
    fun progress(evaluations: List<GateEvaluation>): Float {
        if (evaluations.isEmpty()) return 0f
        return passedCount(evaluations).toFloat() / evaluations.size.toFloat()
    }
}
