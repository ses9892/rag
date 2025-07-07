# LangChain4j + Spring Boot WebSocket 스트리밍 챗봇 프로젝트

## 프로젝트 개요
- **프로젝트명**: RAG (Retrieval-Augmented Generation) 챗봇
- **기술 스택**: Spring Boot 3.4.8-SNAPSHOT + LangChain4j 1.1.0-beta7 + OpenAI GPT-4o
- **주요 기능**: WebSocket 기반 실시간 스트리밍 AI 챗봇
- **프로젝트 경로**: `/Users/jangjinho/cursor/langChain4j/rag`

## 핵심 기술 스택

### 1. LangChain4j 라이브러리
```xml
<!-- 핵심 LangChain4j 의존성 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>1.1.0-beta7</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>1.1.0-beta7</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-reactor</artifactId>
    <version>1.1.0-beta7</version>
</dependency>
```

### 2. Spring Boot WebSocket
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-messaging</artifactId>
</dependency>
```

### 3. H2 데이터베이스 (인메모리)
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 프로젝트 아키텍처

### High Level 아키텍처 선택
- **LangChain4j High Level API 사용**: `@AiService` 어노테이션 기반
- **이유**: 복잡성 감소, 보일러플레이트 코드 최소화, 선언적 API

### 스트리밍 전용 시스템
- **동기 방식 완전 제거**: 모든 AI 응답을 스트리밍으로 처리
- **WebSocket 기반**: 실시간 양방향 통신
- **Reactor 활용**: 비동기 스트리밍 처리

## 핵심 컴포넌트

### 1. AI Service Interface
**파일**: `src/main/java/com/langchain/rag/service/ChatAssistant.java`
```java
@AiService
public interface ChatAssistant {
    @SystemMessage("당신은 친절하고 도움이 되는 한국어 AI 어시스턴트입니다. 항상 정중하고 유용한 답변을 제공해주세요.")
    Flux<String> chatStream(String userMessage);
}
```

### 2. WebSocket 핸들러 (스트리밍 전용)
**파일**: `src/main/java/com/langchain/rag/websocket/ChatWebSocketHandler.java`
- **모든 메시지를 스트리밍으로 처리**
- **동기 방식 완전 제거됨**
- **세션 관리 및 브로드캐스트 기능**
- **실시간 토큰 단위 스트리밍**

### 3. WebSocket 설정
**파일**: `src/main/java/com/langchain/rag/config/WebSocketConfig.java`
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(chatAssistant), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

### 4. REST API (백업용)
**파일**: `src/main/java/com/langchain/rag/controller/ChatController.java`
- **HTTP SSE 기반 스트리밍**
- **테스트 및 디버깅용**

### 5. 전역 예외 처리
**파일**: `src/main/java/com/langchain/rag/exception/ChatExceptionHandler.java`
- **@ControllerAdvice 기반**
- **통일된 에러 응답 형식**

## 설정 파일

### application.properties
```properties
# H2 데이터베이스 설정
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true

# OpenAI ChatModel 설정
langchain4j.open-ai.chat-model.api-key=${OPENAI_API_KEY}
langchain4j.open-ai.chat-model.model-name=gpt-4o
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true

