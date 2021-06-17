package com.twq.miaosha.service;

import com.alibaba.fastjson.JSON;
import com.twq.miaosha.Redis.BasePrefix;
import com.twq.miaosha.Redis.KeyPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/04/27/13:05
 * @Description:
 */
@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;  // 通过 redis/RedisPoolFactory.class这个bean中对 方法@Bean注解 在容器中生成了JedisPool的bean 然后在这里注入


    /**
     * 这里为什么要用泛型和反射
     * 是因为将这个方法可以写成通用的方式，就可以在很多地方调用，不限于参数的格式
     * 通过反射和泛型，可以确定参数的格式，然后返回对应类型，或者泛型。然后在做处理。
     * @param prefix  前缀策略。 获取一个前缀
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public <T>T get(KeyPrefix prefix,String key, Class<T> clazz){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String value = jedis.get(prefix.getPrefix()+key);
            // 将获得的value 通过反射 转换为 对象或者泛型
            T t = StringToBean(value, clazz);
            return t;
        }finally {
            returnToPool(jedis);
        }
    }

    public <T>boolean set(KeyPrefix prefix,String key, T value){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String val = beanToStr(value);
            if (val==null){
                return false;
            }
            int expireSeconds = prefix.expireSeconds(); // 设置过期时间
            if(expireSeconds<=0){
                System.out.println(prefix.getPrefix()+key);
                jedis.set(prefix.getPrefix()+key, val);
            }else{
                jedis.setex(prefix.getPrefix()+key,expireSeconds ,val);
            }

            return true;
        }finally {
            returnToPool(jedis);
        }
    }

    /**
     * 给key中存储的value +1 这个value是数字，
     * 如果 key 不存在或者key对应的值不为数字，那么 key 对应的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * @param prefix
     * @param key
     * @param <T>
     * @return
     */
    public <T>Long incr(KeyPrefix prefix, String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.incr(prefix.getPrefix()+key);
        }finally {
            returnToPool(jedis);
        }
    }

    /**
     * 给key中存储的value -1 这个value是数字，
     * 如果 key 不存在或者key对应的值不为数字，那么 key 对应的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * @param prefix  前缀策略。 获取一个前缀
     * @param key
     * @param <T>
     * @return  返回的是减少后的值
     */
    public <T>Long decr(KeyPrefix prefix, String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.decr(prefix.getPrefix()+key);
        }finally {
            returnToPool(jedis);
        }
    }

    public Boolean isExist(BasePrefix prefix,String key){

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Boolean isExit = jedis.exists(prefix.getPrefix()+key);
            return isExit;
        }finally {
            returnToPool(jedis);
        }
    }


    // 将String 转换为 bean对象 PS bean对象实际上就是class。 在Spring中 则叫Bean

    /**
     * 这里为什么要用泛型和反射
     * 是因为将这个方法可以写成通用的方式，就可以在很多地方调用，不限于参数的格式
     * 通过反射和泛型，可以确定参数的格式，然后返回对应类型，或者泛型。然后在做处理。
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public static  <T>T StringToBean(String str, Class<T> clazz) {
        if(str==null||str.length()<=0||clazz==null){
            return null;
        }

        if(clazz == int.class || clazz == Integer.class){
            return (T)Integer.valueOf(str);
        }else if(clazz == String.class){
            return (T)str;
        }else if(clazz==long.class||clazz==Long.class){
            return (T)Long.valueOf(str);
        }else{ //其他类型则认为他是一个classBean，转换为Json的String对象。
            return JSON.toJavaObject(JSON.parseObject(str),clazz);
        }
    }



    public static  <T> String beanToStr(T value) {
        if(value==null){
            return null;
        }

        // 利用反射判断 value的类型，然后将其转换为字符串。
        Class<?> clazz = value.getClass();
        if(clazz == int.class || clazz == Integer.class){
            return ""+value;
        }else if(clazz == String.class){
            return (String)value;
        }else if(clazz==long.class||clazz==Long.class){
            return ""+value;
        }else{ //其他类型则认为他是一个classBean，转换为Json的String对象。
            return JSON.toJSONString(value);
        }

    }

    /**
     * Jedis 使用完成后 将其释放回连接池，否则会资源耗尽。
     * @param jedis
     */
    private void returnToPool(Jedis jedis){
        if(jedis !=null){
            jedis.close(); // close() 如果有连接池则不会关闭jedis链接 ，而是将其释放连接池等待下次使用， 否则则会关闭jedis链接。
        }
    }


    /**
     * 删除
     * */
    public boolean delete(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis =  jedisPool.getResource();
            //生成真正的key
            String realKey  = prefix.getPrefix() + key;
            long ret =  jedis.del(realKey);
            return ret > 0;
        }finally {
            returnToPool(jedis);
        }
    }


}
