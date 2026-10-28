package com.cashpro.approval_service.Service;

import com.cashpro.approval_service.Configuration.ApprovalProperties;
import com.cashpro.approval_service.Entity.ApprovalDecision;
import com.cashpro.approval_service.Entity.ApprovalDecisionType;
import com.cashpro.approval_service.Entity.ApprovalRequest;
import com.cashpro.approval_service.Entity.ApprovalStatus;
import com.cashpro.approval_service.Repository.ApprovalDecisionRepository;
import com.cashpro.approval_service.Repository.ApprovalOutboxRepository;
import com.cashpro.approval_service.Repository.ApprovalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {
    private ApprovalRequestRepository requests;
    private ApprovalDecisionRepository decisions;
    private ApprovalOutboxRepository outbox;
    private ApprovalProperties properties;
    private ApprovalService service;

    @BeforeEach
    void setUp() {
        requests = mock(ApprovalRequestRepository.class);
        decisions = mock(ApprovalDecisionRepository.class);
        outbox = mock(ApprovalOutboxRepository.class);
        // Constructor takes the raw comma-separated config string
        properties = new ApprovalProperties("checker-1, checker-2");
        service = new ApprovalService(requests, decisions, outbox, properties);
    }

    @Test
    void approvalRequiresAuthorizedDifferentCheckerAndPersistsDecisionAndOutbox() {
        UUID paymentId = UUID.randomUUID();
        ApprovalRequest request = pendingRequest(paymentId);
        when(requests.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.of(request));
        when(decisions.save(any(ApprovalDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.approve(paymentId, "checker-1", "Reviewed");

        assertThat(result.decision()).isEqualTo(ApprovalDecisionType.APPROVED);
        assertThat(result.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(result.makerId()).isEqualTo("maker-1");
        assertThat(result.approverId()).isEqualTo("checker-1");
        assertThat(result.reason()).isEqualTo("Reviewed");
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        verify(outbox).save(any());
    }

    @Test
    void unauthorizedAndMakerCannotDecide() {
        UUID paymentId = UUID.randomUUID();
        when(requests.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.of(pendingRequest(paymentId)));

        assertThatThrownBy(() -> service.approve(paymentId, "stranger", null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.approve(paymentId, "maker-1", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void secondDecisionIsRejected() {
        UUID paymentId = UUID.randomUUID();
        ApprovalRequest request = pendingRequest(paymentId);
        request.setStatus(ApprovalStatus.APPROVED);
        when(requests.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.reject(paymentId, "checker-2", "No"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ----- new tests enabled by the immutable-Set design -----

    @Test
    void trimsWhitespaceAroundConfiguredUsers() {
        // "checker-2" has a leading space in the raw config; must still match
        assertThat(properties.isAuthorized("checker-2")).isTrue();
        assertThat(properties.isAuthorized("  checker-1  ")).isTrue();
    }

    @Test
    void emptyConfigurationDeniesEveryone() {
        ApprovalProperties empty = new ApprovalProperties("");
        assertThat(empty.isAuthorized("checker-1")).isFalse();
        assertThat(empty.isAuthorized("")).isFalse();
        assertThat(empty.isAuthorized(null)).isFalse();

        UUID paymentId = UUID.randomUUID();
        when(requests.findByPaymentIdForUpdate(paymentId))
                .thenReturn(Optional.of(pendingRequest(paymentId)));
        ApprovalService lockedDown = new ApprovalService(requests, decisions, outbox, empty);

        assertThatThrownBy(() -> lockedDown.approve(paymentId, "checker-1", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unauthorizedDecisionIsForbidden() {
        UUID paymentId = UUID.randomUUID();
        when(requests.findByPaymentIdForUpdate(paymentId))
                .thenReturn(Optional.of(pendingRequest(paymentId)));

        assertThatThrownBy(() -> service.approve(paymentId, "stranger", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ApprovalRequest pendingRequest(UUID paymentId) {
        Instant now = Instant.now();
        return ApprovalRequest.builder()
                .paymentId(paymentId)
                .makerId("maker-1")
                .clientId("client-1")
                .amount(new BigDecimal("1500.00"))
                .currency("USD")
                .status(ApprovalStatus.PENDING_APPROVAL)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}