# OpenAI StreamingChatModel 설정 (스트리밍용)
langchain4j.open-ai.streaming-chat-model.api-key=${OPENAI_API_KEY}
langchain4j.open-ai.streaming-chat-model.model-name=gpt-4o
langchain4j.open-ai.streaming-chat-model.log-requests=true
langchain4j.open-ai.streaming-chat-model.log-responses=true
```

### API 키 관리
- **파일**: `api.key` (gitignore에 포함)
- **환경변수**: `OPENAI_API_KEY`

## 사용자 인터페이스

### 스트리밍 전용 웹 UI
**파일**: `src/main/resources/static/index.html`

**주요 특징**:
- **WebSocket 자동 연결**: 페이지 로드 시 즉시 연결
- **실시간 연결 상태 표시**: 연결됨/연결 중/연결 끊김
- **자동 재연결 기능**: 연결 끊김 시 자동 복구
- **스트리밍 애니메이션**: 답변 생성 중 깜빡이는 커서
- **반응형 디자인**: 모바일/데스크톱 대응

**UI 흐름**:
1. 페이지 로드 → WebSocket 자동 연결
2. 사용자 메시지 입력 → 즉시 스트리밍 시작
3. AI 응답을 토큰 단위로 실시간 출력
4. 연결 끊김 시 자동 재연결 시도

## 개발 과정에서 해결한 주요 문제들

### 1. 스트리밍 설정 문제
**문제**: `streamingChatModel cannot be null` 오류
**해결**: `application.properties`에 StreamingChatModel 별도 설정 추가

### 2. WebSocket 의존성 문제
**문제**: WebSocket 관련 import 오류
**해결**: Spring WebSocket, Messaging 의존성 추가

### 3. 아키텍처 단순화
**문제**: 동기/스트리밍 혼재로 인한 복잡성
**해결**: 스트리밍 전용으로 완전 통일, 동기 방식 제거

### 4. UI 복잡성 문제
**문제**: 다중 테스트 UI로 인한 사용성 저하
**해결**: WebSocket 스트리밍 전용 단순 UI로 통일

## 프로젝트 구조
```
rag/
├── src/main/java/com/langchain/rag/
│   ├── RagApplication.java                    # 메인 애플리케이션
│   ├── service/
│   │   └── ChatAssistant.java                # AI 서비스 인터페이스
│   ├── controller/
│   │   ├── ChatController.java               # REST API (백업용)
│   │   └── WebSocketStatusController.java    # WebSocket 상태 관리
│   ├── websocket/
│   │   └── ChatWebSocketHandler.java         # WebSocket 핸들러
│   ├── config/
│   │   └── WebSocketConfig.java              # WebSocket 설정
│   └── exception/
│       └── ChatExceptionHandler.java         # 전역 예외 처리
├── src/main/resources/
│   ├── application.properties                # 설정 파일
│   └── static/
│       └── index.html                        # 웹 UI
├── pom.xml                                   # Maven 의존성
├── .cursorrules                              # 개발 가이드
├── api.key                                   # API 키 (gitignore)
└── PROJECT_HISTORY.md                        # 이 문서
```

## 현재 상태 및 기능

### ✅ 완료된 기능
1. **LangChain4j + OpenAI GPT-4o 통합**
2. **WebSocket 기반 실시간 스트리밍**
3. **스트리밍 전용 아키텍처**
4. **자동 연결/재연결 기능**
5. **반응형 웹 UI**
6. **전역 예외 처리**
7. **세션 관리 및 브로드캐스트**

### 🎯 핵심 특징
- **완전한 스트리밍 전용 시스템**: 동기 방식은 완전히 제거됨, 추가 금지
- **실시간 토큰 단위 응답**: 사용자 경험 최적화
- **자동 연결 관리**: 끊김 시 자동 복구
- **한국어 최적화**: 시스템 메시지 및 UI 모두 한국어

## 다음 대화를 위한 중요 정보

### 🔑 기억해야 할 핵심 사항
1. **스트리밍 전용**: 동기 방식은 완전히 제거됨, 추가 금지
2. **LangChain4j High Level API 사용**: `@AiService` 기반
3. **WebSocket 우선**: HTTP보다 WebSocket 기반 솔루션 선호
4. **OpenAI GPT-4o 모델 사용**: 다른 모델 변경 시 주의
5. **한국어 환경**: 모든 메시지, 로그, UI는 한국어

### 📋 주요 파일 위치
- **AI 서비스**: `src/main/java/com/langchain/rag/service/ChatAssistant.java`
- **WebSocket 핸들러**: `src/main/java/com/langchain/rag/websocket/ChatWebSocketHandler.java`
- **설정 파일**: `src/main/resources/application.properties`
- **웹 UI**: `src/main/resources/static/index.html`

### 🚀 향후 확장 가능성
- RAG 기능 추가 (문서 임베딩, 벡터 검색)
- 채팅 히스토리 저장
- 사용자 인증 및 세션 관리
- 다양한 AI 모델 지원
- Function Calling (Tools) 기능

### ⚠️ 주의사항
- API 키는 환경변수로 관리 (`api.key` 파일은 gitignore)
- 스트리밍 외 동기 방식 추가 금지
- 모든 새 기능은 스트리밍 기반으로 구현
- LangChain4j 1.1.0-beta7 버전 유지

---

**마지막 업데이트**: 2024년 12월 (프로젝트 완료 시점)
**상태**: 스트리밍 전용 WebSocket 챗봇 완성 