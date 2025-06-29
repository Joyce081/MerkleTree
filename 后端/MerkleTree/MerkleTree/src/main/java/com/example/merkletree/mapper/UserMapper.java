package com.example.merkletree.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.merkletree.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
