package com.venyu.notification.internal;

import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.EmailVerificationRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class EmailVerificationListenerTests {

    @Test
    void logsVerificationLinkWhenEventIsPublished(Scenario scenario, CapturedOutput output) {
        var event = new EmailVerificationRequested(UUID.randomUUID(), "ece@example.com", "Ece", "test-token-123");

        scenario.publish(event)
                .andWaitForStateChange(() -> output.getOut().contains("test-token-123"))
                .andVerify(found -> assertThat(output.getOut())
                        .contains("To: ece@example.com")
                        .contains("http://localhost:3000/verify-email?token=test-token-123"));
    }
}