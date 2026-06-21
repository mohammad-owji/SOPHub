package com.sophub;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class SophubApplication {

    public static void main(String[] args) {
        SpringApplication.run(SophubApplication.class, args);
    }

    @Bean
    public ApplicationRunner openSwagger() {
        return args -> Runtime.getRuntime().exec(
                new String[]{"cmd", "/c", "start", "http://localhost:8080/swagger-ui/index.html"}
        );
    }
}