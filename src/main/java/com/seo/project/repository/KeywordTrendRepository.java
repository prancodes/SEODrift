package com.seo.project.repository;

import com.seo.project.model.KeywordTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KeywordTrendRepository extends JpaRepository<KeywordTrend, Long> {
    Optional<KeywordTrend> findByKeywordAndRecordedDate(String keyword, LocalDate recordedDate);
    List<KeywordTrend> findByKeywordOrderByRecordedDateDesc(String keyword);
}
