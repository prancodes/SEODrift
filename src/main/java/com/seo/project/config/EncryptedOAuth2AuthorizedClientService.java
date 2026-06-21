package com.seo.project.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.util.Collections;

/**
 * A decorator for {@link OAuth2AuthorizedClientService} that encrypts and decrypts
 * access and refresh tokens at the application layer before persisting them.
 * This provides defense-in-depth protection for sensitive OAuth2 credentials.
 */
public class EncryptedOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final OAuth2AuthorizedClientService delegate;
    private final TextEncryptor encryptor;

    public EncryptedOAuth2AuthorizedClientService(OAuth2AuthorizedClientService delegate, TextEncryptor encryptor) {
        this.delegate = delegate;
        this.encryptor = encryptor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        OAuth2AuthorizedClient client = this.delegate.loadAuthorizedClient(clientRegistrationId, principalName);
        if (client == null) {
            return null;
        }

        OAuth2AccessToken decryptedAccessToken = decryptAccessToken(client.getAccessToken());
        OAuth2RefreshToken decryptedRefreshToken = decryptRefreshToken(client.getRefreshToken());

        return (T) new OAuth2AuthorizedClient(
                client.getClientRegistration(),
                client.getPrincipalName(),
                decryptedAccessToken,
                decryptedRefreshToken
        );
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        OAuth2AccessToken encryptedAccessToken = encryptAccessToken(authorizedClient.getAccessToken());
        OAuth2RefreshToken incomingRefreshToken = authorizedClient.getRefreshToken();
        OAuth2RefreshToken encryptedRefreshToken;

        if (incomingRefreshToken == null) {
            // Retrieve the existing raw client directly from the delegate to reuse the already encrypted refresh token
            OAuth2AuthorizedClient existingRaw = this.delegate.loadAuthorizedClient(
                    authorizedClient.getClientRegistration().getRegistrationId(),
                    authorizedClient.getPrincipalName()
            );
            if (existingRaw != null && existingRaw.getRefreshToken() != null) {
                encryptedRefreshToken = existingRaw.getRefreshToken();
            } else {
                encryptedRefreshToken = null;
            }
        } else {
            encryptedRefreshToken = encryptRefreshToken(incomingRefreshToken);
        }

        OAuth2AuthorizedClient encryptedClient = new OAuth2AuthorizedClient(
                authorizedClient.getClientRegistration(),
                authorizedClient.getPrincipalName(),
                encryptedAccessToken,
                encryptedRefreshToken
        );

        this.delegate.saveAuthorizedClient(encryptedClient, principal);
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        this.delegate.removeAuthorizedClient(clientRegistrationId, principalName);
    }

    private OAuth2AccessToken encryptAccessToken(OAuth2AccessToken token) {
        if (token == null) {
            return null;
        }
        String encryptedValue = this.encryptor.encrypt(token.getTokenValue());
        return new OAuth2AccessToken(
                token.getTokenType(),
                encryptedValue,
                token.getIssuedAt(),
                token.getExpiresAt(),
                token.getScopes() != null ? token.getScopes() : Collections.emptySet()
        );
    }

    private OAuth2AccessToken decryptAccessToken(OAuth2AccessToken token) {
        if (token == null) {
            return null;
        }
        String decryptedValue = this.encryptor.decrypt(token.getTokenValue());
        return new OAuth2AccessToken(
                token.getTokenType(),
                decryptedValue,
                token.getIssuedAt(),
                token.getExpiresAt(),
                token.getScopes() != null ? token.getScopes() : Collections.emptySet()
        );
    }

    private OAuth2RefreshToken encryptRefreshToken(OAuth2RefreshToken token) {
        if (token == null) {
            return null;
        }
        String encryptedValue = this.encryptor.encrypt(token.getTokenValue());
        return new OAuth2RefreshToken(
                encryptedValue,
                token.getIssuedAt(),
                token.getExpiresAt()
        );
    }

    private OAuth2RefreshToken decryptRefreshToken(OAuth2RefreshToken token) {
        if (token == null) {
            return null;
        }
        String decryptedValue = this.encryptor.decrypt(token.getTokenValue());
        return new OAuth2RefreshToken(
                decryptedValue,
                token.getIssuedAt(),
                token.getExpiresAt()
        );
    }
}
