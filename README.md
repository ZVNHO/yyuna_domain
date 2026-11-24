# 🍞 yuna homepage

> Nuxt 4 + Spring Boot 3로 만든 로그인 / 회원가입 / 방명록 웹사이트

🔗 **배포 주소:** http://yyuna03.cafe24.com

<br>

## 📌 프로젝트 소개
프론트엔드(Nuxt)와 백엔드(Spring Boot)를 분리해서 개발하고,
배포할 때는 **하나의 WAR 파일로 합쳐서** 카페24 Tomcat 호스팅에 올린 프로젝트입니다.

<br>

## ✨ 주요 기능

### 회원
- 회원가입 (아이디·이메일 **실시간 중복 확인**, 비밀번호 조건·일치 표시)
- 로그인 / 로그아웃 (토큰 기반 인증)
- 비밀번호 **BCrypt 암호화** 저장
- **로그인 2단계 인증**: 비밀번호 확인 후 이메일로 받은 6자리 코드 입력 (5분 유효, 5회 실패 시 만료)
- **아이디 찾기**: 가입한 이메일로 아이디 발송
- **비밀번호 재설정**: 아이디+이메일 확인 → 인증 코드 메일 → 새 비밀번호 설정 (10분 유효)
- 가입 여부가 드러나지 않도록 찾기 요청은 항상 같은 안내 문구로 응답

### 방명록
- 로그인한 회원만 글 작성
- 작성자 / 작성 시간 표시
- 회원 탈퇴 시 작성한 글 자동 삭제
- **답글 · 답글의 답글** (`@이름` 표시), 본인 글·답글 **수정** ("수정됨" 표시)
- 새 글이 올라오면 **관리자에게 알림 메일**

### 자료실
- 파일 업로드(10MB) / 다운로드 / 삭제, 파일별 답글
- 업로드 **진행률 → 서버 저장 중 → 완료** 상태 표시

