package com.twq.miaosha.Redis;

/**
 * User的key生成。 继承了BasePrefix， 可以调用其父类的方法。
 *
 * @Author: tangwq
 * @Date: 2021/04/27/14:49
 * @Description:
 */
public class MiaoshaUserKey extends BasePrefix{

    // 默认过期时间
    public static final int TOKEN_RXPIRE = 3600*24*2; //两天
    // 带过期时间的构造函数
    private MiaoshaUserKey(int expireString, String prefix) {
        super(expireString,prefix);
    }
    public static MiaoshaUserKey token = new MiaoshaUserKey(TOKEN_RXPIRE,"tk");

    public static MiaoshaUserKey getById = new MiaoshaUserKey(0, "id");
}
