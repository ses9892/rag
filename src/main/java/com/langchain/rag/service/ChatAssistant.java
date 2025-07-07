package com.langchain.rag.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface ChatAssistant {
    
    @SystemMessage("당신은 친절하고 도움이 되는 한국어 AI 어시스턴트입니다. 사용자의 질문에 정중하고 유용한 답변을 제공해주세요. 이전 대화 내용을 기억하고 맥락에 맞게 대답해주세요.")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
    
    // 스트리밍 응답 (실시간 채팅용)
    @SystemMessage("당신은 친절하고 도움이 되는 한국어 AI 어시스턴트입니다. 사용자의 질문에 정중하고 유용한 답변을 제공해주세요. 이전 대화 내용을 기억하고 맥락에 맞게 대답해주세요.")
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String userMessage);
} 