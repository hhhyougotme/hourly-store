package com.hourlystore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.hourlystore.mapper")
@EnableScheduling
public class HourlyStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(HourlyStoreApplication.class, args);
    }
}
