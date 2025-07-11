package com.facility.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FacilityManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacilityManagementApplication.class, args);
    }
}
