package com.example.merkletree.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.merkletree.domain.User;
import com.example.merkletree.domain.vo.R;
import com.example.merkletree.service.EmailService;
import com.example.merkletree.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService; // 注入邮件服务

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping("/test")
    public String test() {
        System.out.println("Test");
        return "Test";
    }

    @PostMapping("/sendVerifyCode")
    public R<String> sendVerifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        System.out.println("收到的邮箱：" + email);

        // 邮箱格式验证
        if (email == null || email.trim().isEmpty()) {
            return new R<>(400, "邮箱不能为空", null);
        }

        email = email.trim(); // 去除前后空格
        String emailPattern = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Za-z]{2,4}$";
        if (!Pattern.matches(emailPattern, email)) {
            return new R<>(400, "邮箱格式不正确", null);
        }

        // 检查邮箱是否已注册
        R<String> checkResult = userService.checkEmail(email);
        if (checkResult.getCode() != 200) {
            return checkResult;
        }

        // 发送验证码
        return userService.sendVerifyCode(email);
    }

    @PostMapping("/register")
    public R<User> register(@RequestBody User user) {
        System.err.println("注册");

        // 从Redis获取验证码
        String redisCode = redisTemplate.opsForValue().get("verify_code_" + user.getEmail());

        // 验证验证码
        if (redisCode == null || !redisCode.equals(user.getVerifyCode())) {
            return new R<>(201, "验证码错误或已过期", null);
        }

        // 执行注册（调用Service层方法）
        R<User> register = userService.register(user);

        // 注册成功后删除验证码
        if (register.getCode() == 200) {
            redisTemplate.delete("verify_code_" + user.getEmail());
        }

        return register;
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody User user) {
        System.err.println("登录");
        return userService.login(user);
    }
}