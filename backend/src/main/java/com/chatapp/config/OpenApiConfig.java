package com.chatapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatAppOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChatApp API")
                        .version("1.0.0")
                        .description("REST API for authentication, direct/group chat, message history, sync, receipts and administration. WebSocket/STOMP is documented separately in docs/WEBSOCKET.md"))
                .servers(List.of(new Server().url("/").description("Current server")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register and login"),
                        new Tag().name("Users").description("Profile and user search"),
                        new Tag().name("Conversations").description("Direct and group conversations"),
                        new Tag().name("Messages").description("Message history and REST fallback"),
                        new Tag().name("Administration").description("ADMIN-only metadata, moderation and audit APIs")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
