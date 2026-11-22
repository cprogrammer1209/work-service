package com.projecty.work.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App-wide settings under {@code projecty.*}. Module settings live in each module's own properties class.
 * {@code token} describes the access tokens Platform issues; Work only checks them.
 */
@ConfigurationProperties("projecty")
public record AppProperties(Cors cors, Token token) {

	public record Cors(List<String> allowedOrigins) {
	}

	/**
	 * @param issuer the {@code iss} Platform puts in every token
	 * @param audience the {@code aud} of customer tokens
	 * @param jwkSetUri where Platform publishes its public keys
	 */
	public record Token(String issuer, String audience, String jwkSetUri) {
	}

}
