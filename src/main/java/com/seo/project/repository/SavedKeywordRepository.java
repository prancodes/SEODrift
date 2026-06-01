package com.seo.project.repository;

import com.seo.project.model.SavedKeyword;
import com.seo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavedKeywordRepository extends JpaRepository<SavedKeyword, Long> {
    List<SavedKeyword> findByUserOrderBySavedAtDesc(User user);
    Optional<SavedKeyword> findByUserAndKeyword(User user, String keyword);
}
