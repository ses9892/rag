package com.langchain.rag.controller;

import com.langchain.rag.config.ChatMemoryConfig;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = "*")
public class MemoryController {

    private final ChatMemoryConfig chatMemoryConfig;

    public MemoryController(ChatMemoryConfig chatMemoryConfig) {
        this.chatMemoryConfig = chatMemoryConfig;
    }

    /**
     * 특정 세션의 대화 히스토리 조회
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getMemory(@PathVariable String sessionId) {
        ChatMemory memory = chatMemoryConfig.getMemory(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (memory != null) {
            List<ChatMessage> messages = memory.messages();
            // ChatMessage를 간단한 DTO로 변환
            List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("type", msg.type().toString());
                    
                    // ChatMessage의 텍스트 내용 추출
                    if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                        msgMap.put("text", ((dev.langchain4j.data.message.UserMessage) msg).singleText());
                    } else if (msg instanceof dev.langchain4j.data.message.AiMessage) {
                        msgMap.put("text", ((dev.langchain4j.data.message.AiMessage) msg).text());
                    } else if (msg instanceof dev.langchain4j.data.message.SystemMessage) {
                        msgMap.put("text", ((dev.langchain4j.data.message.SystemMessage) msg).text());
                    } else {
                        msgMap.put("text", msg.toString());
                    }
                    
                    return msgMap;
                })
                .toList();
            
            response.put("sessionId", sessionId);
            response.put("messageCount", messages.size());
            response.put("messages", messageList);
            response.put("exists", true);
        } else {
            response.put("sessionId", sessionId);
            response.put("messageCount", 0);
            response.put("messages", List.of());
            response.put("exists", false);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 세션의 메모리 초기화
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearMemory(@PathVariable String sessionId) {
        chatMemoryConfig.clearMemory(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "세션 " + sessionId + "의 메모리가 초기화되었습니다.");
        response.put("sessionId", sessionId);
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 메모리 초기화
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> clearAllMemories() {
        int count = chatMemoryConfig.getActiveMemoryCount();
        chatMemoryConfig.clearAllMemories();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", count + "개의 메모리 세션이 모두 초기화되었습니다.");
        response.put("clearedCount", count);
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 메모리 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMemoryStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeMemoryCount", chatMemoryConfig.getActiveMemoryCount());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 세션 메모리 완전 삭제 (세션 종료용)
     */
    @DeleteMapping("/{sessionId}/remove")
    public ResponseEntity<Map<String, Object>> removeMemorySession(@PathVariable String sessionId) {
        chatMemoryConfig.removeMemorySession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "세션 " + sessionId + "이 완전히 삭제되었습니다.");
        response.put("sessionId", sessionId);
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }
} 