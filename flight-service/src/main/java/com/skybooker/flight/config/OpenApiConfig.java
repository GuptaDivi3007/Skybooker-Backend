package com.skybooker.flight.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flightOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyBooker Flight Service")
                        .version("1.0")
                        .description("Flight schedule, search, status, and seat counter APIs"));
    }
}