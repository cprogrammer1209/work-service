package com.projecty.work.common.security.filter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.common.security.handler.SecurityErrorWriter;
import com.projecty.work.common.service.PortalAccessService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks the {@code X-Portal-Id} header of customer requests against the token's {@code portals} claim and,
 * if allowed, stores it for {@code RequestPortalContext} (PTL-US-04). Without the header there is simply no
 * portal context; endpoints that need one ask for it. Admin console and auth endpoints are not portal-scoped.
 *
 * Created by {@code SecurityConfig} and placed after bearer-token authentication, not registered as a bean.
 */
public class PortalContextFilter extends OncePerRequestFilter {

	private final SecurityErrorWriter errorWriter;

	private final PortalAccessService portalAccessService;

	public PortalContextFilter(SecurityErrorWriter errorWriter, PortalAccessService portalAccessService) {
		this.errorWriter = errorWriter;
		this.portalAccessService = portalAccessService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(AppConstants.HEADER_PORTAL_ID);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (header == null || header.isBlank() || !(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
			chain.doFilter(request, response);
			return;
		}
		UUID portalId;
		try {
			portalId = UUID.fromString(header.strip());
		}
		catch (IllegalArgumentException ex) {
			this.errorWriter.write(response, HttpServletResponse.SC_BAD_REQUEST, AppConstants.ERROR_PORTAL_REQUIRED,
					AppConstants.MESSAGE_PORTAL_ID_INVALID);
			return;
		}
		List<String> portals = jwt.getClaimAsStringList(AppConstants.CLAIM_PORTALS);
		// The token says which portals the user had when it was issued; membership is re-checked live so a
		// deactivation takes effect at once (USR-BR-09).
		if (portals == null || !portals.contains(portalId.toString())
				|| !this.portalAccessService.isActiveMember(portalId, UUID.fromString(jwt.getSubject()))) {
			this.errorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, AppConstants.ERROR_PORTAL_ACCESS_DENIED,
					AppConstants.MESSAGE_PORTAL_ACCESS_DENIED);
			return;
		}
		request.setAttribute(AppConstants.REQUEST_ATTRIBUTE_PORTAL_ID, portalId);
		chain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !path.startsWith("/api/");
	}

}
