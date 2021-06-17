package com.twq.miaosha.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: tangwq
 * @Date: 2021/04/28/11:21
 * @Description:
 */
public class MD5Util {

    public static String md5(String src){
        return DigestUtils.md5Hex(src);
    }

    // salt 实际上是将salt的字符零散的拼接在密码上，扰乱密码，增加安全性。
    private static final String salt = "saberTangWin";

    // MD5加密
    public static String inputPassToFormPass(String inputPass){
        String tempPas = ""+salt.charAt(0)+salt.charAt(2)+inputPass+salt.charAt(5)+salt.charAt(4);
        return md5(tempPas);
    }

    //  salt是随机的salt
    public static String formPassToDBPass(String formPass, String salt) {
        String str = ""+salt.charAt(0)+salt.charAt(2) + formPass +salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    //俩次MD5加密后， 存入数据库的密码
    public static String inputPassToDbPass(String inputPass, String saltDB) {
        // 第一次MD5
        String formPass = inputPassToFormPass(inputPass);
        // 第二次MD5
        String dbPass = formPassToDBPass(formPass, saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
        System.out.println(inputPassToDbPass("123456", salt));
        //System.out.println(inputPassToFormPass("123456"));
    }

}
