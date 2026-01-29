package com.tryangle.domain.gate

import com.tryangle.data.model.FrameAnalysis
import com.tryangle.data.model.GateEvaluation

/**
 * Gate 인터페이스
 * 각 Gate는 프레임 분석 결과를 평가하고 GateEvaluation을 반환
 */
interface Gate {
    suspend fun evaluate(analysis: FrameAnalysis): GateEvaluation
}
