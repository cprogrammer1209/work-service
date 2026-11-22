package com.projecty.work.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} (used by the outbox relay).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
