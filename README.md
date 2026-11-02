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
        │   ├── controller   API + 화면
