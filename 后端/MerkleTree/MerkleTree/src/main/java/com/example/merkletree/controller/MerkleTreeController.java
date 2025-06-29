package com.example.merkletree.controller;

import com.example.merkletree.domain.MerkleTree;
import com.example.merkletree.domain.vo.R;
import com.example.merkletree.service.MerkleTreeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merkle")
public class MerkleTreeController {

    private final MerkleTreeService merkleTreeService;

    public MerkleTreeController(MerkleTreeService merkleTreeService) {
        this.merkleTreeService = merkleTreeService;
    }

    @PostMapping("/create")
    public R<MerkleTree> createMerkleTree(
            @RequestParam Integer userId,
            @RequestParam String treeName,
            @RequestParam String algorithm,
            @RequestBody List<String> dataItems) {
        return merkleTreeService.createMerkleTree(userId, treeName, algorithm, dataItems);
    }

    @GetMapping("/{id}")
    public R<MerkleTree> getTreeById(@PathVariable Integer id) {
        return merkleTreeService.getTreeById(id);
    }

    @GetMapping("/user/{userId}")
    public R<List<MerkleTree>> getUserTrees(@PathVariable Integer userId) {
        return merkleTreeService.getUserTrees(userId);
    }

    /**
     * 验证路径
     * @param treeId
     * @param leafData
     * @return
     */
    @GetMapping("/proof")
    public R<List<Map<String, String>>> getProofPath(
            @RequestParam Integer treeId,  // 改为@RequestParam
            @RequestParam String leafData) {
        return merkleTreeService.getProofPath(treeId, leafData);
    }


    // 修改验证接口
    @PostMapping("/verify-change")
    public R<Map<String, Object>> verifyDataChange(
            @RequestParam Integer treeId,
            @RequestBody Map<String, String> requestData) {
        return merkleTreeService.verifyDataChange(treeId,
                requestData.get("originalData"), requestData.get("modifiedData"));
    }

    // 增强整树验证接口
    @PostMapping("/verify-tree")
    public R<Map<String, Object>> verifyEntireTree(
            @RequestParam Integer treeId,
            @RequestBody Map<String, String> requestRootData) {
        return merkleTreeService.verifyEntireTree(treeId,
                requestRootData.get("originalData"), requestRootData.get("modifiedData"));
    }

    // 新增获取树变更检测接口
    @GetMapping("/detect-changes")
    public R<List<Map<String, String>>> detectDataChanges(
            @RequestParam Integer treeId,
            @RequestBody List<String> currentDataItems) {
        return merkleTreeService.detectDataChanges(treeId, currentDataItems);
    }

    /**
     * 删除树（记录）
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public R<String> deleteTree(@PathVariable Integer id) {
        return merkleTreeService.deleteTree(id);
    }
}