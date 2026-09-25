package io.tokenshield.proxy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tokenshield.proxy")
public class ProxyConfig {

    private Map<String, String> targetProviders;

    @Bean
    public WebClient proxyWebClient() {
        // High-performance WebClient with unconstrained memory buffers for streaming payloads
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();
    }
}