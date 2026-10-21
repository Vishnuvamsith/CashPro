package com.cashpro.approval_service.DTO;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(@Size(max = 500) String reason) {
}
