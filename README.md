# KMPBook

Kotlin Multiplatform 기반 앱 프로젝트. Android / iOS 공통 코드베이스.

---

## 앱 목록

### 1. Body Fit MVP

AI 기반 신체 치수 측정 도구.

- 카메라로 촬영한 사진에서 ML Kit Pose Detection으로 어깨·힙 위치 자동 감지
- 기준 물체(카드, A4 등) 대비 픽셀→cm 변환
- 흉부·둔부 둘레 계산 및 한국식 의류 사이즈(44~99, 컵 사이즈) 추정
- CameraX + Google ML Kit + Compose Multiplatform

> 현재 코드베이스에서 제거됨. git 히스토리(`feat(app): initialize kotlin multiplatform android and ios applications`) 참고.

---

### 2. 급식

NEIS 오픈 API를 이용한 학교 급식 조회 앱.

- 학교 이름 검색 → 선택 저장
- 날짜별 급식 메뉴 조회 (조식·중식·석식)
- 열량 및 영양 정보 표시
- 모노크롬 미니멀리즘 디자인

**사용 API**: [NEIS 오픈 API](https://open.neis.go.kr) — `schoolInfo`, `mealServiceDietInfo`

> API 키 없이도 하루 1000건까지 무료 호출 가능. 앱 내 "API 키" 버튼에서 입력.

---

## 프로젝트 구조

```
composeApp/
  src/
    commonMain/   # 공통 로직 및 UI (Compose Multiplatform)
    androidMain/  # Android 전용 구현
    iosMain/      # iOS 전용 구현
iosApp/           # Xcode 프로젝트
```

## 빌드

```shell
# Android debug APK
./gradlew :composeApp:assembleDebug

# Android release APK
./gradlew :composeApp:assembleRelease
```

iOS는 `/iosApp` 디렉토리를 Xcode에서 열어 실행.

## 기술 스택

| 항목 | 내용 |
|------|------|
| UI | Compose Multiplatform + Material3 |
| HTTP | Ktor Client (OkHttp / Darwin) |
| 직렬화 | kotlinx.serialization |
| 날짜 | kotlinx-datetime |
| 설정 저장 | multiplatform-settings |
| 빌드 | Kotlin 2.3.20 / AGP 9.1.0 |