package com.skybooker.seat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI seatOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyBooker Seat Service")
                        .version("1.0")
                        .description("Seat map, seat inventory, hold, release, confirm, and availability APIs"));
    }
}