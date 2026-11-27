package com.projecty.work.common.security.helper;

import com.projecty.work.common.constants.AppConstants;
import com.projecty.work.external.redis.service.KeyValueStore;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Refuses an access token whose session Platform has ended (logout, password reset, support session ended), so a
 * revoked session stops working here at once instead of when the token expires. If Redis is down nothing is
 * denied, and the token's own expiry still applies.
 */
public class DenylistValidator implements OAuth2TokenValidator<Jwt> {

	private final KeyValueStore keyValueStore;

	public DenylistValidator(KeyValueStore keyValueStore) {
		this.keyValueStore = keyValueStore;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		String sessionId = jwt.getClaimAsString(AppConstants.CLAIM_SESSION_ID);
		if (sessionId != null && this.keyValueStore.exists(AppConstants.DENYLIST_KEY_PREFIX + sessionId)) {
			return OAuth2TokenValidatorResult
				.failure(new OAuth2Error(AppConstants.ERROR_SESSION_ENDED, AppConstants.MESSAGE_SESSION_ENDED, null));
		}
		return OAuth2TokenValidatorResult.success();
	}

}
