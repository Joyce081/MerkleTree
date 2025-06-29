package com.example.merkletree.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.merkletree.handler.StandardJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("merkle_trees")
public class MerkleTree {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String treeName;

    private String algorithm;

    private String rootHash;

    private Integer leafCount;

    @TableField(typeHandler = StandardJsonTypeHandler.class)
    private String treeData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @TableField(exist = false)
    private String username; // 关联查询用户信息
    @TableField(exist = false)
    private List<String> originalDataList; // 原始数据列表

    public List<String> getOriginalDataList() {
        return originalDataList;
    }

    public void setOriginalDataList(List<String> originalDataList) {
        this.originalDataList = originalDataList;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTreeName() {
        return treeName;
    }

    public void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Integer getLeafCount() {
        return leafCount;
    }

    public void setLeafCount(Integer leafCount) {
        this.leafCount = leafCount;
    }

    public String getTreeData() {
        return treeData;
    }

    public void setTreeData(String treeData) {
        this.treeData = treeData;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}