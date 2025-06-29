package com.example.merkletree.utils;

import java.util.Random;
/**
 * 用来生成和处理验证码的工具类
 *
 */

public class VerifyCodeUtil {

    /**
     * 生成指定长度的随机数字验证码
     * @param length 验证码长度
     * @return 随机验证码
     */
    public static String generateVerifyCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}