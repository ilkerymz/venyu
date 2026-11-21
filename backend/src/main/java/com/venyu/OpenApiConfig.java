package com.venyu;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Uygulama geneli API dokümantasyonu: tüm modüllerin endpoint'leri tek spec'te.
@Configuration
class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI venyuOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Venyu API")
                .version("v1")
                .description("Mekan, organizatör ve katılımcıları buluşturan etkinlik platformu."))
            // Sabit ve göreli: üretilen spec, isteğin geldiği host'a göre değişmesin.
            .servers(List.of(new Server().url("/")))
            .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
