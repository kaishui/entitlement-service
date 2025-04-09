package com.kaishui.entitlement.config;

import com.kaishui.entitlement.util.AuthorizationUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import reactor.core.publisher.Mono;

@Configuration
@EnableReactiveMongoAuditing // Enable auditing
public class MongoConfig { // Rename if you have an existing config class

    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        // Use deferContextual to get the username from the request context
        return () -> Mono.deferContextual(contextView ->
                Mono.just(AuthorizationUtil.extractUsernameFromContext(contextView))
                        .switchIfEmpty(Mono.just(AuthorizationUtil.UNKNOWN_USER)) // Provide default if empty
        );
    }
}