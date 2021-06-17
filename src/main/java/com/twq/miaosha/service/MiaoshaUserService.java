package com.twq.miaosha.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.twq.miaosha.Redis.MiaoshaUserKey;
import com.twq.miaosha.dao.MiaoshaUserDao;
import com.twq.miaosha.domain.MiaoshaUser;
import com.twq.miaosha.expection.GlobalException;
import com.twq.miaosha.result.CodeMsg;
import com.twq.miaosha.util.MD5Util;
import com.twq.miaosha.util.UUIDUtil;
import com.twq.miaosha.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class MiaoshaUserService {

	public static final String COOKIE_NAME_TOKRN = "token";
	
	@Autowired
	MiaoshaUserDao miaoshaUserDao;

	@Autowired
	RedisService redisService;


	public MiaoshaUser getByIdNoRedis(Long id){
		return miaoshaUserDao.getById(id);
	}

	/**
	 * 第一次登录查找用户对象后，将其放入缓存。 第二次登陆 就不必从数据库中查找
	 *
	 * 但是 更改用户信息之后，需要将缓存的对应数据更新。
	 * @param id
	 * @return
	 */
	public MiaoshaUser getById(Long id){
		//取缓存
		MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, ""+id, MiaoshaUser.class);
		if(user != null) {
			return user;
		}
		//取数据库
		user = miaoshaUserDao.getById(id);
		if(user != null) {
			redisService.set(MiaoshaUserKey.getById, ""+id, user);
		}
		return user;

	}


	public String login(HttpServletResponse response,LoginVo loginVo) {

		if(loginVo == null){
			// 直接抛全局异常， 会有我们写的全局异常拦截器GlobalExceptionHandler去拦截处理
			throw  new GlobalException(CodeMsg.SERVER_ERROR);
		}

		String mobile = loginVo.getMobile();
		String pw = loginVo.getPassword();

		MiaoshaUser miaoshaUser = getById(Long.parseLong(mobile));
		if(miaoshaUser==null){
			// 直接抛全局异常， 会有我们写的全局异常处理器GlobalExceptionHandler去拦截处理
			throw  new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}

		// 验证密码
		String dbPass = miaoshaUser.getPassword();
		String dbSalt = miaoshaUser.getSalt();
		String calPw = MD5Util.formPassToDBPass(pw,dbSalt);
		if(dbPass.equals(calPw)){
			// 登录成功后 生成一个Cookie
			String token = UUIDUtil.uuid();
			//  存入cookie和redis。
			addCookieAndRedis(response, miaoshaUser,token);
			return token;
		}else{
			// 密码不正确 ， 直接抛全局异常， 会有我们写的全局异常处理器GlobalExceptionHandler去拦截处理
			throw  new GlobalException(CodeMsg.PASSWORD_ERROR);
		}

	}

	public MiaoshaUser getByToken(HttpServletResponse response, String token) {
		if(StringUtils.isEmpty(token)){
			return null;
		}
		// 从redis中获取对用token的user对象。
		MiaoshaUser user = redisService.get(MiaoshaUserKey.token,token,MiaoshaUser.class);

		if(user!=null){
			// 再次调用此方法，刷新token在Cookie和redis缓存中的用户信息存在的时间
			// 因此用户调用了网页的某个接口，就证明他是活跃的，当他不调用任何接口的时候，他的信息就会清除然后重新登录
			// 我们登陆网站的时候也是这样的。
			addCookieAndRedis(response, user, token);
		}

		return user;
	}

	/**
	 * 生成用户的token，然后将token存入cookie， 将token作为key 用户信息作为value存入redis
	 * @param response
	 * @param user
	 */
	private void addCookieAndRedis(HttpServletResponse response, MiaoshaUser user,String token){


		// 将User对象以token(存入redis的token是将生成的token做了一次封装)作为key存入redis。  实现分布式session
		redisService.set(MiaoshaUserKey.token,token,user);
		Cookie cookie = new Cookie(COOKIE_NAME_TOKRN, token);
		//设置cookie的有效期， 这里设成和redis中的有效期保持一致（在定义的MiaoshaUserKey中）
		cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
		//设置cookie的路径（注意这里是url）， 我们设置为网站根目录
		cookie.setPath("/");
		// cookie 存入客户端
		response.addCookie(cookie);
	}

	/**
	 * 修改用户密码时，注意缓存中的对象也需要改变
	 *
	 * 一般为了保证redis和数据库一致性 都会先更新数据库 然后删除缓存。
	 * 在下次登录后，就会把数据库对象重新存入缓存中，就保证了数据库和redis的一致性，
	 *
	 * 如果先删redis在更新数据库， 就会出现 在更新数据库时候，又有新的请求请求到数据库对应的数据，将更新之前的数据又放入到了redis这就就出现了脏数据
	 * @param token
	 * @param id
	 * @param formPass
	 * @return
	 */
	// http://blog.csdn.net/tTU1EvLDeLFq5btqiK/article/details/78693323
	public boolean updatePassword(String token, long id, String formPass) {
		//取user
		MiaoshaUser user = getById(id);
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//更新数据库
		MiaoshaUser toBeUpdate = new MiaoshaUser();
		toBeUpdate.setId(id);
		toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
		miaoshaUserDao.update(toBeUpdate);
		//处理缓存 删除缓存中 对应的对象
		redisService.delete(MiaoshaUserKey.getById, ""+id);
		user.setPassword(toBeUpdate.getPassword());
		redisService.set(MiaoshaUserKey.token, token, user);
		return true;
	}
}
