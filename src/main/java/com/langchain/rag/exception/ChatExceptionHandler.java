package com.langchain.rag.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ChatExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ChatExceptionHandler.class);
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleChatError(RuntimeException e) {
        log.error("채팅 처리 중 오류가 발생했습니다", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "CHAT_ERROR",
            "채팅 처리 중 오류가 발생했습니다: " + e.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "BAD_REQUEST",
            e.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * 에러 응답 DTO
     */
    public static class ErrorResponse {
        private String code;
        private String message;
        
        public ErrorResponse() {}
        
        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
} 