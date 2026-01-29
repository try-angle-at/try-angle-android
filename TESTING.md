# Phase 1 테스트 가이드

## 현재 상태

✅ **프로젝트 파일 생성 완료** (20개 파일)
- Gradle 빌드 설정
- 데이터 모델 (Feedback, GateEvaluation, FrameAnalysis)
- Gate System (5개 Gate)
- GuideEngine
- 단위 테스트 (14개 테스트)

## 테스트 방법

### 옵션 1: Android Studio 사용 (권장) 🎯

Android Studio를 사용하면 가장 쉽고 빠르게 테스트할 수 있습니다.

#### 1. Android Studio 설치 확인

[Android Studio 다운로드](https://developer.android.com/studio)

최소 요구사항:
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17 (Android Studio에 포함)
- 8GB RAM 이상 권장

#### 2. 프로젝트 열기

1. Android Studio 실행
2. **File → Open** 선택
3. **`C:\Users\user\try-angle-android`** 폴더 선택
4. **OK** 클릭

#### 3. Gradle Sync 대기

프로젝트를 처음 열면:
- Android Studio가 자동으로 Gradle Wrapper 다운로드
- 모든 의존성 다운로드 (약 2-5분 소요)
- 하단에 "Gradle Sync" 진행 상황 표시

#### 4. 테스트 실행

**방법 A: UI에서 실행**
1. 좌측 **Project** 패널에서:
   ```
   app → src → test → java → com.tryangle.domain.gate → GateSystemTest
   ```
2. `GateSystemTest` 우클릭
3. **Run 'GateSystemTest'** 선택
4. 결과 확인:
   ```
   ✅ 10 tests passed
   ✅ 0 tests failed
   ```

**방법 B: 터미널에서 실행**
1. Android Studio 하단 **Terminal** 탭 클릭
2. 다음 명령어 실행:
   ```bash
   ./gradlew test
   ```

### 옵션 2: 명령줄에서 테스트 (고급)

**전제조건**:
- JDK 17 설치 필요
- Gradle 설치 또는 Android Studio를 통한 Gradle Wrapper 다운로드

#### Windows PowerShell

```powershell
cd C:\Users\user\try-angle-android

# Gradle Wrapper가 있다면
.\gradlew.bat test

# 전체 빌드
.\gradlew.bat build
```

> **Note**: 현재 Gradle Wrapper JAR 파일이 없어서 직접 실행 불가능합니다.
> Android Studio를 사용하면 자동으로 다운로드됩니다.

## 예상 테스트 결과

### ✅ 성공 시

```
> Task :app:compileDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest

GateSystemTest
  ✓ test gate system evaluates all gates
  ✓ test all gates pass with perfect analysis
  ✓ test progress calculation
  ✓ test aspect ratio gate passes with 16-9 ratio
  ✓ test framing gate fails without framing info
  ✓ test pose gate fails without pose result
  ... (4 more tests)

GuideEngineTest
  ✓ test priority feedback returns most severe
  ✓ test progress message for complete success
  ✓ test progress message for zero progress
  ✓ test group feedback by category

BUILD SUCCESSFUL in 45s
14 actionable tasks: 14 executed
```

### 검증 내용

테스트가 성공하면 다음을 확인할 수 있습니다:

| 항목 | 검증 내용 |
|------|-----------|
| **데이터 모델** | Feedback, GateEvaluation, FrameAnalysis가 올바르게 정의됨 |
| **Gate System** | 5개 Gate가 iOS와 동일하게 동작 |
| **AspectRatioGate** | 16:9, 4:3 등 화면 비율 검증 로직 정상 |
| **FramingGate** | Headroom, Leading room 계산 정확 |
| **PositionGate** | 중앙 위치 검증 로직 정상 |
| **LensDistanceGate** | 거리 추정 로직 정상 |
| **PoseGate** | 키포인트 가시성 검증 정상 |
| **GuideEngine** | 우선순위 피드백 생성 정상 |
| **Hilt DI** | 의존성 주입 설정 정상 |

## 문제 해결

### 문제 1: "SDK location not found"

**원인**: Android SDK 경로가 설정되지 않음

**해결**:
- `local.properties` 파일 확인 (이미 생성됨 ✅)
- SDK 경로가 올바른지 확인: `C:\Users\user\AppData\Local\Android\Sdk`

### 문제 2: "gradle-wrapper.jar not found"

**원인**: Gradle Wrapper 파일이 다운로드되지 않음

**해결**:
- Android Studio에서 프로젝트 열기
- **File → Sync Project with Gradle Files** 클릭
- Android Studio가 자동으로 다운로드

### 문제 3: "Gradle JDK not found"

**원인**: JDK 17이 설치되지 않음

**해결**:
- Android Studio → **Settings** (Ctrl+Alt+S)
- **Build, Execution, Deployment → Build Tools → Gradle**
- **Gradle JDK** 드롭다운에서 **Download JDK** 선택
- JDK 17 다운로드 및 설치

### 문제 4: 빌드가 느림

**해결**: `gradle.properties` 파일 생성 (프로젝트 루트):

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
kotlin.code.style=official
android.useAndroidX=true
```

## 다음 단계

테스트가 성공하면:

1. ✅ **Phase 1 완료 확인**
2. 🚀 **Phase 2 시작**: Camera2 구현
   - CameraManager.kt
   - 권한 처리
   - 프리뷰 스트림
   - 이미지 캡처

---

## 빠른 시작 체크리스트

- [ ] Android Studio 설치 확인
- [ ] 프로젝트 열기 (`C:\Users\user\try-angle-android`)
- [ ] Gradle Sync 완료 대기
- [ ] 단위 테스트 실행 (`./gradlew test`)
- [ ] 14개 테스트 모두 통과 확인
- [ ] Phase 2로 진행 준비 완료! 🎉
