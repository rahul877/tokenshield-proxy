package io.tokenshield.proxy.controller;

import io.tokenshield.proxy.config.ProxyConfig;
import io.tokenshield.proxy.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final RateLimiterService rateLimiterService;
    private final ProxyConfig proxyConfig;
    private final WebClient proxyWebClient;

    @RequestMapping(value = "/{provider}/**", method = {RequestMethod.POST, RequestMethod.GET})
    public Mono<ResponseEntity<Flux<DataBuffer>>> proxyRequest(
            @PathVariable("provider") String provider,
            @RequestHeader(value = "X-TokenShield-Tenant-ID", defaultValue = "default_tenant") String tenantId,
            @RequestHeader(value = "X-TokenShield-Estimated-Tokens", defaultValue = "1000") long estimatedTokens,
            ServerHttpRequest request,
            @RequestBody(required = false) Flux<DataBuffer> body) {

        String baseUrl = proxyConfig.getTargetProviders().get(provider.toLowerCase());
        if (baseUrl == null) {
            log.warn("Unsupported LLM provider requested: {}", provider);
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Flux.empty()));
        }

        // 1. Sliding window rate limit & token budget check (1-minute window, 100k token limit default)
        return rateLimiterService.checkRateLimit(tenantId, 60000L, 100000L, estimatedTokens)
                .flatMap(limitResult -> {
                    if (!limitResult.allowed()) {
                        log.warn("Rate limit or budget exceeded for tenant: {}", tenantId);
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header("X-TokenShield-Remaining-Tokens", String.valueOf(limitResult.remainingTokens()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Flux.empty()));
                    }

                    // 2. Extract provider-relative path
                    String requestPath = request.getPath().pathWithinApplication().value();
                    String pathSegment = "/v1/proxy/" + provider;
                    String remainingPath = requestPath.startsWith(pathSegment)
                            ? requestPath.substring(pathSegment.length())
                            : "";

                    URI targetUri = URI.create(baseUrl + remainingPath + (request.getURI().getQuery() != null ? "?" + request.getURI().getQuery() : ""));

                    // 3. Construct non-blocking reactive WebClient call
                    WebClient.RequestBodySpec requestSpec = proxyWebClient.method(request.getMethod())
                            .uri(targetUri)
                            .headers(headers -> {
                                headers.addAll(request.getHeaders());
                                headers.remove(HttpHeaders.HOST);
                                headers.remove("X-TokenShield-Tenant-ID");
                                headers.remove("X-TokenShield-Estimated-Tokens");
                            });

                    if (body != null) {
                        requestSpec.body(body, DataBuffer.class);
                    }

                    // 4. Forward response directly as a stream of DataBuffers (SSE supported out of the box)
                    return requestSpec.exchangeToMono(clientResponse -> {
                        HttpHeaders responseHeaders = new HttpHeaders();
                        responseHeaders.addAll(clientResponse.headers().asHttpHeaders());
                        responseHeaders.add("X-TokenShield-Remaining-Tokens", String.valueOf(limitResult.remainingTokens()));

                        Flux<DataBuffer> responseStream = clientResponse.bodyToFlux(DataBuffer.class);

                        return Mono.just(ResponseEntity.status(clientResponse.statusCode())
                                .headers(responseHeaders)
                                .body(responseStream));
                    });
                });
    }
}