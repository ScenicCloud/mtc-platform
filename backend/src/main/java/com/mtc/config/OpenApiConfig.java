package com.mtc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mtcOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MTC API")
                        .description("Modern Testing Capabilities Backend API")
                        .version("0.1.0"));
    }
}
