package com.resumeanalyzer.repository;
import com.resumeanalyzer.entity.ChatMessage; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> { List<ChatMessage> findTop50ByUserIdAndResumeIdOrderByTimestampDesc(Long userId,Long resumeId); }
