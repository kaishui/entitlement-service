package com.kaishui.entitlement.config;

import brave.Tracer;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

@Configuration
public class MongoTracingConfig {

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private Tracer tracer;
    @Bean
    public MongoClientSettingsBuilderCustomizer tracingCustomizer() {
        return builder -> {

            // 添加追踪监听器
            builder.addCommandListener(new MongoObservationCommandListener(observationRegistry));
            // 添加自定义监听器注入traceId
            if (tracer != null) {
                builder.addCommandListener(new TracingMongoCommandListener(tracer));
            }
        };
    }
}

