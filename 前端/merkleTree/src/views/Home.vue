<template>
  <div class="merkle-tree-container">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="logo">MerkleTree可视化平台</div>
      <div class="user-info">
        <el-dropdown>
          <span class="el-dropdown-link">
            <span class="welcome-text">欢迎，{{ userInfo.nickname || userInfo.email }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goToPersonal">个人中心</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 上部构建区域 -->
      <el-card class="build-section">
        <template #header>
          <div class="card-header">
            <span>构建MerkleTree</span>
          </div>
        </template>
        
        <div class="build-content">
          <el-form label-position="top" class="build-form">
            <el-form-item label="输入数据项 (每行一个)">
              <el-input
                v-model="inputData"
                type="textarea"
                :rows="5"
                placeholder="请输入数据项，每行一个"
              />
            </el-form-item>
            
            <div class="form-row">
              <el-form-item label="哈希算法" class="form-item">
                <el-select v-model="hashAlgorithm" placeholder="选择哈希算法">
                  <el-option label="SHA-256" value="sha256" />
                  <el-option label="SHA-1" value="sha1" />
                  <el-option label="MD5" value="md5" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="树名称" class="form-item">
                <el-input v-model="treeName" placeholder="为您的MerkleTree命名" />
              </el-form-item>
              
              <el-button 
                type="primary" 
                @click="generateMerkleTree"
                :loading="isGenerating"
                class="generate-btn"
              >
                生成MerkleTree
              </el-button>
            </div>
          </el-form>
        </div>
      </el-card>

      <!-- 中部可视化区域 -->
      <el-card class="visualization-section">
        <template #header>
          <div class="card-header">
            <span>MerkleTree可视化</span>
            <div class="controls">
              <el-button-group>
                <el-button 
                  size="small" 
                  @click="zoomOut"
                  :disabled="zoomLevel <= 0.5"
                >
                  <el-icon><ZoomOut /></el-icon>
                </el-button>
                <el-button size="small" disabled>
                  {{ Math.round(zoomLevel * 100) }}%
                </el-button>
                <el-button 
                  size="small" 
                  @click="zoomIn"
                  :disabled="zoomLevel >= 2"
                >
                  <el-icon><ZoomIn /></el-icon>
                </el-button>
              </el-button-group>
              <el-button 
                size="small"
                @click="downloadTreeImage"
                v-if="merkleTree"
              >
                下载图片
              </el-button>
            </div>
          </div>
        </template>
        
        <!-- D3.js 树形可视化容器 -->
        <div class="tree-container" ref="treeContainer">
          <svg ref="treeSvg" class="tree-svg"></svg>
          <div v-if="!merkleTree" class="empty-tip">
            <el-empty description="暂无数据，请先输入数据生成MerkleTree" />
          </div>
        </div>

        <!-- 数据修改验证区域 -->
        <div class="data-modification-section">
          <el-form label-position="top">
            <el-form-item label="选择要修改的叶子节点">
              <el-select 
                v-model="selectedLeafIndex" 
                placeholder="选择叶子节点"
                @change="handleLeafSelectionForModification"
                style="width: 100%"
              >
                <el-option
                  v-for="(leaf, index) in leafNodes"
                  :key="index"
                  :label="`节点 ${index + 1}: ${shortenData(leaf.data)}`"
                  :value="index"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="原始数据">
              <el-input 
                v-model="selectedOriginalData" 
                readonly 
                type="textarea" 
                :rows="2"
              />
            </el-form-item>
            
            <el-form-item label="修改为">
              <el-input 
                v-model="modifiedData" 
                type="textarea" 
                :rows="2"
                placeholder="输入修改后的数据"
              />
            </el-form-item>
            
            <el-button 
              type="primary" 
              @click="verifyDataChange"
              :disabled="selectedLeafIndex === null || !modifiedData"
            >
              验证数据变更
            </el-button>
          </el-form>
          
          <!-- 验证结果展示 -->
          <div v-if="changeVerificationResult" class="verification-details">
            <el-alert 
              :title="changeVerificationResult.title"
              :type="changeVerificationResult.isValid ? 'success' : 'error'"
              show-icon
              :closable="false"
            />
            <div class="verification-details-content">
              <div v-if="!changeVerificationResult.isValid">
                <p><strong>变更详情：</strong></p>
                <p>原始哈希: <code>{{ changeVerificationResult.originalHash }}</code></p>
                <p>新哈希: <code>{{ changeVerificationResult.modifiedHash }}</code></p>
                <p>使用的算法: {{ changeVerificationResult.algorithm }}</p>
              </div>
              <div v-else>
                <p>数据验证一致，没有检测到变更</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 整树验证区域 -->
      <el-card class="full-verification-section">
        <template #header>
          <div class="card-header">
            <span>整树验证</span>
          </div>
        </template>
        
        <div class="verification-content">
          <el-button 
            type="primary" 
            @click="verifyEntireTree"
            :disabled="!merkleTree"
            :loading="isVerifyingEntireTree"
          >
            验证整棵树完整性
          </el-button>
          
          <div v-if="entireTreeVerificationResult" class="verification-result">
            <el-alert 
              :title="entireTreeVerificationResult.message"
              :type="entireTreeVerificationResult.isValid ? 'success' : 'error'"
              show-icon
              :closable="false"
            />
            
            <div class="verification-details">
              <p><strong>验证详情：</strong></p>
              <p>存储的根哈希: <code>{{ entireTreeVerificationResult.storedRootHash }}</code></p>
              <p>计算的根哈希: <code>{{ entireTreeVerificationResult.computedRootHash }}</code></p>
              <p>叶子节点数量: {{ entireTreeVerificationResult.leafCount }}</p>
              <p>使用的算法: {{ entireTreeVerificationResult.algorithm }}</p>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import * as d3 from 'd3'
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'Home',
  components: {
    ZoomIn,
    ZoomOut
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    
    // DOM 引用
    const treeContainer = ref(null)
    const treeSvg = ref(null)
    
    // 用户信息
    const userInfo = computed(() => store.state.auth.user || {})
    
    // MerkleTree 构建相关状态
    const inputData = ref('')
    const hashAlgorithm = ref('sha256')
    const treeName = ref('')
    const isGenerating = ref(false)
    const merkleTree = ref(null)
    
    // 可视化相关状态
    const zoomLevel = ref(1)
    const visualizationHeight = ref(500)
    
    // 数据修改验证相关状态
    const selectedLeafIndex = ref(null)
    const selectedOriginalData = ref('')
    const modifiedData = ref('')
    const changeVerificationResult = ref(null)
    
    // 整树验证相关状态
    const isVerifyingEntireTree = ref(false)
    const entireTreeVerificationResult = ref(null)
    
    // 高亮节点数组
    const highlightedNodes = ref([])
    
    // 计算窗口高度
    const calculateHeight = () => {
      const windowHeight = window.innerHeight
      visualizationHeight.value = Math.max(400, windowHeight * 0.5)
    }


    // 初始化
    onMounted(() => {
      calculateHeight()
      window.addEventListener('resize', calculateHeight)
      
      // 从路由参数加载历史数据
      if (router.currentRoute.value.query.historyData) {
        try {
          const historyData = JSON.parse(router.currentRoute.value.query.historyData)
          treeName.value = historyData.treeName
          hashAlgorithm.value = historyData.algorithm
          
          if (historyData.treeId) {
            loadTreeById(historyData.treeId)
          } else if (historyData.dataItems) {
            inputData.value = historyData.dataItems.join('\n')
            generateMerkleTree()
          }
        } catch (e) {
          console.error('加载历史数据失败:', e)
        }
      }
    })
    
    // 计算属性：获取叶子节点
    const leafNodes = computed(() => {
      if (!merkleTree.value) return []
      try {
        const treeData = JSON.parse(merkleTree.value.treeData)
        return treeData.leaves?.map(leaf => ({
          hash: leaf.hash,
          data: leaf.data
        })) || []
      } catch (e) {
        console.error('解析叶子节点失败:', e)
        return []
      }
    })
    // 绘制树
    const d3TreeData = computed(() => {
  if (!merkleTree.value) return null;

  try {
    const treeData = JSON.parse(merkleTree.value.treeData);
    console.log("原始树数据:", treeData);

    // 构建完整的节点映射
    const nodeMap = {};
    
    // 1. 创建所有节点
    treeData.levels.flat().forEach(hash => {
      nodeMap[hash] = nodeMap[hash] || {
        id: hash,
        name: shortenHash(hash),
        isLeaf: treeData.leaves.some(leaf => leaf.hash === hash)
      };
    });

    // 2. 构建父子关系 (从倒数第二层开始向上)
    for (let i = treeData.levels.length - 2; i >= 0; i--) {
      treeData.levels[i].forEach((hash, index) => {
        const children = [];
        const nextLevel = treeData.levels[i + 1];
        
        // 每个父节点对应两个子节点
        const leftChild = nextLevel[index * 2];
        const rightChild = nextLevel[index * 2 + 1];
        
        if (leftChild) children.push(nodeMap[leftChild]);
        if (rightChild) children.push(nodeMap[rightChild]);
        
        nodeMap[hash].children = children.length ? children : undefined;
      });
    }

    // 3. 添加叶子节点数据
    treeData.leaves.forEach(leaf => {
      if (nodeMap[leaf.hash]) {
        nodeMap[leaf.hash].data = leaf.data;
      }
    });

    const rootNode = nodeMap[treeData.root];
    console.log("转换后的D3数据:", JSON.parse(JSON.stringify(rootNode)));
    return rootNode;

  } catch (e) {
    console.error("解析树数据失败:", e);
    return null;
  }
    });
    
    // 方法：缩短哈希显示
    const shortenHash = (hash) => {
      if (!hash) return ''
      return hash.length > 12 ? `${hash.substring(0, 6)}...${hash.substring(hash.length - 6)}` : hash
    }
    
    // 方法：缩短数据显示
    const shortenData = (data) => {
      if (!data) return ''
      return data.length > 20 ? `${data.substring(0, 15)}...` : data
    }
    
    // 方法：根据ID加载树
    const loadTreeById = async (treeId) => {
      isGenerating.value = true
      try {
        const response = await axios.get(`/api/merkle/${treeId}`)
        if (response.data.code === 200) {
          const tree = response.data.data
          inputData.value = tree.originalDataList.join('\n')
          treeName.value = tree.treeName
          hashAlgorithm.value = tree.algorithm
          merkleTree.value = tree
          // 重置选择状态
          selectedLeafIndex.value = null
          selectedOriginalData.value = ''
          modifiedData.value = ''
          changeVerificationResult.value = null
        } else {
          ElMessage.error(response.data.msg || '加载树详情失败')
        }
      } catch (error) {
        console.error('加载树详情失败:', error)
        ElMessage.error('加载树详情失败')
      } finally {
        isGenerating.value = false
      }
    }
    
    // 方法：生成MerkleTree
    const generateMerkleTree = async () => {
      if (!inputData.value.trim()) {
        ElMessage.warning('请输入数据项')
        return
      }
      
      if (!treeName.value.trim()) {
        ElMessage.warning('请输入树名称')
        return
      }
      
      isGenerating.value = true
   
      try {
        const dataItems = inputData.value.trim().split('\n')
          .filter(item => item.trim())
          .map(item => item.trim())

        if (dataItems.length === 0) {
          ElMessage.warning('请输入至少一个有效数据项')
          return
        }

        const response = await axios.post('/api/merkle/create', 
          dataItems,
          {
            params: {
              userId: userInfo.value.id,
              treeName: treeName.value,
              algorithm: hashAlgorithm.value
            }
          }
        )
 
        if (response.data.code === 200) {
          merkleTree.value = response.data.data
          ElMessage.success('MerkleTree生成成功')
          // 重置验证结果
          selectedLeafIndex.value = null
          selectedOriginalData.value = ''
          modifiedData.value = ''
          changeVerificationResult.value = null
          entireTreeVerificationResult.value = null
        } else {
          ElMessage.error(response.data.msg || '生成MerkleTree失败')
        }
      } catch (error) {
        console.error('生成MerkleTree失败:', error)
        let errorMsg = '生成MerkleTree失败'
        if (error.response) {
          errorMsg = error.response.data?.msg || errorMsg
        }
        ElMessage.error(errorMsg)
      } finally {
        isGenerating.value = false
      }
    }
    
    // 方法：绘制树形图
    const drawTree = () => {
      console.groupCollapsed('[drawTree] 开始绘制 Merkle 树');
      
      // 1. 基础检查
      if (!treeSvg.value || !d3TreeData.value) {
        console.error('渲染中止: 缺少 SVG 元素或树数据');
        console.groupEnd();
        return;
      }

      const svg = d3.select(treeSvg.value);
      const width = treeContainer.value.clientWidth;
      const height = visualizationHeight.value;
      const margin = { top: 50, right: 120, bottom: 100, left: 120 };

      // 2. 清空并设置 SVG 基础属性
      svg.selectAll('*').remove();
      svg
        .attr('width', width)
        .attr('height', height)
        .attr('viewBox', [0, 0, width, height])
        .style('background-color', '#f9f9f9');

      // 3. 准备树布局
      const treeLayout = d3.tree()
        .size([width - margin.left - margin.right, height - margin.top - margin.bottom]);

      const root = d3.hierarchy(d3TreeData.value);
      const treeData = treeLayout(root);
      
      console.log('树布局计算结果:', {
        nodes: treeData.descendants(),
        links: treeData.links()
      });

      // 4. 处理单节点情况
      if (treeData.descendants().length <= 1) {
        svg.append('text')
          .attr('x', width / 2)
          .attr('y', height / 2)
          .attr('text-anchor', 'middle')
          .text(treeData.descendants().length === 0 ? '无节点数据' : '仅根节点（检查数据完整性）')
          .attr('fill', '#f56c6c')
          .attr('font-size', '16px');
        
        console.warn('树节点数量不足:', treeData.descendants().length);
        console.groupEnd();
        return;
      }

      // 5. 绘制普通连接线
      svg.append('g')
        .attr('class', 'links')
        .attr('transform', `translate(${margin.left},${margin.top})`)
        .selectAll('path')
        .data(treeData.links())
        .join('path')
        .attr('d', d3.linkVertical()
          .x(d => d.x)
          .y(d => d.y)
        )
        .attr('stroke', '#a0c4ff')
        .attr('stroke-width', 2)
        .attr('stroke-opacity', 0.7)
        .attr('fill', 'none');

    // 5.1 绘制高亮连接路径（完整路径 + 验证路径）
if (highlightedNodes.value.length > 0) {
  const highlightedNodeIds = new Set(highlightedNodes.value.map(n => n.hash));

  // 高亮所有相关连接线（从叶子到根）
  const highlightedLinks = [];
  treeData.descendants().forEach(node => {
    if (node.parent && highlightedNodeIds.has(node.data.id) && highlightedNodeIds.has(node.parent.data.id)) {
      highlightedLinks.push({
        source: node,
        target: node.parent
      });
    }
  });

  // 绘制高亮连接线（红色，加粗）
  svg.append('g')
  .attr('class', 'highlighted-links')
  .attr('transform', `translate(${margin.left},${margin.top})`) // 相同的偏移
  .selectAll('path')
  .data(highlightedLinks)
  .join('path')
  .attr('d', d3.linkVertical().x(d => d.x).y(d => d.y))// 相同的路径生成器
    .attr("stroke", "#ff4d4f")
    .attr("stroke-width", 3)
    .attr("stroke-opacity", 1)
    .attr("fill", "none");
}

      // 6. 绘制节点组
      const nodeGroups = svg.append('g')
        .attr('class', 'nodes')
        .attr('transform', `translate(${margin.left},${margin.top})`)
        .selectAll('g')
        .data(treeData.descendants())
        .join('g')
        .attr('transform', d => `translate(${d.x},${d.y})`)
        .classed('highlighted', d => 
          highlightedNodes.value.some(n => n.hash === d.data.id)
        );

      // 7. 绘制节点圆圈（不同层级不同样式）
      nodeGroups.append('circle')
        .attr('r', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? 12 : 10)
        .attr('fill', d => {
          const isHighlighted = highlightedNodes.value.some(n => n.hash === d.data.id);
          if (isHighlighted) return '#ff7875'; // 高亮节点-红色
          if (d.depth === 0) return '#95de64'; // 根节点-绿色
          return d.data.isLeaf ? '#ffbb96' : '#91d5ff'; // 叶子-橙色，中间-蓝色
        })
        .attr('stroke', d => 
          highlightedNodes.value.some(n => n.hash === d.data.id) ? '#ff4d4f' : '#fff'
        )
        .attr('stroke-width', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? 3 : 2)
        .attr('cursor', 'pointer')
        .on('mouseover', function(event, d) {
          if (!highlightedNodes.value.some(n => n.hash === d.data.id)) {
            d3.select(this).attr('r', 12);
          }
        })
        .on('mouseout', function(event, d) {
          if (!highlightedNodes.value.some(n => n.hash === d.data.id)) {
            d3.select(this).attr('r', 10);
          }
        });

      // 8. 添加节点文本（哈希值）
      nodeGroups.append('text')
        .attr('dy', '0.31em')
        .attr('x', d => d.children ? -15 : 15)
        .attr('text-anchor', d => d.children ? 'end' : 'start')
        .text(d => d.data.name)
        .attr('font-size', '11px')
        .attr('fill', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? '#fff' : '#333')
        .attr('font-weight', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? 'bold' : 'normal')
        .clone(true) // 添加文字描边提升可读性
        .lower()
        .attr('stroke', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? '#ff4d4f' : 'white')
        .attr('stroke-width', 3);

      // 9. 添加叶子节点数据提示
      nodeGroups.filter(d => d.data.isLeaf)
        .append('text')
        .attr('dy', '1.5em')
        .attr('x', 15)
        .attr('text-anchor', 'start')
        .text(d => shortenData(d.data.data || ''))
        .attr('font-size', '10px')
        .attr('fill', d => highlightedNodes.value.some(n => n.hash === d.data.id) ? '#000' : '#666')
        .attr('class', 'leaf-data');

      // 10. 添加箭头标记定义
      svg.append('defs').append('marker')
        .attr('id', 'arrowhead')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 15)
        .attr('refY', 0)
        .attr('orient', 'auto')
        .attr('markerWidth', 6)
        .attr('markerHeight', 6)
        .attr('xoverflow', 'visible')
        .append('path')
        .attr('d', 'M 0,-5 L 10,0 L 0,5')
        .attr('fill', '#ff7875');

      // 11. 添加缩放控制（如果启用）
      if (zoomLevel.value !== 1) {
        treeContainer.value.style.transform = `scale(${zoomLevel.value})`;
        treeContainer.value.style.transformOrigin = 'top center';
      }

      console.log('树绘制完成');
      console.groupEnd();
    };
    
    // 方法：获取验证路径
    const getProofPath = async (treeId, leafData) => {
      try {
        const response = await axios.get('/api/merkle/proof', {
          params: {
            treeId: treeId,
            leafData: leafData
          }
        })
        
        if (response.data.code === 200) {
          return response.data.data
        } else {
          ElMessage.error(response.data.msg || '获取验证路径失败')
          return null
        }
      } catch (error) {
        console.error('获取验证路径失败:', error)
        ElMessage.error('获取验证路径失败')
        return null
      }
    }
    
    // 方法：处理叶子节点选择
    const handleLeafSelectionForModification = async (index) => {
  if (index !== null && leafNodes.value[index]) {
    const leaf = leafNodes.value[index];
    selectedOriginalData.value = leaf.data;
    modifiedData.value = '';
    changeVerificationResult.value = null;

    // 获取验证路径（可能不完整）
    const proofPath = await getProofPath(merkleTree.value.id, leaf.data);
    
    // 1. 找到叶子节点在 D3 树中的对应节点
    const svg = d3.select(treeSvg.value);
    const leafNode = svg.selectAll(".nodes g")
      .filter(d => d.data.id === leaf.hash)
      .data()[0]; // 获取 D3 节点数据

    if (!leafNode) return;

    // 2. 从叶子节点回溯到根节点，记录完整路径
    const fullPathNodes = [];
    let currentNode = leafNode;
    while (currentNode) {
      fullPathNodes.push(currentNode.data.id); // 记录节点哈希
      currentNode = currentNode.parent; // 向上遍历
    }

    // 3. 高亮完整路径（包括验证路径的兄弟节点）
    const nodesToHighlight = [
      ...new Set([...fullPathNodes, ...(proofPath?.map(n => n.hash) || [])])
    ].map(hash => ({ hash }));

    highlightedNodes.value = nodesToHighlight;
  }
};
 
    // 方法：验证数据变更
    const verifyDataChange = async () => {
  if (selectedLeafIndex.value === null || !modifiedData.value) {
    ElMessage.warning('请选择节点并输入修改后的数据')
    return
  }
  
  const leaf = leafNodes.value[selectedLeafIndex.value]
  if (!leaf) {
    ElMessage.warning('选择的节点无效')
    return
  }
  
  try {
    const response = await axios.post('/api/merkle/verify-change', {
      originalData: leaf.data,
      modifiedData: modifiedData.value
    }, {
      params: {
        treeId: merkleTree.value.id
      }
    })
    
    if (response.data.code === 200) {
      changeVerificationResult.value = {
        ...response.data.data,
        title: response.data.data.isChanged ? '数据已变更' : '数据验证一致',
        isValid: !response.data.data.isChanged
      }
      
      // 数据变更警告
      if (response.data.data.isChanged) {
        ElMessage.warning('数据已变更')
   
      } else {
        // 数据验证一致，不需要刷新树
        ElMessage.success('数据验证一致')
      }
    } else {
      ElMessage.error(response.data.msg || '验证失败')
    }
  } catch (error) {
    console.error('验证数据变更失败:', error)
    ElMessage.error('验证数据变更失败')
  }
     }
  // 方法：验证整棵树
const verifyEntireTree = async () => {
  if (!merkleTree.value) {
    ElMessage.warning('请先生成或加载MerkleTree')
    return
  }

  // 确保已选择叶子节点并输入修改数据
  if (selectedLeafIndex.value === null || !modifiedData.value) {
    ElMessage.warning('请先选择节点并输入修改后的数据')
    return
  }

  const leaf = leafNodes.value[selectedLeafIndex.value]
  if (!leaf) {
    ElMessage.warning('选择的节点无效')
    return
  }

  isVerifyingEntireTree.value = true
  console.log("originalData:",leaf.data);
  console.log("modifiedData:",modifiedData.value);
  
  try {
    const response = await axios.post('/api/merkle/verify-tree', {
      originalData: leaf.data,        // 当前选中的叶子节点原始数据
      modifiedData: modifiedData.value // 用户输入的修改后数据
    }, {
      params: {
        treeId: merkleTree.value.id  // 树ID作为查询参数
      }
    })
    
    if (response.data.code === 200) {
      entireTreeVerificationResult.value = response.data.data
      
      // // 如果验证失败，高亮所有叶子节点
      // if (!response.data.data.isValid) {
      //   highlightedNodes.value = leafNodes.value.map(leaf => ({ hash: leaf.hash }))
      // } else {
      //   highlightedNodes.value = []
      // }
      
      ElMessage.success(response.data.data.isValid ? '整树验证通过' : '整树验证失败')
    } else {
      ElMessage.error(response.data.msg || '验证失败')
    }
  } catch (error) {
    console.error('验证整棵树失败:', error)
    ElMessage.error('验证整棵树失败: ' + (error.response?.data?.msg || error.message))
  } finally {
    isVerifyingEntireTree.value = false
  }
}
    // 方法：下载树图片
    const downloadTreeImage = async () => {
      if (!treeSvg.value) return
      
      try {
        const svgData = new XMLSerializer().serializeToString(treeSvg.value)
        const canvas = document.createElement("canvas")
        const ctx = canvas.getContext("2d")
        
        const img = new Image()
        img.src = "data:image/svg+xml;base64," + btoa(unescape(encodeURIComponent(svgData)))
        
        await new Promise((resolve) => {
          img.onload = resolve
        })
        
        canvas.width = img.width
        canvas.height = img.height
        ctx.drawImage(img, 0, 0)
        
        const link = document.createElement("a")
        link.download = `merkle-tree-${Date.now()}.png`
        link.href = canvas.toDataURL()
        link.click()
      } catch (error) {
        console.error('下载图片失败:', error)
        ElMessage.error('下载图片失败')
      }
    }
    
    // 方法：缩放控制
    const zoomIn = () => {
      if (zoomLevel.value < 2) {
        zoomLevel.value += 0.1
        updateZoom()
      }
    }
    
    const zoomOut = () => {
      if (zoomLevel.value > 0.5) {
        zoomLevel.value -= 0.1
        updateZoom()
      }
    }
    
    const updateZoom = () => {
      if (treeContainer.value) {
        treeContainer.value.style.transform = `scale(${zoomLevel.value})`
        treeContainer.value.style.transformOrigin = 'top center'
      }
    }
    
    // 方法：导航
    const goToPersonal = () => {
      router.push('/personal')
    }
    
    const handleLogout = () => {
      store.dispatch('logout')
      router.push('/')
      ElMessage.success('已退出登录')
    }
    
    // 监听树数据变化，重新绘制
    watch([d3TreeData, highlightedNodes], () => {
      nextTick(() => {
        drawTree()
      })
    })
    
    return {
      // DOM 引用
      treeContainer,
      treeSvg,
      
      // 状态
      userInfo,
      inputData,
      hashAlgorithm,
      treeName,
      isGenerating,
      merkleTree,
      zoomLevel,
      visualizationHeight,
      selectedLeafIndex,
      selectedOriginalData,
      modifiedData,
      changeVerificationResult,
      isVerifyingEntireTree,
      entireTreeVerificationResult,
      leafNodes,
      
      // 方法
      generateMerkleTree,
      handleLeafSelectionForModification,
      verifyDataChange,
      verifyEntireTree,
      shortenHash,
      shortenData,
      downloadTreeImage,
      zoomIn,
      zoomOut,
      goToPersonal,
      handleLogout
    }
  }
}
</script>

