package com.venyu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Spec'i tüm uygulamadan üretir ve repoya yazar. CI, commit'lenmiş spec ile farkı kontrol eder.
// Testler backend/ dizininden çalışır: ../docs/api = repo kökündeki docs/api.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiSpecTests {

    private static final Path SPEC_FILE = Path.of("..", "docs", "api", "openapi.json");

    @Autowired
    MockMvc mvc;

    @Test
    void generatesSpecForAllModulesAndWritesItToDocs() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
            .andExpect(jsonPath("$.paths['/me'].get").exists())
            .andExpect(jsonPath("$.paths['/venues/{venueId}/slots/{slotId}/bookings'].post").exists())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andReturn().getResponse().getContentAsString();

        Files.createDirectories(SPEC_FILE.getParent());
        Files.writeString(SPEC_FILE, spec.endsWith("\n") ? spec : spec + "\n");
    }

    @Test
    void swaggerUiIsPubliclyReachable() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
