package com.softwarecampus.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 * 
 * @since 2025-11-26
 * @author Software Campus Backend Team
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 스키마 설정
        String jwtSchemeName = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)")
                );

        return new OpenAPI()
                .info(new Info()
                        .title("Software Campus API")
                        .description("소프트웨어 캠퍼스 백엔드 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Software Campus Team")
                                .email("contact@softwarecampus.com")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("로컬 개발 서버")
                ))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
