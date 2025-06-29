package com.example.merkletree.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.merkletree.domain.User;
import com.example.merkletree.domain.vo.R;
import com.example.merkletree.mapper.UserMapper;
import com.example.merkletree.service.EmailService;
import com.example.merkletree.service.UserService;
import com.example.merkletree.utils.VerifyCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailService emailService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Override
    public R<User> register(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", user.getEmail());
        User userDatabase = userMapper.selectOne(wrapper);
        if (userDatabase != null) {
            return new R<>(201, "用户已存在!", null);
        } else {
            // 密码加密
            user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
            int insert = userMapper.insert(user);
            if (insert > 0) {
                return new R<>(200, "注册成功!", user);
            } else {
                return new R<>(201, "注册失败!", null);
            }
        }
    }

    @Override
    public R<User> login(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", user.getEmail());
        User userDatabase = userMapper.selectOne(wrapper);
        if (bCryptPasswordEncoder.matches(user.getPassword(), userDatabase.getPassword())) {
            // 实际项目中应比对密码（此处简化）
            return new R<>(200, "登录成功!", userDatabase);
        } else {
            return new R<>(201, "登录失败!", null);
        }
    }

    @Override
    public R<String> sendVerifyCode(String email) {
        try {
            // 生成6位验证码
            String verifyCode = VerifyCodeUtil.generateVerifyCode(6);

            // 构造邮件内容
            String subject = "注册验证码";
            String content = "您的注册验证码为：" + verifyCode + "，有效期为5分钟。";

            // 发送邮件
            emailService.sendSimpleMail(email, subject, content);

            // 将验证码存入Redis，有效期5分钟
            redisTemplate.opsForValue().set("verify_code_" + email, verifyCode, 5, TimeUnit.MINUTES);

            return new R<>(200, "验证码发送成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new R<>(500, "验证码发送失败", null);
        }
    }

    @Override
    public R<String> checkEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            return new R<>(201, "该邮箱已被注册", null);
        }
        return new R<>(200, "邮箱可用", null);
    }
}