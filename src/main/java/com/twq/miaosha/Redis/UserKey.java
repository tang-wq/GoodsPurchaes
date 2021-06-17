package com.twq.miaosha.Redis;

/**
 * User的key生成。 继承了BasePrefix， 可以调用其父类的方法。
 *
 * @Author: tangwq
 * @Date: 2021/04/27/14:49
 * @Description:
 */
public class UserKey extends BasePrefix{

    private UserKey(String prefix) {
        super(prefix);
    }
    public static UserKey getById = new UserKey("id");
    public static UserKey getByName = new UserKey("name");
}