### 개발 뉴스
- [GeekNews](https://news.hada.io) 공식 RSS를 서버가 1분마다 받아와 실시간으로 표시 (60초 자동 새로고침, NEW 표시)

### 내 소개
- 이름, 나이, 취미, 직업 조회 / 등록 / 수정 / 삭제 (CRUD)
- 나이를 생일 기준으로 **1초마다** 갱신 (만 나이 · 세는 나이 · 태어난 뒤 개월 수)
- 표(Grid), 달력(Calendar), 차트(ApexCharts) 화면 구성

<br>

## 🛠️ 기술 스택
| 구분 | 사용 기술 |
|---|---|
| Front-end | Nuxt 4 · Vue 3 · TypeScript · Nuxt UI · ApexCharts |
| Back-end | Spring Boot 3.1 · MyBatis · Spring Security Crypto · Spring Mail (네이버 SMTP) |
| Database | MariaDB 10.1 |
| Server | Apache Tomcat 10 · JDK 17 (WAR 배포) |
| Hosting | Cafe24 Tomcat JSP 호스팅 |
| Tools | VS Code · IntelliJ · DBeaver · FileZilla · GitHub |

<br>

## 📂 프로젝트 구조
```
toyy
├── domaim_fe          프론트엔드 (Nuxt)
│   ├── app
│   │   ├── pages      화면 (index, login, signup, find-id, reset-password, yuna)
│   │   ├── components 홈 탭(내 소개·제빵·방명록·자료실·뉴스), 비밀번호 입력 등
│   │   └── composables
│   └── scripts
│       └── deploy.ps1 검사 → 빌드 → WAR → 업로드 → 재시작 자동 배포
└── domain_be          백엔드 (Spring Boot)
    ├── sql                  운영 DB 변경 SQL (배포 전에 실행)
    └── src/main
        ├── java/com/example/demo
        │   ├── controller   API + 화면 연결(SpaController)
        │   ├── service
        │   ├── repository
        │   └── config
        └── resources
            ├── mapper       MyBatis SQL
            ├── static       빌드된 프론트 파일 (배포 때 생성, git 제외)
            ├── application.example.yaml  설정 예시 (application.yaml은 git 제외)
            └── schema.sql   테이블 생성 SQL
```

### 처음 받았을 때 설정
1. `domain_be/src/main/resources/application.example.yaml`을 `application.yaml`로 복사하고 DB 비밀번호를 채웁니다.
2. 메일 발송 계정은 홈 폴더의 `secret.yaml`(서버는 `~/secret.yaml`)에 둡니다. 네이버는 2단계 인증 후 **애플리케이션 비밀번호**를 써야 합니다.
   ```yaml
   spring:
     mail:
       username: 네이버아이디
       password: '애플리케이션비밀번호'
   ```

<br>

## 🗄️ DB 구조
| 테이블 | 설명 |
|---|---|
| `member` | 회원 (아이디, 암호화된 비밀번호, 이름, 이메일, 토큰) |
| `guestbook` | 방명록 (작성자 `member_id`, 내용, 작성일, 수정일) |
| `guestbook_reply` | 방명록 답글 (원래 답글 `parent_reply_id`로 답글의 답글) |
| `library_file` / `library_reply` | 자료실 파일 / 답글 |
| `password_reset` | 비밀번호 재설정 인증 코드 (암호화 저장) |
| `login_code` | 로그인 2단계 인증 코드 (암호화 저장) |
| `yuna` | 내 소개 (나이, 취미, 직업) |

<br>

## 🚀 배포 방법
```bash
# 1. 프론트 빌드
cd domaim_fe
pnpm install
pnpm nuxt generate

# 2. 빌드 결과를 백엔드 static 폴더로 복사
#    domaim_fe/.output/public/*  →  domain_be/src/main/resources/static/

# 3. 백엔드를 WAR로 빌드
cd domain_be
./gradlew clean bootWar      # build/libs/ROOT.war 생성

# 4. FTP로 ROOT.war를 /tomcat/webapps/ 에 업로드
```

지금은 위 과정을 `domaim_fe/scripts/deploy.ps1` 하나로 자동화했습니다.
```powershell
# 처음 한 번: 서버 비밀번호를 이 PC에 암호화 저장 (Windows DPAPI)
powershell -ExecutionPolicy Bypass -File domaim_fe\scripts\deploy.ps1 -Setup
# 배포: 타입검사·ESLint → nuxt generate → static 복사 → bootWar → 업로드 → Tomcat 정지·교체·시작 → 새 버전 확인
powershell -ExecutionPolicy Bypass -File domaim_fe\scripts\deploy.ps1
```
DB 구조가 바뀌는 배포는 `domain_be/sql/`의 SQL을 운영 DB에서 먼저 실행합니다.

<br>

## 💡 배포하면서 해결한 문제
| 문제 | 원인 | 해결 |
|---|---|---|
| 서버에서 앱이 실행되지 않음 | 카페24는 Tomcat 10.0 / JDK 17 | Spring Boot 3.5 → **3.1**, Java 21 → **17** |
| WAR 빌드 실패 | Gradle 9와 Spring Boot 3.1 플러그인 충돌 | Gradle **8.10.2**로 변경 |
| `/login` 새로고침 시 404 | Spring이 `login/index.html`을 찾지 못함 | `SpaController`로 화면 경로 연결 |
| API 요청 연결 거부 | 프론트가 `localhost:8080`으로 요청 | `apiBase`를 같은 주소(빈 값)로 변경 |
| 재배포 후 앱 실행 실패 | 반복 배포로 `OutOfMemoryError: Metaspace` | Tomcat 재시작, 배포는 한 번에 모아서 진행 |
| 배포 중 이전 버전 백업 실패 | 호스팅 용량 초과 (`Disk quota exceeded`) | 백업을 복사(cp) 대신 이동(mv)으로 1개만 유지 |
| 메일 로그인 실패 (`535 Username and Password not accepted`) | 네이버는 외부 SMTP에 일반 비밀번호를 허용하지 않음 | 2단계 인증 + 애플리케이션 비밀번호 사용 |
