package com.payflow;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayflowApplication {

	public static void main(String[] args) {
		// Windows/VN JVM default is Asia/Saigon; Postgres 16 expects Asia/Ho_Chi_Minh or UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(PayflowApplication.class, args);
	}

}
