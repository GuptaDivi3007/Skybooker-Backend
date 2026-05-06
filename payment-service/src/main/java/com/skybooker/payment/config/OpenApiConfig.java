package com.skybooker.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyBooker Payment Service")
                        .version("1.0")
                        .description("Payment creation, processing, status tracking, and booking confirmation APIs"));
    }
}