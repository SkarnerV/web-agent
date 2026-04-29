package com.agentplatform.common.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 方法参数注解：注入当前请求 {@link UserPrincipal}。
 *
 * <p>解析逻辑见 {@code CurrentUserArgumentResolver}（位于 ap-app）。</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
