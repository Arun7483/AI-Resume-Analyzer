package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.ChatRequestDto;
import com.resumeanalyzer.dto.ChatResponseDto;
import com.resumeanalyzer.entity.ChatMessage;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.BadRequestException;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.repository.ChatMessageRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messages;
    private final ResumeRepository resumes;
    private final CurrentUser currentUser;
    private final ChatClient.Builder chatClientBuilder;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Transactional
    public ChatResponseDto chat(ChatRequestDto request) {

        // Get currently logged-in user
        User currentUserEntity = currentUser.require();

        // Find selected resume
        Resume selectedResume = null;

        if (request.resumeId() != null) {
            selectedResume = resumes
                    .findByIdAndUserId(
                            request.resumeId(),
                            currentUserEntity.getId()
                    )
                    .orElseThrow(
                            () -> new ResourceNotFoundException(
                                    "Resume not found"
                            )
                    );
        }

        // Validate user message
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new BadRequestException(
                    "Message cannot be empty."
            );
        }

        // Save user's message
        messages.save(
                ChatMessage.builder()
                        .user(currentUserEntity)
                        .resume(selectedResume)
                        .sender(ChatMessage.Sender.USER)
                        .content(request.prompt())
                        .build()
        );

        // Check Groq API key
        if (apiKey == null
                || apiKey.isBlank()
                || apiKey.equals("dummy-key")) {

            throw new BadRequestException(
                    "Groq AI is not configured. " +
                    "Set spring.ai.openai.api-key to your Groq API key."
            );
        }

        /*
         * These are final so they can safely be used
         * inside the lambda below.
         */
        final String resumeContext =
                selectedResume == null
                        ? "No resume was selected."
                        : selectedResume.getRawText();

        final String userQuestion = request.prompt();

        String answer;

        try {

            ChatClient chatClient = chatClientBuilder.build();

            answer = chatClient
                    .prompt()
                    .system("""
                            You are a concise and practical resume coach.

                            Rules:
                            1. Use the provided resume as the candidate context.
                            2. Never invent qualifications, skills, education,
                               experience, projects, or achievements.
                            3. If the resume does not contain the requested
                               information, clearly say that it is not available
                               in the resume.
                            4. Give useful and direct answers.
                            5. Use simple formatting when appropriate.
                            """)
                    .user(user -> user
                            .text("""
                                    RESUME CONTEXT:
                                    {resume}

                                    USER QUESTION:
                                    {question}
                                    """)
                            .param("resume", resumeContext)
                            .param("question", userQuestion)
                    )
                    .call()
                    .content();

        } catch (RuntimeException ex) {

            throw new BadRequestException(
                    "Groq AI request failed. " +
                    "Check the Groq API key, model configuration, " +
                    "and backend internet connection."
            );
        }

        // Validate AI response
        if (answer == null || answer.isBlank()) {
            throw new BadRequestException(
                    "Groq AI returned an empty response. " +
                    "Check the configured model."
            );
        }

        // Save AI response
        ChatMessage savedMessage = messages.saveAndFlush(
                ChatMessage.builder()
                        .user(currentUserEntity)
                        .resume(selectedResume)
                        .sender(ChatMessage.Sender.BOT)
                        .content(answer)
                        .build()
        );

        return new ChatResponseDto(
                savedMessage.getId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp()
        );
    }
}