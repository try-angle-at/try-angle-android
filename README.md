# TryAngle Android

iOS tryAngle 앱의 Android 포팅 프로젝트

## 기술 스택

- **언어**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material3
- **카메라**: Camera2 API
- **AI**: TensorFlow Lite
- **DI**: Hilt
- **비동기**: Coroutines + Flow
- **아키텍처**: Clean Architecture + MVVM

## 프로젝트 구조 (계획)

```
app/src/main/java/com/tryangle/
├── data/
│   └── model/              # 데이터 모델
│       ├── Feedback.kt
│       ├── GateEvaluation.kt
│       └── FrameAnalysis.kt
├── domain/
│   ├── gate/              # Gate System
│   │   ├── GateSystem.kt
│   │   ├── AspectRatioGate.kt
│   │   ├── FramingGate.kt
│   │   ├── PositionGate.kt
│   │   ├── LensDistanceGate.kt
│   │   └── PoseGate.kt
│   ├── feedback/          # 피드백 엔진
│   │   └── GuideEngine.kt
│   └── analyzer/          # 분석기 (Phase 3에서 구현)
├── platform/
│   ├── camera/            # Camera2 API (Phase 2에서 구현)
│   └── ml/                # TFLite 추론 (Phase 3에서 구현)
└── presentation/
    ├── camera/            # 카메라 화면 (Phase 4에서 구현)
    ├── components/        # UI 컴포넌트
    └── theme/             # Material3 테마
```

## 개발 로드맵

### ✅ Phase 1: Foundation (완료)
- [x] Android 프로젝트 셋업
- [x] 데이터 모델 포팅 (Feedback, GateEvaluation, FrameAnalysis)
- [x] Gate System 구현 (5개 Gate)
- [x] GuideEngine 구현
- [x] 단위 테스트 작성

### ✅ Phase 2: Camera2 구현 (완료)
- [x] CameraManager 구현
- [x] 권한 처리
- [x] 60fps 프리뷰
- [x] 이미지 캡처
- [x] 이미지 처리 (YUV → RGB)
- [x] 단위 테스트 작성
- [x] **버그 수정**:
  - [x] 카메라 배율 수정 (2배 → 1배)
  - [x] 화질 개선 (1080p → 4K 지원)
  - [x] 왜곡 수정 (aspect ratio 정확도 향상)
- [x] **카메라 제어 기능**:
  - [x] 줌 제어 (Pinch-to-zoom + 슬라이더)
  - [x] 프레임레이트 조절 (30fps ↔ 60fps)
  - [x] 노출 보정
  - [x] 포커스 모드 전환

### 📅 Phase 3: AI 모델 통합 (예정)
- [ ] ONNX → TFLite 모델 변환
- [ ] RTMPoseRunner 구현
- [ ] FramingAnalyzer 구현

### 📅 Phase 4: UI 구현 (예정)
- [ ] CameraScreen (Compose)
- [ ] FeedbackOverlay
- [ ] GateProgressBar
- [ ] 통합 테스트

## 라이선스

MIT License

## 기여

이슈 및 PR은 언제든 환영합니다!
