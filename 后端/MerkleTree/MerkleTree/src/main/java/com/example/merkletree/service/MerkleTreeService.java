package com.example.merkletree.service;

import com.example.merkletree.domain.MerkleTree;
import com.example.merkletree.domain.vo.R;

import java.util.List;
import java.util.Map;

public interface MerkleTreeService {
    R<MerkleTree> createMerkleTree(Integer userId, String treeName, String algorithm, List<String> dataItems);

    R<MerkleTree> getTreeById(Integer id);

    R<List<MerkleTree>> getUserTrees(Integer userId);
    R<List<Map<String, String>>> getProofPath(Integer treeId, String leafData);
    /**
     * 验证数据变更
     * @param treeId 树ID
     * @param originalData 原始数据
     * @param modifiedData 修改后的数据
     * @return 包含验证结果的Map，包括原始哈希、修改后哈希、是否变更等信息
     */
    R<Map<String, Object>> verifyDataChange(Integer treeId, String originalData, String modifiedData);

    /**
     * 检测数据变更
     * @param treeId 树ID
     * @param currentDataItems 当前数据项列表
     * @return 变更列表，包含位置、原始数据、当前数据、哈希对比等信息
     */
    R<List<Map<String, String>>> detectDataChanges(Integer treeId, List<String> currentDataItems);

    /**
     * 验证整棵树完整性
     * @param treeId 树ID
     * @return 包含详细验证结果的Map，包括存储的根哈希、计算的根哈希、是否有效等信息
     */
    R<Map<String, Object>> verifyEntireTree(Integer treeId, String originalData, String modifiedData);

    R<String> deleteTree(Integer id);
}