package com.skybooker.passenger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI passengerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyBooker Passenger Service")
                        .version("1.0")
                        .description("Passenger details, ticket number, validation, seat assignment, and check-in APIs"));
    }
}