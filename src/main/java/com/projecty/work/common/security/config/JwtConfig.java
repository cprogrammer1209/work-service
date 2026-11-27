package com.projecty.work.common.security.config;

import java.util.List;

import com.projecty.work.common.config.AppProperties;
import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.security.helper.DenylistValidator;
import com.projecty.work.external.redis.service.KeyValueStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Checks Platform's access tokens locally: the signature against Platform's published keys, the issuer, and the
 * audience, and that Platform has not ended the token's session (denylist). Admin console tokens carry another
 * audience and are refused. A token with the customer audience gets
 * role USER, as in Platform.
 */
@Configuration
public class JwtConfig {

	@Bean
	public JwtDecoder jwtDecoder(AppProperties properties, KeyValueStore keyValueStore) {
		AppProperties.Token token = properties.token();
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(token.jwkSetUri()).build();
		OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
				(aud) -> aud != null && aud.contains(token.audience()));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(token.issuer()),
				audience, new DenylistValidator(keyValueStore)));
		return decoder;
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter((jwt) -> List
			.of(new SimpleGrantedAuthority(AppConstants.ROLE_PREFIX + AppConstants.ROLE_USER)));
		return converter;
	}

}
