package com.twq.miaosha.access;


import com.twq.miaosha.domain.MiaoshaUser;

public class UserContext {
	/**
	 * 这里用 ThreadLocal 是因为
	 * 一次请求 就相当于一个线程。 而每次请求的用户可能不同，不同的请求就是不同的线程， 一次请求结束那个这个线程也结束
	 * 对应的threadlocalMap也会消失。所以不会出现内存泄漏
	 *
	 * 因此我们这里用ThreadLocal，保护不同请求中user的私有。 线程安全。

	 */
	private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();
	
	public static void setUser(MiaoshaUser user) {
		userHolder.set(user);
	}
	
	public static MiaoshaUser getUser() {
		return userHolder.get();
	}

}
