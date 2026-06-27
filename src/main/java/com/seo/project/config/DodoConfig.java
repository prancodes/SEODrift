package com.seo.project.config;

import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.client.okhttp.DodoPaymentsOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DodoConfig {

    @Value("${dodo.payments.api.key:}")
    private String apiKey;

    @Value("${dodo.payments.environment}")
    private String environment;

    @Bean
    public DodoPaymentsClient dodoPaymentsClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Dodo Payments API key is not configured. Payments will not work.");
            return null;
        }
        return DodoPaymentsOkHttpClient.builder()
                .bearerToken(apiKey)
                .baseUrl(environment.equalsIgnoreCase("live") ? "https://live.dodopayments.com" : "https://test.dodopayments.com")
                .build();
    }
}
