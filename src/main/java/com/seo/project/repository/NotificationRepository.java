package com.seo.project.repository;

import com.seo.project.model.Notification;
import com.seo.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean isRead);
    long countByUserAndIsRead(User user, boolean isRead);
}
