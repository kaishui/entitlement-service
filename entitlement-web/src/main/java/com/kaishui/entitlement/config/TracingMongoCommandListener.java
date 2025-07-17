package com.kaishui.entitlement.config;

import brave.Span;
import brave.Tracer;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import org.bson.BsonString;

public class TracingMongoCommandListener implements CommandListener {
    private final Tracer tracer;

    public TracingMongoCommandListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
//        if (tracer.currentSpan() != null) {
//            event.getCommand().put("traceId",
//                    new BsonString(tracer.currentSpan().context().traceIdString()));
//        }

        Span span = tracer.nextSpan(); // 从pendingSpans获取或新建
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            event.getCommand().put("traceId",
                    new BsonString(span.context().traceIdString()));
        } finally {
            span.finish(); // 确保结束Span
        }
    }
}

