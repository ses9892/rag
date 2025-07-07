package com.langchain.rag.controller;

import com.langchain.rag.service.ChatAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final ChatAssistant chatAssistant;
    
    public ChatController(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }
    
    /**
     * 일반 채팅 API (동기 처리)
     */
    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        return chatAssistant.chat(request.getMessage());
    }
    
    /**
     * 스트리밍 채팅 API (실시간 응답)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatAssistant.chatStream(request.getMessage())
                .map(token -> "data: " + token + "\n\n");
    }
    
    /**
     * 간단한 테스트용 GET 엔드포인트
     */
    @GetMapping("/test")
    public String test(@RequestParam(value = "message", defaultValue = "안녕하세요!") String message) {
        return chatAssistant.chat(message);
    }
    
    /**
     * 스트리밍 테스트용 GET 엔드포인트
     */
    @GetMapping(value = "/test-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStream(@RequestParam(value = "message", defaultValue = "긴 이야기를 들려주세요") String message) {
        return chatAssistant.chatStream(message)
                .map(token -> "data: " + token + "\n\n");
    }
    
    /**
     * 채팅 요청 DTO
     */
    public static class ChatRequest {
        private String message;
        
        public ChatRequest() {}
        
        public ChatRequest(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
} 