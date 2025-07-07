package com.langchain.rag.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import com.langchain.rag.service.ChatAssistant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ChatMemoryConfig {

    // 세션별 메모리를 관리하는 ConcurrentHashMap
    private final ConcurrentHashMap<String, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    /**
     * ChatAssistant AI 서비스 빈 생성
     * 
     * @param chatModel Spring Boot가 자동으로 생성한 OpenAI ChatModel (동기 처리용)
     * @param streamingChatModel Spring Boot가 자동으로 생성한 OpenAI StreamingChatModel (스트리밍용)
     * @return 메모리 기능이 탑재된 ChatAssistant 인스턴스
     */
    @Bean
    public ChatAssistant chatAssistant(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel) {
        
        // LangChain4j AiServices 빌더를 사용하여 ChatAssistant 인터페이스 구현체 생성
        return AiServices.builder(ChatAssistant.class)
                // 동기 채팅용 모델 설정 (현재 프로젝트에서는 사용하지 않지만 필수)
                .chatModel(chatModel)
                // 스트리밍 채팅용 모델 설정 (실제로 사용되는 모델)
                .streamingChatModel(streamingChatModel)
                // 메모리 제공자 설정: memoryId(세션ID)를 받아서 해당하는 ChatMemory 반환
                // 람다 함수: memoryId -> getOrCreateMemory(memoryId.toString())
                // 세션별로 독립적인 메모리 공간을 제공하여 대화 컨텍스트를 유지
                .chatMemoryProvider(memoryId -> getOrCreateMemory(memoryId.toString()))
                // 최종적으로 프록시 객체 생성 (Spring이 ChatAssistant 인터페이스의 구현체를 동적 생성)
                .build();
    }

    /**
     * 세션 ID에 따라 메모리를 가져오거나 새로 생성
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return memoryStore.computeIfAbsent(sessionId, 
            id -> MessageWindowChatMemory.withMaxMessages(10));
    }

    /**
     * 특정 세션의 메모리 조회
     */
    public ChatMemory getMemory(String sessionId) {
        return memoryStore.get(sessionId);
    }

    /**
     * 특정 세션의 메모리 삭제
     */
    public void clearMemory(String sessionId) {
        ChatMemory memory = memoryStore.get(sessionId);
        if (memory != null) {
            memory.clear();
        }
    }

    /**
     * 모든 메모리 삭제
     */
    public void clearAllMemories() {
        memoryStore.values().forEach(ChatMemory::clear);
        memoryStore.clear();
    }

    /**
     * 현재 활성 메모리 세션 수
     */
    public int getActiveMemoryCount() {
        return memoryStore.size();
    }

    /**
     * 메모리 세션 삭제 (완전 제거)
     */
    public void removeMemorySession(String sessionId) {
        ChatMemory memory = memoryStore.remove(sessionId);
        if (memory != null) {
            memory.clear();
        }
    }
} 