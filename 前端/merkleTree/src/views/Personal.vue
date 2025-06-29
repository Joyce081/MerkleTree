<template>
  <div class="personal-container">
    <el-header class="header">
      <div class="logo">MerkleTree可视化平台</div>
      <div class="user-info">
        <el-dropdown>
          <span class="el-dropdown-link">
            <span class="welcome-text">欢迎，{{ userInfo.nickname || userInfo.email }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>个人中心</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <div class="main-content">
      <el-card class="history-card">
        <template #header>
          <div class="card-header">
            <span>历史记录</span>
            <el-button type="primary" size="small" @click="goToHome">返回首页</el-button>
          </div>
        </template>

        <el-table :data="historyList" style="width: 100%" v-loading="loading">
          <el-table-column prop="treeName" label="树名称" width="180" />
          <el-table-column prop="algorithm" label="哈希算法" width="120" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column prop="leafCount" label="叶子节点数" width="120" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="viewTree(row)">查看</el-button>
              <el-button 
                size="small" 
                type="danger" 
                :loading="deletingId === row.id"
                @click="deleteTree(row.id)"
              >
                {{ deletingId === row.id ? '删除中...' : '删除' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

export default {
  name: 'Personal',
  setup() {
    const store = useStore()
    const router = useRouter()
    const historyList = ref([])
    const loading = ref(false)
    const deletingId = ref(null)
    
    // 用户信息
    const userInfo = computed(() => store.state.auth.user || {})
    
    // 加载历史记录
    const loadHistory = async () => {
      loading.value = true
      try {
        const response = await axios.get(`/api/merkle/user/${userInfo.value.id}`)
        if (response.data.code === 200) {
          historyList.value = response.data.data || []
        } else {
          ElMessage.error(response.data.msg || '获取历史记录失败')
        }
      } catch (error) {
        console.error('获取历史记录失败:', error)
        ElMessage.error('获取历史记录失败')
      } finally {
        loading.value = false
      }
    }
    
    // 查看树
    const viewTree = (tree) => {
      try {
        router.push({
          path: '/home',
          query: {
            historyData: JSON.stringify({
              treeId: tree.id,
              treeName: tree.treeName,
              algorithm: tree.algorithm,
              dataItems: tree.originalDataList || []
            })
          }
        })
      } catch (error) {
        console.error('跳转失败:', error)
        ElMessage.error('无法查看该记录')
      }
    }
    
    // 删除树记录
    const deleteTree = async (treeId) => {
      try {
        deletingId.value = treeId
        await ElMessageBox.confirm('确定要删除这条记录吗？删除后不可恢复', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await axios.delete(`/api/merkle/${treeId}`)
        if (response.data.code === 200) {
          ElMessage.success('删除成功')
          await loadHistory() // 刷新列表
        } else {
          ElMessage.error(response.data.msg || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
        }
      } finally {
        deletingId.value = null
      }
    }
    
    // 返回首页
    const goToHome = () => {
      router.push('/home')
    }
    
    // 退出登录
    const handleLogout = () => {
      store.dispatch('logout')
      router.push('/')
      ElMessage.success('已退出登录')
    }
    
    onMounted(() => {
      loadHistory()
    })
    
    return {
      // 数据
      userInfo,
      historyList,
      loading,
      deletingId,
      
      // 方法
      loadHistory,
      viewTree,
      deleteTree,
      goToHome,
      handleLogout
    }
  }
}
</script>

<style scoped>
.personal-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #409eff;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  flex: 1;
  padding: 20px;
}

.history-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>