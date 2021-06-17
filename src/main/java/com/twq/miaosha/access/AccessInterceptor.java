package com.twq.miaosha.access;

import java.io.OutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twq.miaosha.Redis.AccessKey;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.result.Result;
import com.twq.miaosha.service.MiaoshaUserService;
import com.twq.miaosha.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSON;

/**
 * 定义一个限流拦截器
 * 拦截一些参数，进行逻辑处理，这里是对AccessLimit注解的参数经行比较。
 * 进行限流防刷 同一个用户在某个时间段内最多访问这个接口多少次
 */
@Service
public class AccessInterceptor  implements HandlerInterceptor {
	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;

	/**
	 * Handle实际上就是 url映射的controller
	 *
	 * preHandle就是在controller方法之前拦截
	 *
	 * @param request
	 * @param response
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 如果这个 handler 是 HandlerMethod 类型 实际上就是 url映射的是controller中的方法
		if(handler instanceof HandlerMethod) {
			MiaoshaUser user = getUser(request, response);
			/**
			 * 将user存入 UserContext的user对象中。 然后请求的接口就可以直接获取到user对象。
			 */
			UserContext.setUser(user);
			HandlerMethod hm = (HandlerMethod)handler;
			// 获取方法是否被AccessLimit注解
			AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
			//如果没有被注解，则不进行参数校验（或者说是限流防刷逻辑）
			if(accessLimit == null) {
				return true;
			}
			// seconds 时间段内访问接口多少次。 实际上就是设置redis中访问接口次数的key的有效时间
			// 如果时间到 则key消失， 重新统计次数 这样就可以限制seconds时间内，一个接口的访问次数了。
			int seconds = accessLimit.seconds();
			int maxCount = accessLimit.maxCount();
			boolean needLogin = accessLimit.needLogin();
			String key = request.getRequestURI();
			if(needLogin) {
				if(user == null) {
					render(response, CodeMsg.SESSION_ERROR);
					return false;
				}
				key += "_" + user.getId();
			}else {
				//do nothing
			}
			AccessKey ak = AccessKey.withExpire(seconds);
			Integer count = redisService.get(ak, key, Integer.class);

	    	if(count  == null) {// 如果是第一此进入这个接口，则在redis中初始化访问次数。
	    		 redisService.set(ak, key, 1);
	    	}else if(count < maxCount) {// 如果访问次数小于 注解预定的最大次数，则count+1 且可以继续访问
	    		 redisService.incr(ak, key);
	    	}else {
	    		// 否则限制访问。
	    		render(response, CodeMsg.ACCESS_LIMIT_REACHED);
	    		return false;
	    	}
		}
		return true;
	}
	
	private void render(HttpServletResponse response, CodeMsg cm)throws Exception {
		response.setContentType("application/json;charset=UTF-8");
		OutputStream out = response.getOutputStream();
		String str  = JSON.toJSONString(Result.error(cm));

		// 写出去， 给前端获得。
		out.write(str.getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response) {
		String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKRN);
		String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKRN);
		if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
			return null;
		}
		String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
		return userService.getByToken(response, token);
	}
	
	private String getCookieValue(HttpServletRequest request, String cookiName) {
		Cookie[]  cookies = request.getCookies();
		if(cookies == null || cookies.length <= 0){
			return null;
		}
		for(Cookie cookie : cookies) {
			if(cookie.getName().equals(cookiName)) {
				return cookie.getValue();
			}
		}
		return null;
	}
	
}
