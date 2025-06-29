package com.example.merkletree.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.merkletree.domain.MerkleTree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface MerkleTreeMapper extends BaseMapper<MerkleTree> {

    @Select("SELECT m.*, u.username FROM merkle_trees m LEFT JOIN user u ON m.user_id = u.id WHERE m.user_id = #{userId}")
    List<MerkleTree> selectByUserIdWithUsername(Integer userId);
}