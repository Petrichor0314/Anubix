package com.petrichor.feature_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class FeatureServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeatureServiceApplication.class, args);
	}

}
