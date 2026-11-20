package com.venyu.notification.internal;

import com.venyu.TestcontainersConfiguration;
import com.venyu.identity.PhoneVerificationRequested;
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
class PhoneVerificationListenerTests {

    @Test
    void logsCodeWhenEventIsPublished(Scenario scenario, CapturedOutput output) {
        var event = new PhoneVerificationRequested(UUID.randomUUID(), "+905551234567", "482913");

        scenario.publish(event)
                .andWaitForStateChange(() -> output.getOut().contains("482913"))
                .andVerify(found -> assertThat(output.getOut())
                        .contains("[DEV SMS] To: +905551234567"));
    }
}