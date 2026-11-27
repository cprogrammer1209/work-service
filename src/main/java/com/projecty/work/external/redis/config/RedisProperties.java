package com.projecty.work.external.redis.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param type {@code redis} or {@code memory} (tests: everything stays in this process)
 * @param accessTtl how long a cached "is this user an active member, with which permissions" answer may live;
 * applying a member event evicts it at once, so this is only the safety net
 * @param definitionsTtl how long cached custom field, layout and status definitions may live; a change bumps the
 * portal's version, so old entries are never read again and simply expire
 * @param responseTtl how long a cached read (a lookup or an API response) may live; a change also moves the version
 * its key carries, so this is only the safety net and must stay shorter than one day
 * @param backoff after a failed Redis call, how long Redis is left alone (callers get "nothing cached" at once)
 * instead of every request waiting for its own timeout
 */
@ConfigurationProperties("projecty.redis")
public record RedisProperties(String type, Duration accessTtl, Duration definitionsTtl, Duration responseTtl, Duration backoff) {
}
