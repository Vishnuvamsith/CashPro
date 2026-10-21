package com.cashpro.approval_service.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "cashpro.approval")
public class ApprovalProperties {
    private String authorizedApprovers = "";

    public String getAuthorizedApprovers() {
        return authorizedApprovers;
    }

    public void setAuthorizedApprovers(String authorizedApprovers) {
        this.authorizedApprovers = authorizedApprovers == null ? "" : authorizedApprovers;
    }

    public boolean isAuthorized(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Set<String> users = Arrays.stream(authorizedApprovers.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        return users.contains(userId.trim());
    }
}
