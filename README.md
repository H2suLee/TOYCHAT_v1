# TOYCHAT_v1
### 개요

- 실시간 메시징을 지원하는 채팅 애플리케이션(Websocket)
- 사용자와 관리자간의 1:1채팅 및 그룹채팅 가능
- OAuth2 기반 소셜 로그인 지원(Kakao, naver, google, github)
- Redis, Pinia, jwt를 활용하여 안전하고 빠른 서비스
- Firebase FCM 푸시알림 활용
- promtail, loki 로 app 로그 수집, prometheus 로 app 메트릭 수집, grafana로 모니터링 시스템 구축

### 기술 스택
#### FE
- Vue 3

#### BE
- Java 17
- Spring Boot
- Spring Security
- Websocket
- Logback
- Maven

#### Database & Cache
- MongoDB
- Redis

#### Deployment
- Docker & GHCR (컨테이너화 및 이미지 관리)
- Github Actions (CI/CD)
- Nginx (리버스 프록시/SSL)
- Vultr Linux/Ubuntu VPS
- systemd (인스턴스 관리 체계)

#### Monitoring / Logging
- Logback
- Loki + Promtail
- Prometheus
- Grafana

#### IDE/VCS
- Eclipse
- Visual studio code
- Github
- Notion

---
