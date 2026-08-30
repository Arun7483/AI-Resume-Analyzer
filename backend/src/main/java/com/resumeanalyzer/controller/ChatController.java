package com.resumeanalyzer.controller;
import com.resumeanalyzer.dto.*; import com.resumeanalyzer.service.ChatService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/chat") @RequiredArgsConstructor public class ChatController { private final ChatService chat; @PostMapping ChatResponseDto chat(@Valid @RequestBody ChatRequestDto request){return chat.chat(request);} }
