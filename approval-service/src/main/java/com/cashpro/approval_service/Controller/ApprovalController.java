package com.cashpro.approval_service.Controller;

import com.cashpro.approval_service.DTO.ApprovalDecisionRequest;
import com.cashpro.approval_service.DTO.ApprovalDecisionResponse;
import com.cashpro.approval_service.DTO.ApprovalRequestResponse;
import com.cashpro.approval_service.Service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;

    @GetMapping("/{paymentId}")
    public ApprovalRequestResponse getRequest(@PathVariable UUID paymentId) {
        return service.getRequest(paymentId);
    }

    @GetMapping("/{paymentId}/decision")
    public ApprovalDecisionResponse getDecision(@PathVariable UUID paymentId) {
        return service.getDecision(paymentId);
    }

    @PostMapping("/{paymentId}/approve")
    public ApprovalDecisionResponse approve(@PathVariable UUID paymentId,
                                            @RequestHeader("X-User-Id") String checkerId,
                                            @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return service.approve(paymentId, checkerId, request == null ? null : request.reason());
    }

    @PostMapping("/{paymentId}/reject")
    public ApprovalDecisionResponse reject(@PathVariable UUID paymentId,
                                           @RequestHeader("X-User-Id") String checkerId,
                                           @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return service.reject(paymentId, checkerId, request == null ? null : request.reason());
    }
}
