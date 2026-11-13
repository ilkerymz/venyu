package com.venyu;

import org.springframework.boot.SpringApplication;

public class TestVenyuApplication {

	public static void main(String[] args) {
		SpringApplication.from(VenyuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
