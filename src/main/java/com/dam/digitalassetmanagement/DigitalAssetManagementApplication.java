package com.dam.digitalassetmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DigitalAssetManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalAssetManagementApplication.class, args);
	}
}