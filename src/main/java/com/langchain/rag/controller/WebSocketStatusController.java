package com.langchain.rag.controller;

import com.langchain.rag.websocket.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ws")
public class WebSocketStatusController {

    private final ChatWebSocketHandler webSocketHandler;

    public WebSocketStatusController(ChatWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * WebSocket 상태 정보 조회
     */
    @GetMapping("/status")
    public Map<String, Object> getWebSocketStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeConnections", webSocketHandler.getActiveSessionCount());
        status.put("status", "running");
        status.put("endpoint", "/ws/chat");
        return status;
    }

    /**
     * 모든 연결된 클라이언트에 브로드캐스트
     */
    @GetMapping("/broadcast/test")
    public Map<String, String> testBroadcast() {
        ChatWebSocketHandler.ChatMessage message = new ChatWebSocketHandler.ChatMessage(
            "system", 
            "🔔 관리자 테스트 브로드캐스트 메시지입니다!", 
            "broadcast"
        );
        
        webSocketHandler.broadcast(message);
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "브로드캐스트 전송 완료");
        result.put("recipients", String.valueOf(webSocketHandler.getActiveSessionCount()));
        return result;
    }
} 