<template>
  <div class="login-register-container">
    <!-- 背景图片 -->
    <div class="background-image"></div>

    <!-- 登录注册卡片 -->
    <div class="card">
      <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <h2 class="title">欢迎来到MerkleTree可视化平台</h2>
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="0">
            <!-- QQ 邮箱 -->
            <el-form-item prop="email">
              <el-input
                v-model="loginForm.email"
                placeholder="QQ 邮箱"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 密码 -->
            <el-form-item prop="password">
              <el-input
                type="password"
                v-model="loginForm.password"
                placeholder="密码"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 登录按钮 -->
            <el-form-item>
              <el-button type="primary" class="submit-button" @click="handleLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <h2 class="title">注册MerkleTree可视化平台</h2>
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="0">
            <!-- 用户名 -->
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- QQ 邮箱 -->
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="QQ 邮箱"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 验证码 -->
            <el-form-item prop="code">
              <el-input
                v-model="registerForm.code"
                placeholder="验证码"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
                <template #append>
                  <el-button @click="sendCode" :disabled="isCodeSent">
                    {{ isCodeSent ? `${countdown}秒后重试` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <!-- 密码 -->
            <el-form-item prop="password">
              <el-input
                type="password"
                v-model="registerForm.password"
                placeholder="密码"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 确认密码 -->
            <el-form-item prop="confirmPassword">
              <el-input
                type="password"
                v-model="registerForm.confirmPassword"
                placeholder="确认密码"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 注册按钮 -->
            <el-form-item>
              <el-button type="primary" class="submit-button" @click="handleRegister">注册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'LoginRegister',
  components: {
    User,
    Lock,
    Message,
    Key,
  },
  setup() {
    const store = useStore();
    const router = useRouter();
    const activeTab = ref('login');
    const isCodeSent = ref(false);
    const countdown = ref(60);

    // 登录表单数据
    const loginForm = ref({
      email: '',
      password: '',
    });

    // 注册表单数据
    const registerForm = ref({
      username: '',
      email: '',
      code: '',
      password: '',
      confirmPassword: '',
    });

    // 登录表单验证规则
    const loginRules = ref({
      email: [
        { required: true, message: '请输入 QQ 邮箱', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            const emailRegex = /^[a-zA-Z0-9_.-]+@qq\.com$/;
            if (!emailRegex.test(value)) {
              callback(new Error('请输入有效的 QQ 邮箱'));
            } else {
              callback();
            }
          },
          trigger: 'blur',
        },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
      ],
    });

    // 注册表单验证规则
    const registerRules = ref({
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
      ],
      email: [
        { required: true, message: '请输入 QQ 邮箱', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            const emailRegex = /^[a-zA-Z0-9_.-]+@qq\.com$/;
            if (!emailRegex.test(value)) {
              callback(new Error('请输入有效的 QQ 邮箱'));
            } else {
              callback();
            }
          },
          trigger: 'blur',
        },
      ],
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
      ],
      confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== registerForm.value.password) {
              callback(new Error('两次输入密码不一致!'));
            } else {
              callback();
            }
          },
          trigger: 'blur',
        },
      ],
    });
const startCountdown = () => {
  isCodeSent.value = true;
  countdown.value = 60;
  
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
      isCodeSent.value = false;
      countdown.value = 60;
    }
  }, 1000);
};

const resetCountdown = () => {
  isCodeSent.value = false;
  countdown.value = 60;
};

// 修改sendCode函数
const sendCode = async () => {
  const email = registerForm.value.email;
  
  // 验证邮箱格式
  const emailRegex = /^[a-zA-Z0-9_.-]+@qq\.com$/;
  if (!emailRegex.test(email)) {
    ElMessage.warning('请输入有效的QQ邮箱');
    return;
  }

  try {
    // 开始倒计时
    startCountdown();
    
    const response = await axios.post('/user/sendVerifyCode', {
      email: email
    });

    if (response.data.code === 200) {
      ElMessage.success('验证码已发送至您的邮箱');
    } else {
      ElMessage.error(response.data.message || '发送验证码失败');
      resetCountdown();
    }
  } catch (error) {
    console.error('发送验证码失败:', error);
    resetCountdown();
    let errorMsg = '发送验证码失败';
    if (error.response) {
      errorMsg = error.response.data?.message || errorMsg;
    }
    ElMessage.error(errorMsg);
  }
};

