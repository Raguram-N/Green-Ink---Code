package com.greenink.api;

import com.greenink.api.config.GreenInkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GreenInkProperties.class)
public class GreenInkApplication {
    public static void main(String[] args) {
        SpringApplication.run(GreenInkApplication.class, args);
    }
}
