***

## 💬 TOYCHAT_v1

<img width="600" alt="Image" src="https://github.com/user-attachments/assets/deb51809-ff7d-4f1e-bf54-5d05033d2df4" />

### 개요
**TOYCHAT**은 웹소켓을 활용한 실시간 채팅 및 관리 프로그램입니다.

저의 기술 공부를 위한 열정과 실험 정신이 고스란히 담겨있습니다.


### 주요 기능
- Websocket 을 이용한 실시간 채팅/관리 구현
- 사용자와 관리자간의 1:1채팅 및 그룹채팅 가능
- OAuth2 기반 소셜 로그인 지원(Kakao, naver, google, github)
- 영구 저장용 MongoDB 와 캐싱용 Redis를 혼합한 하이브리드DB 환경으로 성능 및 효율 최적화
- Redis, Pinia, jwt를 활용하여 안전하고 빠른 서비스
- Firebase Admin SDK 및 FCM 을 활용하여 푸시 알림 전송 및 디바이스 토큰 관리 기능을 구현
- promtail, loki 로 app 로그 수집, prometheus 로 app 메트릭 수집, grafana로 모니터링 시스템 구축
- systemd 도입으로 여러 인스턴스 관리 체계 구축, nginx 도입으로 서브 도메인 관리, 리버스 프록시 적용
- Github Actions 도입하여 CI/CD 체계 구축. push 시 자동 빌드→배포→헬스체크 진행, 롤백 트리거 시 롤백 수행. 
- GHCR 도입으로 외부에서 Docker image 관리


### 기술 스택
#### FE
- Vue 3

#### BE
- Java 17
- Spring Boot
- Spring Security

#### Database & Cache
- MongoDB
- Redis

#### Deployment
- Mavan
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

### 향후 도전 및 목표
- (v2) EC2, Jenkins 활용하여 CI/CD 구축 , Vue 3 → React 전환

***
