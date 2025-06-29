package com.example.merkletree.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.merkletree.domain.User;
import com.example.merkletree.domain.vo.R;

public interface UserService extends IService<User> {
    R<User> register(User user);
    R<User> login(User user);
    R<String> sendVerifyCode(String email);
    R<String> checkEmail(String email); // 新增邮箱检查方法
}