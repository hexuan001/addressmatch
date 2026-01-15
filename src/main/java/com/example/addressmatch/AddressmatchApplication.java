package com.example.addressmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AddressmatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(AddressmatchApplication.class, args);
    }
}