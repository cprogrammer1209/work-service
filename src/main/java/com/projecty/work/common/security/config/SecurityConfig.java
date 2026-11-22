package com.projecty.work.common.security.config;

import com.projecty.work.common.config.AppProperties;
import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.security.filter.PortalContextFilter;
import com.projecty.work.common.security.handler.RestAccessDeniedHandler;
import com.projecty.work.common.security.handler.RestAuthenticationEntryPoint;
import com.projecty.work.common.security.handler.SecurityErrorWriter;
import com.projecty.work.common.service.PortalAccessService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless API security. Access tokens are JWTs issued by Platform and checked here by Spring's OAuth2 resource
 * server support; the {@code JwtDecoder} bean is in {@link JwtConfig}. Work never talks to Platform to check a
 * request.
 *
 * CSRF protection is off: API calls authenticate with a bearer header, which browsers never attach on their own.
 * {@code PortalContextFilter} then checks the active portal ({@code X-Portal-Id}) against the token and against
 * Work's own copy of the portal's members.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain apiSecurity(HttpSecurity http, RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler, SecurityErrorWriter errorWriter,
			PortalAccessService portalAccessService) throws Exception {
		return http.csrf((csrf) -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((auth) -> auth
				.requestMatchers(HttpMethod.GET, "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**",
						"/swagger-ui.html")
				.permitAll()
				.requestMatchers("/api/**")
				.hasRole(AppConstants.ROLE_USER)
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.addFilterAfter(new PortalContextFilter(errorWriter, portalAccessService), BearerTokenAuthenticationFilter.class)
			.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.cors().allowedOrigins());
		config.addAllowedMethod("*");
		config.addAllowedHeader("*");
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
