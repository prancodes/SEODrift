package com.seo.project.service;

import com.seo.project.model.User;
import com.seo.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User processAndSyncUser(String email, String name, String googleId, String pictureUrl) {
        log.info("Identity synchronization for user: [{}]", email);

        // Priority 1: Match by Google ID (Most robust)
        Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);

        if (userByGoogleId.isPresent()) {
            User existingUser = userByGoogleId.get();
            log.debug("Found existing user by Google ID. Refreshing profile attributes.");
            existingUser.setName(name);
            existingUser.setEmail(email);
            existingUser.setPictureUrl(pictureUrl);
            return userRepository.save(existingUser);
        } else {
            // Priority 2: Match by Email (For users who might have existed before OAuth)
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User existingUser = userByEmail.get();
                log.debug("Linking Google ID to existing account: {}", email);
                existingUser.setGoogleId(googleId);
                existingUser.setName(name);
                existingUser.setPictureUrl(pictureUrl);
                return userRepository.save(existingUser);
            } else {
                // Priority 3: Create new user
                log.info("No existing account found. Provisioning new user record for: {}", email);
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .googleId(googleId)
                        .pictureUrl(pictureUrl)
                        .build();
                return userRepository.save(newUser);
            }
        }
    }
}
