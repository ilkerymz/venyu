package com.venyu.notification.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "venyu.notification")
record NotificationProperties(@NotBlank String frontendUrl) {
}