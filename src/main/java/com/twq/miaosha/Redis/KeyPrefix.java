package com.twq.miaosha.Redis;

/**
 * key前缀，防止存入redis的key重复
 * 定义一些前缀生成策略。
 *
 * @Author: tangwq
 * @Date: 2021/04/27/14:44
 * @Description:
 */
public interface KeyPrefix {

    // 设定有效期
    public int expireSeconds();

    // 获得前缀
    public String getPrefix();
}
