package com.twq.miaosha.access;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 限流参数的注解。
 * 被这个注解注释的方法，会进行限流防刷的逻辑
 *
 * AccessInterceptor拦截器 处理限流防刷的逻辑
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {
	/**
	 * 下面三个应该是注解时可以传入的三个参数
	 * 设置时间 就是多长时间 允许访问接口多少次
	 * 最大数量
	 * 和是否需要登录
	 * @return
	 */
	int seconds();
	int maxCount();
	boolean needLogin() default true;
}
