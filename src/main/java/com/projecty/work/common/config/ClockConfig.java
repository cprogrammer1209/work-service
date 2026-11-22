package com.projecty.work.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One clock for the whole app, so time can be controlled in tests.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
