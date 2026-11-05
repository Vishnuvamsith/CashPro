package com.cashpro.payment_service.Configuration;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TraceContextSnapshotTest {
    @Test
    void missingAndMalformedTraceParentsAreIgnored() {
        Tracer tracer = mock(Tracer.class);
        Propagator propagator = mock(Propagator.class);
        TraceContextSnapshot snapshots = new TraceContextSnapshot(tracer, propagator);

        assertThatCode(() -> {
            try (TraceContextSnapshot.Scope ignored = snapshots.continueFrom(null, null)) {
                // No parent context is expected for an uncorrelated outbox record.
            }
            try (TraceContextSnapshot.Scope ignored = snapshots.continueFrom("not-a-traceparent", "bad")) {
                // Invalid context must fall back to a normal root publication span.
            }
        }).doesNotThrowAnyException();

        verifyNoInteractions(tracer, propagator);
    }
}
