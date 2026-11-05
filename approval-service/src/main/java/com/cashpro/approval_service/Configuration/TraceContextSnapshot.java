package com.cashpro.approval_service.Configuration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Link;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TraceContextSnapshot {
    private static final Pattern TRACEPARENT = Pattern.compile(
            "(?i)^00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$");

    private final Tracer tracer;
    private final Propagator propagator;

    public TraceContextSnapshot(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Context capture() {
        Span current = tracer.currentSpan();
        if (current == null) {
            return new Context(null, null);
        }
        Map<String, String> values = new HashMap<>();
        propagator.inject(current.context(), values, Map::put);
        return new Context(values.get("traceparent"), values.get("tracestate"));
    }

    public Scope continueFrom(String traceparent, String tracestate) {
        return continueFrom(traceparent, tracestate, "cashpro.outbox.publish");
    }

    public Scope continueFrom(String traceparent, String tracestate, String spanName) {
        if (!isValidTraceparent(traceparent)) {
            return Scope.noop();
        }

        try {
            Map<String, String> values = new HashMap<>();
            values.put("traceparent", traceparent);
            if (tracestate != null && !tracestate.isBlank()) {
                values.put("tracestate", tracestate);
            }
            Span.Builder parent = propagator.extract(values, Map::get);
            if (parent == null) {
                return Scope.noop();
            }
            Span current = tracer.currentSpan();
            if (current != null) {
                parent.addLink(new Link(current.context()));
            }
            Span span = parent.name(spanName).start();
            return new Scope(span, tracer.withSpan(span));
        } catch (RuntimeException ignored) {
            // Corrupt persisted context must never prevent an outbox event from being published.
            return Scope.noop();
        }
    }

    static boolean isValidTraceparent(String traceparent) {
        if (traceparent == null) {
            return false;
        }
        Matcher matcher = TRACEPARENT.matcher(traceparent);
        if (!matcher.matches()) {
            return false;
        }
        return !matcher.group(1).matches("(?i)0{32}")
                && !matcher.group(2).matches("(?i)0{16}");
    }

    public record Context(String traceparent, String tracestate) {
    }

    public static final class Scope implements AutoCloseable {
        private final Span span;
        private final Tracer.SpanInScope spanInScope;

        private Scope(Span span, Tracer.SpanInScope spanInScope) {
            this.span = span;
            this.spanInScope = spanInScope;
        }

        private static Scope noop() {
            return new Scope(null, null);
        }

        @Override
        public void close() {
            if (spanInScope != null) {
                spanInScope.close();
            }
            if (span != null) {
                span.end();
            }
        }
    }
}
