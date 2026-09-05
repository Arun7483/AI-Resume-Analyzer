package com.resumeanalyzer.repository;
import com.resumeanalyzer.entity.Resume; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List; import java.util.Optional;
public interface ResumeRepository extends JpaRepository<Resume,Long> { Optional<Resume> findByIdAndUserId(Long id,Long userId); Optional<Resume> findTopByUserIdOrderByUploadedAtDesc(Long userId); List<Resume> findAllByUserIdOrderByUploadedAtDesc(Long userId); }
