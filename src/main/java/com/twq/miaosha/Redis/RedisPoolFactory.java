package com.twq.miaosha.Redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Service
public class RedisPoolFactory {

	@Autowired
	RedisConfig redisConfig;


	/**
	 * @Bean作用在方法上，可以告诉容器 我们可以通过该方法去拿到一个 返回值类型的bean, 然后注入到在IOC容器中
	 * 因此@Bean注解的方法 必须是带返回值的，这样才可以在Spring的IOC容器中注册一个单例bean
	 *
	 * 这里因为返回了 JedisPool这个单例bean  因此 用Autoried注解可以直接获得这个类型的bean对象并注入
	 * @return
	 */
	@Bean
	public JedisPool JedisPoolFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(redisConfig.getPoolMaxIdle());
		poolConfig.setMaxTotal(redisConfig.getPoolMaxTotal());
		poolConfig.setMaxWaitMillis(redisConfig.getPoolMaxWait() * 1000);
		JedisPool jp = new JedisPool(poolConfig, redisConfig.getHost(), redisConfig.getPort(),
				redisConfig.getTimeout()*1000, redisConfig.getPassword(), 0);
		return jp;
	}
	
}
