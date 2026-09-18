package com.escrowflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EscrowFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(EscrowFlowApplication.class, args);
	}

}
