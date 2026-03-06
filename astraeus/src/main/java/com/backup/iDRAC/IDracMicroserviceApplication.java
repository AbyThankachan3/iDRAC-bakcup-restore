package com.backup.iDRAC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class IDracMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IDracMicroserviceApplication.class, args);
	}

}
