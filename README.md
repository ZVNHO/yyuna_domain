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
- 회원가입 (아이디 중복 확인)
- 로그인 / 로그아웃 (토큰 기반 인증)
- 비밀번호 **BCrypt 암호화** 저장

### 방명록
- 로그인한 회원만 글 작성
- 작성자 / 작성 시간 표시
- 회원 탈퇴 시 작성한 글 자동 삭제

### 내 소개
- 이름, 나이, 취미, 직업 조회 / 등록 / 수정 / 삭제 (CRUD)
- 표(Grid), 달력(Calendar), 차트(ApexCharts) 화면 구성

<br>

## 🛠️ 기술 스택
| 구분 | 사용 기술 |
|---|---|
| Front-end | Nuxt 4 · Vue 3 · TypeScript · Nuxt UI · ApexCharts |
| Back-end | Spring Boot 3.1 · MyBatis · Spring Security Crypto |
| Database | MariaDB 10.1 |
| Server | Apache Tomcat 10 · JDK 17 (WAR 배포) |
| Hosting | Cafe24 Tomcat JSP 호스팅 |
| Tools | VS Code · IntelliJ · DBeaver · FileZilla · GitHub |

<br>

## 📂 프로젝트 구조
```
toyy
├── domaim_fe          프론트엔드 (Nuxt)
│   └── app
│       ├── pages      화면 (login, signup, yuna)
│       ├── components
│       └── composables
└── domain_be          백엔드 (Spring Boot)
    └── src/main
        ├── java/com/example/demo
        │   ├── controller   API + 화면 연결(SpaController)
        │   ├── service
        │   ├── repository
        │   └── config
        └── resources
            ├── mapper       MyBatis SQL
            ├── static       빌드된 프론트 파일
            └── schema.sql   테이블 생성 SQL
```

<br>

## 🗄️ DB 구조
| 테이블 | 설명 |
|---|---|
| `member` | 회원 (아이디, 암호화된 비밀번호, 이름, 토큰) |
| `guestbook` | 방명록 (작성자 `member_id`, 내용, 작성일) |
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

<br>

## 💡 배포하면서 해결한 문제
| 문제 | 원인 | 해결 |
|---|---|---|
| 서버에서 앱이 실행되지 않음 | 카페24는 Tomcat 10.0 / JDK 17 | Spring Boot 3.5 → **3.1**, Java 21 → **17** |
| WAR 빌드 실패 | Gradle 9와 Spring Boot 3.1 플러그인 충돌 | Gradle **8.10.2**로 변경 |
| `/login` 새로고침 시 404 | Spring이 `login/index.html`을 찾지 못함 | `SpaController`로 화면 경로 연결 |
| API 요청 연결 거부 | 프론트가 `localhost:8080`으로 요청 | `apiBase`를 같은 주소(빈 값)로 변경 |
| 재배포 후 앱 실행 실패 | 반복 배포로 `OutOfMemoryError: Metaspace` | Tomcat 재시작, 배포는 한 번에 모아서 진행 |
