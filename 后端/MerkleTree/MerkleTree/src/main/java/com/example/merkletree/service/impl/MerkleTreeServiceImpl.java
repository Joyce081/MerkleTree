package com.example.merkletree.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.merkletree.domain.Leaf;
import com.example.merkletree.domain.MerkleTree;
import com.example.merkletree.mapper.LeafMapper;
import com.example.merkletree.mapper.MerkleTreeMapper;
import com.example.merkletree.service.MerkleTreeService;
import com.example.merkletree.domain.vo.R;
import com.example.merkletree.utils.MerkleTreeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.example.merkletree.utils.MerkleTreeUtil.ProofNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MerkleTreeServiceImpl implements MerkleTreeService {

    private final MerkleTreeMapper merkleTreeMapper;
    private final LeafMapper leafMapper;
    private final ObjectMapper objectMapper;

    public MerkleTreeServiceImpl(MerkleTreeMapper merkleTreeMapper,
                                 LeafMapper leafMapper,
                                 ObjectMapper objectMapper) {
        this.merkleTreeMapper = merkleTreeMapper;
        this.leafMapper = leafMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建树
     * @param userId
     * @param treeName
     * @param algorithm
     * @param dataItems
     * @return
     */
    @Override
    @Transactional
    public R<MerkleTree> createMerkleTree(Integer userId, String treeName, String algorithm, List<String> dataItems) {
        try {
            // 1. 生成Merkle树结构
            MerkleTreeUtil.TreeInfo treeInfo = MerkleTreeUtil.buildMerkleTree(dataItems, algorithm);

            // 2. 确保生成的treeJson是标准JSON（非转义字符串）
            String validJson = treeInfo.getTreeJson();
            // 此时validJson已经是标准JSON，无需额外处理

            // 3. 保存树信息（确保数据库存储标准JSON）
            MerkleTree tree = new MerkleTree();
            tree.setUserId(userId);
            tree.setTreeName(treeName);
            tree.setAlgorithm(algorithm);
            tree.setRootHash(treeInfo.getRootHash());
            tree.setLeafCount(dataItems.size());
            tree.setTreeData(validJson); // 直接存储标准JSON
            tree.setCreatedAt(new Date());
            merkleTreeMapper.insert(tree);

            // 4. 保存叶子节点（保持不变）
            for (int i = 0; i < dataItems.size(); i++) {
                Leaf leaf = new Leaf();
                leaf.setTreeId(tree.getId());
                leaf.setOriginalData(dataItems.get(i));
                leaf.setHashValue(treeInfo.getLeafHashes().get(i));
                leaf.setPosition(i);
                leafMapper.insert(leaf);
            }
            // 打印验证JSON格式
            System.out.println("生成的JSON: " + treeInfo.getTreeJson());
            return R.success(tree);
        } catch (Exception e) {
            return R.error("生成MerkleTree失败: " + e.getMessage());
        }
    }

    /**
     * 查询树
     * @param id
     * @return
     */
    @Override
    public R<MerkleTree> getTreeById(Integer id) {
        MerkleTree tree = merkleTreeMapper.selectById(id);
        if (tree == null) return R.error("未找到指定的MerkleTree");

        // 添加叶子节点数据
        List<Leaf> leaves = leafMapper.selectList(
                new QueryWrapper<Leaf>()
                        .eq("tree_id", id)
                        .orderByAsc("position")
        );

        tree.setOriginalDataList(
                leaves.stream()
                        .map(Leaf::getOriginalData)
                        .collect(Collectors.toList())
        );

        return R.success(tree);
    }


    /**
     * 查询用户的树
     * @param userId
     * @return
     */
    @Override
    public R<List<MerkleTree>> getUserTrees(Integer userId) {
        List<MerkleTree> trees = merkleTreeMapper.selectByUserIdWithUsername(userId);
        return R.success(trees);
    }

    /**
     * 获取验证路径
     * @param treeId
     * @param leafData
     * @return
     */
    @Override
    public R<List<Map<String, String>>> getProofPath(Integer treeId, String leafData) {
        try {
            // 1. 获取树信息
            MerkleTree tree = merkleTreeMapper.selectById(treeId);
            if (tree == null) return R.error("树不存在");

            // 2. 解析树数据（兼容双重转义JSON）
            Map<String, Object> treeStructure;
            try {
                String rawJson = objectMapper.readValue(tree.getTreeData(), String.class);
                treeStructure = objectMapper.readValue(rawJson,
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // 如果不是双重转义，尝试直接解析
                treeStructure = objectMapper.readValue(tree.getTreeData(),
                        new TypeReference<Map<String, Object>>() {});
            }

            // 3. 获取验证路径
            List<Map<String, String>> leaves = (List<Map<String, String>>) treeStructure.get("leaves");
            List<List<String>> levels = (List<List<String>>) treeStructure.get("levels");

            // 查找叶子节点位置
            int position = IntStream.range(0, leaves.size())
                    .filter(i -> leaves.get(i).get("data").equals(leafData))
                    .findFirst()
                    .orElse(-1);

            if (position == -1) return R.error("叶子节点不存在");

            // 生成验证路径
            List<ProofNode> proofPath = MerkleTreeUtil.getProofPath(levels, position, tree.getAlgorithm());

            // 转换为DTO
            List<Map<String, String>> pathResult = proofPath.stream()
                    .map(node -> Map.of(
                            "hash", node.getHash(),
                            "direction", node.getDirection()
                    ))
                    .collect(Collectors.toList());

            return R.success(pathResult);
        } catch (Exception e) {
            return R.error("获取验证路径失败: " + e.getMessage());
        }
    }

    /**
     * 修改数据
     * @param treeId 树ID
     * @param originalData 原始数据
     * @param modifiedData 修改后的数据
     * @return
     */
    @Override
    public R<Map<String, Object>> verifyDataChange(Integer treeId, String originalData, String modifiedData) {
        try {
            // 1. 获取树信息
            MerkleTree tree = merkleTreeMapper.selectById(treeId);
            if (tree == null) {
                return R.error("树不存在");
            }

            // 2. 查找原始叶子节点
            QueryWrapper<Leaf> wrapper = new QueryWrapper<>();
            wrapper.eq("tree_id", treeId)
                    .eq("original_data", originalData);
            Leaf originalLeaf = leafMapper.selectOne(wrapper);

            if (originalLeaf == null) {
                return R.error("原始叶子节点不存在");
            }

            // 3. 计算修改后的哈希
            String modifiedHash = MerkleTreeUtil.hashData(modifiedData, tree.getAlgorithm());

            // 4. 准备响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("originalHash", originalLeaf.getHashValue());
            result.put("modifiedHash", modifiedHash);
            result.put("isChanged", !modifiedHash.equals(originalLeaf.getHashValue()));
            result.put("algorithm", tree.getAlgorithm());

            return R.success(result);
        } catch (Exception e) {
            return R.error("验证过程中出错: " + e.getMessage());
        }
    }


    @Override
    public R<List<Map<String, String>>> detectDataChanges(Integer treeId, List<String> currentDataItems) {
        try {
            // 1. 获取树信息和所有叶子节点
            MerkleTree tree = merkleTreeMapper.selectById(treeId);
            if (tree == null) {
                return R.error("树不存在");
            }

            QueryWrapper<Leaf> wrapper = new QueryWrapper<>();
            wrapper.eq("tree_id", treeId)
                    .orderByAsc("position");
            List<Leaf> storedLeaves = leafMapper.selectList(wrapper);

            // 2. 检查数据项数量是否匹配
            if (currentDataItems.size() != storedLeaves.size()) {
                return R.error("数据项数量不匹配，无法比较");
            }

            // 3. 比较每个数据项
            List<Map<String, String>> changes = new ArrayList<>();
            for (int i = 0; i < storedLeaves.size(); i++) {
                Leaf storedLeaf = storedLeaves.get(i);
                String currentData = currentDataItems.get(i);
                String currentHash = MerkleTreeUtil.hashData(currentData, tree.getAlgorithm());

                if (!currentHash.equals(storedLeaf.getHashValue())) {
                    Map<String, String> change = new HashMap<>();
                    change.put("position", String.valueOf(i));
                    change.put("originalData", storedLeaf.getOriginalData());
                    change.put("currentData", currentData);
                    change.put("originalHash", storedLeaf.getHashValue());
                    change.put("currentHash", currentHash);
                    changes.add(change);
                }
            }

            return R.success(changes);
        } catch (Exception e) {
            return R.error("检测数据变更失败: " + e.getMessage());
        }
    }

    /**
     * 整树验证
     * @param treeId 树ID
     * @return
     */
    @Override
    public R<Map<String, Object>> verifyEntireTree(Integer treeId, String originalData, String modifiedData) {
        try {
            // 1. 获取树信息和所有叶子节点
            MerkleTree tree = merkleTreeMapper.selectById(treeId);
            if (tree == null) {
                return R.error("树不存在");
            }

            QueryWrapper<Leaf> wrapper = new QueryWrapper<>();
            wrapper.eq("tree_id", treeId)
                    .orderByAsc("position");
            List<Leaf> leaves = leafMapper.selectList(wrapper);

            // 2. 重新构建树并比较根哈希
//            List<String> dataItems = leaves.stream()
//                    .map(Leaf::getOriginalData)
//                    .collect(Collectors.toList());
            List<String> dataItems = new ArrayList<>();
            for (Leaf leaf : leaves) {
                if(leaf.getOriginalData().equals(originalData)) {
                    dataItems.add(modifiedData);
                } else {
                    dataItems.add(leaf.getOriginalData());
                }
            }

            MerkleTreeUtil.TreeInfo treeInfo = MerkleTreeUtil.buildMerkleTree(dataItems, tree.getAlgorithm());

            // 3. 准备详细验证结果
            Map<String, Object> result = new HashMap<>();
            result.put("isValid", treeInfo.getRootHash().equals(tree.getRootHash()));
            result.put("storedRootHash", tree.getRootHash());
            result.put("computedRootHash", treeInfo.getRootHash());
            result.put("algorithm", tree.getAlgorithm());
            result.put("leafCount", leaves.size());

            // 如果验证失败，添加更多诊断信息
            if (!treeInfo.getRootHash().equals(tree.getRootHash())) {
                result.put("message", "树数据已被篡改，根哈希不匹配");
            } else {
                result.put("message", "树完整性验证通过");
            }

            return R.success(result);
        } catch (Exception e) {
            return R.error("验证过程中出错: " + e.getMessage());
        }
    }

    /**
     * 删除树（记录）
     * @param id
     * @return
     */
    @Override
    @Transactional
    public R<String> deleteTree(Integer id) {
        try {
            // 1. 删除关联的叶子节点
            QueryWrapper<Leaf> leafWrapper = new QueryWrapper<>();
            leafWrapper.eq("tree_id", id);
            leafMapper.delete(leafWrapper);

            // 2. 删除树记录
            int result = merkleTreeMapper.deleteById(id);

            if (result > 0) {
                return R.success("删除成功");
            } else {
                return R.error("未找到指定记录");
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.error("删除失败: " + e.getMessage());
        }
    }
}