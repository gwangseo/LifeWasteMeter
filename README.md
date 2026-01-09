# 인생낭비 측정기 (LifeWasteMeter)

<br />   

- App Name: 인생낭비 측정기
- Google Play Store: 심사 중

<img width="1432" height="1273" alt="Image" src="https://github.com/user-attachments/assets/b1b35023-6d49-4130-b982-d5131ab68bc4" />

<br />   

---

<br />    


# 📝 App Description

인생낭비 측정기는 스마트폰 사용 습관을 시각화하여 디지털 웰빙을 돕는 앱입니다. 유튜브, 인스타그램, 틱톡 등 소셜 미디어 앱에서 스크롤한 거리를 측정하고, 앱별 사용 시간을 추적하여 하루 동안 얼마나 스마트폰을 사용했는지 직관적으로 보여줍니다.

<br />   
<br />   

▶ 실시간 스크롤 거리 측정
- AccessibilityService를 활용하여 실제 스크롤 거리를 픽셀 단위로 측정
- 유튜브, 인스타그램, 틱톡 등 선택한 앱에서의 스크롤 동작을 실시간으로 추적
- 손가락으로 스크롤한 거리만큼 실제로 이동했으면 '한라산을 등산했어요!'와 같은 직관적인 표현으로 표현 

▶ 정확한 앱 사용 시간 추적
- UsageStatsManager의 `queryEvents()` API를 사용하여 디지털 웰빙 앱과 동일한 수준의 정확도 제공
- MOVE_TO_FOREGROUND와 MOVE_TO_BACKGROUND 이벤트를 추적하여 실제 앱 사용 시간 계산
- 앱별 사용 시간을 일별로 자동 저장 및 관리

▶ 두 가지 시각화 모드
- 등산 모드: 스크롤 거리를 등산 높이로 변환 (에베레스트, 백두산, 한라산 등)
- 휴지 낭비 모드: 스크롤 거리를 휴지 길이로 변환 (몇 칸의 휴지가 낭비되었는지 표시)
- 사용자가 원하는 모드를 선택하여 동기부여 방식 변경 가능

▶ 일별 통계 및 그래프
- 최근 7일간의 스크롤 거리를 기본으로 표시하고, 좌우 스크롤로 최대 30일까지 확인 가능
- 동적 차트 스케일링: 실제 데이터에 따라 250m, 500m, 750m... 최대 10,000m까지 자동 조정
- 각 바 위에 정확한 거리 값 표시
- 앱별 사용 시간을 오늘 기준으로 표시 

▶ 자동 일별 리셋
- 매일 자정에 스크롤 거리와 사용 시간이 자동으로 초기화
- 날짜 변경을 실시간으로 감지하여 정확한 일별 데이터 관리

▶ 맞춤형 설정
- 추적할 앱 선택 (기본값: 유튜브, 인스타그램, 틱톡)
- 접근성 권한 및 사용 통계 권한 관리

▶ 간편한 온보딩
- 첫 실행 시 닉네임 설정 및 필수 권한 안내
- 접근성 권한과 사용 통계 권한을 한 번에 설정 가능

<br />   
<br />   

<br />   

---

<br />    


# 👨‍💻 Developer

박광서: 1인 개발 앱 


# 주요 구현 기능

▶ 홈 화면
- 실시간 스크롤 거리 및 앱 사용 시간 표시
- 등산 모드 / 휴지 낭비 모드 전환
- 스크롤 거리에 따른 동적 메시지 표시 (등산 높이, 휴지 칸 수 등)
- 스크롤 추가 시 실시간 알림 ("방금 ##m 추가!")

▶ 온보딩 화면
- 첫 실행 시 닉네임 설정
- 접근성 권한 및 사용 통계 권한 안내 및 설정
- 권한 상태 실시간 확인

▶ 랭킹 화면
- 일별 스크롤 거리 차트 (최근 7일 기본, 30일까지 스크롤 가능)
- 동적 차트 스케일링 (250m ~ 10,000m)
- 앱별 사용 시간 표시 (오늘 기준)
- 총 사용 시간 계산

▶ 설정 화면
- 닉네임 수정
- 추적할 앱 선택 
- 접근성 권한 및 사용 통계 권한 상태 확인 및 설정 이동

▶ 백그라운드 서비스
- AccessibilityService를 통한 실시간 스크롤 이벤트 감지
- 스크롤 거리 계산 (픽셀 → 미터 변환)
- 앱 전환 시 사용 시간 자동 저장

▶ 데이터 관리
- DataStore Preferences를 활용한 로컬 데이터 저장
- 일별 데이터 자동 리셋 (날짜 변경 감지)
- UsageStatsManager를 통한 정확한 앱 사용 시간 추적

<br />   

---

<br />    


# 💻 Development Environment / Tools
- Language: Kotlin
- MinSDK: 24 (Android 7.0)
- TargetSDK: 36 (Android 15)
- CompileSDK: 36
- IDE: Android Studio
- Architecture: MVVM (Model-View-ViewModel)
- UI: Jetpack Compose, Material Design 3
- DataStore Preferences (로컬 저장)
- Android APIs:
  - AccessibilityService (스크롤 이벤트 감지)
  - UsageStatsManager (앱 사용 시간 추적)
  - PACKAGE_USAGE_STATS 권한

<br />   

---

<br />    

# 🔐 Permissions

- 접근성 권한 (BIND_ACCESSIBILITY_SERVICE): 스크롤 이벤트를 감지하기 위해 필요
- 사용 통계 권한 (PACKAGE_USAGE_STATS): 정확한 앱 사용 시간을 측정하기 위해 필요

모든 데이터는 기기 내부에만 저장되며, 외부로 전송되지 않게 처리. 

<br />   
<br />    

