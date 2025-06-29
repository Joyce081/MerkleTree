package com.example.merkletree.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerkleTreeUtil {

    public static class TreeInfo {
        private String rootHash;
        private List<String> leafHashes;
        private String treeJson;
        private List<List<String>> levels; // 添加树的各层哈希

        public String getRootHash() {
            return rootHash;
        }

        public void setRootHash(String rootHash) {
            this.rootHash = rootHash;
        }

        public List<String> getLeafHashes() {
            return leafHashes;
        }

        public void setLeafHashes(List<String> leafHashes) {
            this.leafHashes = leafHashes;
        }

        public String getTreeJson() {
            return treeJson;
        }

        public void setTreeJson(String treeJson) {
            this.treeJson = treeJson;
        }
        public List<List<String>> getLevels() {
            return levels;
        }

        public void setLevels(List<List<String>> levels) {
            this.levels = levels;
        }
    }
    public static class ProofNode {
        private String hash;
        private String direction; // "left" or "right"

        public ProofNode(String hash, String direction) {
            this.hash = hash;
            this.direction = direction;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    /**
     * 构建树
     * @param dataItems
     * @param algorithm
     * @return
     */
    public static TreeInfo buildMerkleTree(List<String> dataItems, String algorithm) {
        try {
            List<List<String>> levels = new ArrayList<>();
            List<String> currentLevel = new ArrayList<>();

            // 1. 生成叶子节点哈希
            for (String data : dataItems) {
                currentLevel.add(hashData(data, algorithm));
            }
            List<String> leafHashes = new ArrayList<>(currentLevel);

            // 添加叶子层级（最底层）
            levels.add(new ArrayList<>(currentLevel));

            // 2. 构建Merkle树各层（从叶子到根）
            while (currentLevel.size() > 1) {
                List<String> nextLevel = new ArrayList<>();
                for (int i = 0; i < currentLevel.size(); i += 2) {
                    String left = currentLevel.get(i);
                    String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                    nextLevel.add(hashData(left + right, algorithm));
                }
                currentLevel = nextLevel;
                // 每次将新层级添加到列表开头，确保根节点在第一个位置
                levels.add(0, new ArrayList<>(currentLevel));
            }

            // 3. 准备返回数据
            TreeInfo info = new TreeInfo();
            info.setRootHash(currentLevel.get(0));
            info.setLeafHashes(leafHashes);
            info.setLevels(levels);

            // 4. 生成完整的树结构JSON
            Map<String, Object> treeStructure = new HashMap<>();
            treeStructure.put("root", info.getRootHash());
            treeStructure.put("levels", levels);

            // 添加叶子节点数据
            List<Map<String, String>> leavesWithData = new ArrayList<>();
            for (int i = 0; i < dataItems.size(); i++) {
                leavesWithData.add(Map.of(
                        "data", dataItems.get(i),
                        "hash", leafHashes.get(i)
                ));
            }
            treeStructure.put("leaves", leavesWithData);

            ObjectMapper mapper = new ObjectMapper();
            info.setTreeJson(mapper.writeValueAsString(treeStructure));

            return info;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("生成树结构JSON失败", e);
        }
    }
    /**
     * 获取验证路径
     */
    /**
     * 获取验证路径（包含从叶子节点到根节点的所有相关节点）
     */
    public static List<ProofNode> getProofPath(List<List<String>> levels, int leafIndex, String algorithm) {
        List<ProofNode> proofPath = new ArrayList<>();
        int currentIndex = leafIndex;

        // 从叶子层开始，向上直到根层
        for (int level = levels.size() - 1; level > 0; level--) {
            List<String> currentLevel = levels.get(level);

            // 处理奇数个节点的情况
            if (currentLevel.size() > 1 && currentIndex == currentLevel.size() - 1 && currentLevel.size() % 2 == 1) {
                // 如果是奇数层的最后一个节点，与前一个节点配对
                currentIndex--;
            }

            // 确定兄弟节点的位置和方向
            int siblingIndex = (currentIndex % 2 == 0) ? currentIndex + 1 : currentIndex - 1;
            String direction = (currentIndex % 2 == 0) ? "right" : "left";

            // 确保兄弟节点存在
            if (siblingIndex < currentLevel.size()) {
                proofPath.add(new ProofNode(currentLevel.get(siblingIndex), direction));
            }

            // 移动到上一层的位置
            currentIndex /= 2;
        }

        // 添加根节点
        if (!levels.isEmpty() && !levels.get(0).isEmpty()) {
            proofPath.add(new ProofNode(levels.get(0).get(0), "root"));
        }

        return proofPath;
    }

    /**
     * 计算数据的哈希值
     */
    public static String hashData(String data, String algorithm) {
        return switch (algorithm.toLowerCase()) {
            /**
             * sha256Hex(data)
             * 功能：计算输入数据的 SHA-256 哈希值，并返回十六进制字符串表示。
             * 安全性：SHA-256 是 SHA-2 家族的一员，目前被认为是安全的，广泛用于密码存储、数字签名等场景。
             * 输出长度：64 个字符（256 位，每个字节转换为 2 个十六进制字符）。
             */
            case "sha256" -> DigestUtils.sha256Hex(data);
            /**
             * sha1Hex(data)
             * 功能：计算输入数据的 SHA-1 哈希值，并返回十六进制字符串表示。
             * 安全性：SHA-1 已被证明存在碰撞漏洞（可生成两个不同数据的相同哈希值），不推荐用于密码存储或安全敏感场景。
             * 输出长度：40 个字符（160 位）。
             */
            case "sha1" -> DigestUtils.sha1Hex(data);
            /**
             * md5Hex(data)
             * 功能：计算输入数据的 MD5 哈希值，并返回十六进制字符串表示。
             * 安全性：MD5 已被广泛破解，存在严重的安全漏洞，严禁用于密码存储或安全敏感场景。
             * 输出长度：32 个字符（128 位）。
             */
            case "md5" -> DigestUtils.md5Hex(data);
            default -> throw new IllegalArgumentException("不支持的哈希算法: " + algorithm);
        };
    }



}