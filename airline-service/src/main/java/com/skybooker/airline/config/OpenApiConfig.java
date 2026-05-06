package com.skybooker.airline.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI airlineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyBooker Airline & Airport Service")
                        .version("1.0")
                        .description("Airline and airport master data APIs"));
    }
}