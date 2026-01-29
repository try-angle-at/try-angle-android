# TryAngle Android

iOS tryAngle 앱의 Android 포팅 프로젝트

## 프로젝트 개요

TryAngle은 AI 기반 카메라 가이드 앱으로, 실시간으로 사진 구도를 분석하고 피드백을 제공합니다.

### 핵심 기능

- **실시간 포즈 검출**: RTMPose 모델을 사용한 17개 관절 검출
- **5단계 Gate 시스템**: 화면 비율, 프레이밍, 위치, 거리, 포즈 검증
- **실시간 피드백**: 사용자에게 즉각적인 구도 개선 가이드 제공
- **고해상도 촬영**: 완벽한 구도가 완성되면 고화질 사진 촬영

## 기술 스택

- **언어**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material3
- **카메라**: Camera2 API
- **AI**: TensorFlow Lite
- **DI**: Hilt
- **비동기**: Coroutines + Flow
- **아키텍처**: Clean Architecture + MVVM

## 프로젝트 구조

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

## 빌드 방법

### 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Gradle 8.2

### 빌드 실행

```bash
# 의존성 다운로드 및 빌드
./gradlew build

# 단위 테스트 실행
./gradlew test

# 디버그 APK 생성
./gradlew assembleDebug
```

## 개발 로드맵

### ✅ Phase 1: Foundation (완료)
- [x] Android 프로젝트 셋업
- [x] 데이터 모델 포팅 (Feedback, GateEvaluation, FrameAnalysis)
- [x] Gate System 구현 (5개 Gate)
- [x] GuideEngine 구현
- [x] 단위 테스트 작성

### 🔄 Phase 2: Camera2 구현 (예정)
- [ ] CameraManager 구현
- [ ] 권한 처리
- [ ] 60fps 프리뷰
- [ ] 이미지 캡처

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
