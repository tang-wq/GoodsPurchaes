package com.twq.miaosha.Redis;

/**
 *
 *key前缀基本实现方法
 * @Author: tangwq
 * @Date: 2021/04/27/14:44
 * @Description:
 */
public abstract class BasePrefix implements KeyPrefix{
    private int expireSeconds;

    private String prefix;

    /**
     * 构造方法 设置过期时间为0
     * @param prefix
     */
    public BasePrefix(String prefix) {//0代表永不过期
        this(0, prefix);
    }

    public BasePrefix( int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }

    public int expireSeconds() {//默认0代表永不过期
        return expireSeconds;
    }

    /**
     * 获取前缀， 使用类名+prefix参数的方式，
     * 生成key的前缀策略
     * @return
     */
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className+":" + prefix;
    }
}