// 处理注册
const handleRegister = async () => {
  try {
    // 验证表单
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致');
      return;
    }

    const response = await axios.post(
      '/user/register',
      {
        username: registerForm.value.username,
        email: registerForm.value.email,
        password: registerForm.value.password,
        verifyCode: registerForm.value.code
      }
    );

    if (response.data.code === 200) {
      ElMessage.success('注册成功！请登录');
      // 清空表单
      registerForm.value = {
        username: '',
        email: '',
        code: '',
        password: '',
        confirmPassword: '',
      };
      // 切换到登录标签页
      activeTab.value = 'login';
    } else {
      ElMessage.error(response.data.message || '注册失败');
    }
  } catch (error) {
    console.error('注册失败:', error);
    let errorMsg = '注册失败';
    if (error.response) {
      if (error.response.status === 409) {
        errorMsg = '邮箱已被注册';
      } else if (error.response.status === 400) {
        errorMsg = error.response.data?.message || '验证码错误';
      } else {
        errorMsg = error.response.data?.message || errorMsg;
      }
    }
    ElMessage.error(errorMsg);
  }
};

// 处理登录
const handleLogin = async () => {
  try {
    const response = await axios.post('/user/login', {
      email: loginForm.value.email,
      password: loginForm.value.password
    });
    
    if (response.data.code === 200) {
      const user = response.data.data;
      
      // 存储用户信息和token
      await store.dispatch('login', { 
        user: {
          id: user.id,
          nickname: user.username,
          email: user.email
        },
        token: 'mock-token-' + user.id // 假设后端返回的token在这里
      });
      
      ElMessage.success('登录成功');
      // 确保跳转前token已存储
      await new Promise(resolve => setTimeout(resolve, 100));

      // 跳转到home页面
      router.push('/home');
    } else {
      ElMessage.error(response.data.message || '登录失败');
    }
  } catch (error) {
    console.error('登录失败:', error);
    let errorMsg = '登录失败';
    if (error.response) {
      if (error.response.status === 401) {
        errorMsg = '用户名或密码错误';
      } else {
        errorMsg = error.response.data?.message || errorMsg;
      }
    }
    ElMessage.error(errorMsg);
  }
};

    return {
      activeTab,
      loginForm,
      registerForm,
      loginRules,
      registerRules,
      isCodeSent,
      countdown,
      sendCode,
      handleLogin,
      handleRegister,
    };
  },
};
</script>

<style scoped>
.login-register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
}

.background-image {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('@/assets/login3.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(2px);
  z-index: -1;
}

.card {
  background: rgba(255, 255, 255, 0.9);
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  width: 400px;
  text-align: center;
  z-index: 1;
}

.title {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.submit-button {
  width: 100%;
  margin-top: 15px;
  height: 40px;
  font-size: 16px;
}

.el-tabs {
  margin-top: 20px;
  font-size: 16px;
}

.el-form-item {
  margin-bottom: 20px;
}

/* 自定义输入框样式 */
.custom-input ::v-deep .el-input__inner {
  height: 40px;
  font-size: 16px;
  padding: 0 12px;
}

/* 输入框图标样式 */
.custom-input ::v-deep .el-input__prefix {
  display: flex;
  align-items: center;
  padding-left: 8px;
}
/* 自定义输入框样式 - 包含背景颜色修改 */
.custom-input ::v-deep .el-input__wrapper {
    background-color:  rgba(255, 255, 255, 0.5)!important;
  }
.custom-input ::v-deep .el-input__inner {
  height: 40px;
  font-size: 16px;
  padding: 0 12px;
  background-color: rgba(255, 255, 255, 0);
  border-radius: 4px;
}

.custom-input ::v-deep  .el-input-group__append, .el-input-group__prepend {
    align-items: center;
    background-color: #7d96bc;
    color:white;
    border-radius: 4px;
    }
  
    /* 登录/注册按钮样式 */
.submit-button {
  width: 100%;
  margin-top: 15px;
  height: 40px;
  font-size: 16px;
  background-color: #7d96bc;
  border:1px solid #7d96bc;
}

/* 按钮悬停时的字体颜色 */
.submit-button:hover {
  background-color: #578ad8;
  transition: color 0.3s ease;
}

::v-deep .el-tabs__item {
  color: #a0a6b0!important;
}
/* 标签页悬停时的字体颜色 */
::v-deep .el-tabs__item:hover {
  color: #5499ff!important;
  transition: color 0.3s ease;
}

/* 标签页激活状态的字体颜色 */
::v-deep .el-tabs__item.is-active {
  color: #5499ff!important;
  border-bottom-color:#7d96bc!important;
}

/* 标签页激活状态的下划线颜色 */
::v-deep .el-tabs__active-bar {
  background-color: #7d96bc;
  height: 2px; /* 下划线高度 */
  transition: background-color 0.3s ease; /* 下划线颜色过渡效果 */
}
</style>