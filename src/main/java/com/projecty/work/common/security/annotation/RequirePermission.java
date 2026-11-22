package com.projecty.work.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The caller's profile in the request's portal must grant this permission (a name from
 * {@code PermissionConstants}), or the request gets 403. Implies a portal context (X-Portal-Id).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

	String value();

}
