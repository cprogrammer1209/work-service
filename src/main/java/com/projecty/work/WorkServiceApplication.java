package com.projecty.work;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkServiceApplication.class, args);
	}

}
