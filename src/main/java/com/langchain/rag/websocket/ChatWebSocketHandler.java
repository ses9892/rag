package com.langchain.rag.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langchain.rag.service.ChatAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ChatAssistant chatAssistant;
    private final ObjectMapper objectMapper;
    
    // 활성 WebSocket 세션 관리
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket 연결 성공: {}", sessionId);
        
        // 연결 확인 메시지 전송
        sendMessage(session, new ChatMessage("system", "연결되었습니다. 메시지를 보내보세요!", "connected"));
    }

        @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        log.info("WebSocket 메시지 수신 [{}]: {}", sessionId, payload);
        
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String userMessage = jsonNode.get("message").asText();
            String memoryId = jsonNode.has("sessionId") ? 
                jsonNode.get("sessionId").asText() : session.getId();
            
            // 모든 메시지를 스트리밍으로 처리 (논리적 세션 ID 사용)
            handleChatMessage(session, userMessage, memoryId);
            
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
            sendMessage(session, new ChatMessage("error", "메시지 처리 중 오류가 발생했습니다: " + e.getMessage(), "error"));
        }
    }

 

    private void handleChatMessage(WebSocketSession session, String userMessage, String memoryId) {
        try {
            // 스트리밍 시작 알림
            sendMessage(session, new ChatMessage("system", "스트리밍을 시작합니다...", "stream_start"));
            
            // 프론트엔드에서 전송한 논리적 세션 ID를 메모리 ID로 사용
            Flux<String> streamResponse = chatAssistant.chatStream(memoryId, userMessage);
            
            streamResponse
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(token -> {
                    try {
                        sendMessage(session, new ChatMessage("ai", token, "stream_token"));
                    } catch (Exception e) {
                        log.error("스트리밍 토큰 전송 중 오류", e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        sendMessage(session, new ChatMessage("system", "스트리밍이 완료되었습니다.", "stream_end"));
                    } catch (Exception e) {
                        log.error("스트리밍 완료 메시지 전송 중 오류", e);
                    }
                })
                .doOnError(error -> {
                    try {
                        log.error("스트리밍 중 오류", error);
                        sendMessage(session, new ChatMessage("error", "스트리밍 중 오류가 발생했습니다.", "error"));
                    } catch (Exception e) {
                        log.error("에러 메시지 전송 중 오류", e);
                    }
                })
                .subscribe();
                
        } catch (Exception e) {
            log.error("스트리밍 메시지 처리 중 오류", e);
            sendMessage(session, new ChatMessage("error", "스트리밍 처리 중 오류가 발생했습니다.", "error"));
        }
    }

    private void sendMessage(WebSocketSession session, ChatMessage chatMessage) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(chatMessage);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("WebSocket 메시지 전송 실패", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류 [{}]", session.getId(), exception);
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket 연결 종료: {} (상태: {})", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 모든 활성 세션에 브로드캐스트
     */
    public void broadcast(ChatMessage message) {
        sessions.values().forEach(session -> sendMessage(session, message));
    }

    /**
     * 현재 활성 세션 수 반환
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 채팅 메시지 DTO
     */
    public static class ChatMessage {
        private String sender;
        private String content;
        private String type;
        private long timestamp;

        public ChatMessage() {
            this.timestamp = System.currentTimeMillis();
        }

        public ChatMessage(String sender, String content, String type) {
            this();
            this.sender = sender;
            this.content = content;
            this.type = type;
        }

        // Getters and Setters
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
} 