<style scoped>
/* 基础布局样式 */
.merkle-tree-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #409eff;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.welcome-text {
  color: white;
  font-size: 16px;
  margin-left: 10px;
}

.main-content {
  display: block;
  padding: 20px;
  overflow-y: auto;
}

/* 构建区域样式 */
.build-section {
  margin-bottom: 20px;
}

.build-content {
  display: flex;
  flex-direction: column;
}

.build-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-row {
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

.form-item {
  flex: 1;
  margin-bottom: 0;
}

.generate-btn {
  flex-shrink: 0;
  margin-left: auto;
}

/* 可视化区域样式 */
.visualization-section {
  margin-bottom: 20px;
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.controls {
  display: flex;
  gap: 10px;
}

.tree-container {
  width: 100%;
  height: 500px;
  position: relative;
  overflow: auto;
  background-color: #f9f9f9;
  border-radius: 4px;
  transition: transform 0.2s ease;
}

.tree-svg {
  font-family: sans-serif;
}

.links path {
  transition: stroke-opacity 0.2s;
}

.links path:hover {
  stroke-opacity: 1;
  stroke-width: 3px;
}

.nodes circle.highlighted {
  stroke: #ff7875 !important;
  filter: drop-shadow(0 0 3px rgba(255, 120, 117, 0.7));
}
.empty-tip {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.8);
}

/* 高亮节点样式 */
.tree-node.highlighted circle {
  stroke: #ff7875 !important;
  stroke-width: 3px !important;
  filter: drop-shadow(0 0 5px rgba(255, 120, 117, 0.7));
}

.tree-node.highlighted text {
  font-weight: bold;
  fill: #ff7875;
}

/* 数据修改区域样式 */
.data-modification-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.verification-details {
  margin-top: 15px;
  padding: 15px;
  background-color: #f0f0f0;
  border-radius: 4px;
}

.verification-details-content {
  margin-top: 10px;
}

/* 整树验证区域样式 */
.full-verification-section {
  margin-top: 20px;
}

.verification-result {
  margin-top: 15px;
}

code {
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}

/* 滚动条样式 */
.tree-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.tree-container::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.2);
}
/* 高亮节点 */
.nodes circle.highlighted {
  fill: #ff7875 !important;
  stroke: #ff4d4f !important;
  stroke-width: 3px !important;
  r: 12px !important;
  filter: drop-shadow(0 0 5px rgba(255, 77, 79, 0.7));
}

/* 高亮连接线 */
.highlighted-links path {
  stroke: #ff4d4f !important;
  stroke-width: 3px !important;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { stroke-width: 3px; }
  50% { stroke-width: 5px; }
  100% { stroke-width: 3px; }
}
</style>