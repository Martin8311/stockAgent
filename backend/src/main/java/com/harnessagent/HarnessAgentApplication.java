package com.harnessagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HarnessAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HarnessAgentApplication.class, args);
    }
}

