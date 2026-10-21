package com.cashpro.payment_service.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "cashpro.approval")
public class ApprovalPolicyProperties {
    private Map<String, BigDecimal> thresholds = new HashMap<>();

    public Map<String, BigDecimal> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, BigDecimal> thresholds) {
        this.thresholds = new HashMap<>();
        if (thresholds != null) {
            thresholds.forEach((currency, threshold) ->
                    this.thresholds.put(currency.toUpperCase(Locale.ROOT), threshold));
        }
    }

    public boolean requiresApproval(BigDecimal amount, String currency) {
        BigDecimal threshold = thresholds.get(currency.toUpperCase(Locale.ROOT));
        return threshold == null || amount.compareTo(threshold) >= 0;
    }
}
