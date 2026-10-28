package com.cashpro.approval_service.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ApprovalProperties {

    private final Set<String> authorizedUsers;

    public ApprovalProperties(
            @Value("${cashpro.approval.authorized-approvers:}") String authorizedApprovers) {
        this.authorizedUsers = Arrays.stream(authorizedApprovers.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        if (this.authorizedUsers.isEmpty()) {
            // log.warn("No authorized approvers configured — all approvals will be denied");
        }
    }

    public Set<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public boolean isAuthorized(String userId) {
        return userId != null
                && !userId.isBlank()
                && authorizedUsers.contains(userId.trim());
    }
}