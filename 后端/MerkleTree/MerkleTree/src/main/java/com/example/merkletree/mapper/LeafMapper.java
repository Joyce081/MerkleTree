package com.example.merkletree.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.merkletree.domain.Leaf;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeafMapper extends BaseMapper<Leaf> {
}