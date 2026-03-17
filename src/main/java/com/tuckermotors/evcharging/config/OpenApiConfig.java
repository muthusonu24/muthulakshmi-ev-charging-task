package com.tuckermotors.evcharging.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evChargingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EV Charging Station Management System API")
                        .description("REST API for managing EV charging stations, OCPP messages, and transactions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tucker Motors Private Limited")
                                .email("hr@tuckermotors.com")));
    }
}
