package com.icop.operator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class K8sOperatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sOperatorApplication.class, args);
    }
}
