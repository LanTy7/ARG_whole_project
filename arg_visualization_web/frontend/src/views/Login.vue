<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">🧬</div>
        <h1>{{ $t('login.title') }}</h1>
        <p>{{ $t('login.subtitle') }}</p>
      </div>
      
      <!-- 语言切换 -->
      <div class="lang-switch-wrapper">
        <LangSwitch />
      </div>
      
      <!-- 标签页切换 -->
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录标签页 -->
        <el-tab-pane :label="$t('login.tabs.login')" name="login">
          <el-form @submit.prevent="() => {}">
            <!-- 邮箱输入框 -->
            <el-form-item>
              <el-input
                v-model="email"
                :placeholder="$t('login.emailPlaceholder')"
                prefix-icon="Message"
                size="large"
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 密码输入框 -->
            <el-form-item>
              <el-input
                v-model="password"
                type="password"
                :placeholder="$t('login.passwordPlaceholder')"
                prefix-icon="Lock"
                size="large"
                show-password
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 验证码输入框和获取验证码按钮 -->
            <el-form-item>
              <div class="code-input-group">
                <el-input
                  v-model="code"
                  :placeholder="$t('login.codePlaceholder')"
                  prefix-icon="Key"
                  size="large"
                  @keydown.enter.prevent
                />
                <el-button
                  native-type="button"
                  :disabled="countdown > 0"
                  size="large"
                  @click.prevent.stop="handleSendCode"
                >
                  {{ countdown > 0 ? $t('login.retryAfter', { seconds: countdown }) : $t('login.getCode') }}
                </el-button>
              </div>
            </el-form-item>
            
            <!-- 登录按钮 -->
            <el-form-item>
              <el-button
                :loading="loading"
                type="primary"
                size="large"
                class="login-button"
                native-type="button"
                @click.prevent="handleLogin"
              >
                {{ $t('login.loginButton') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 注册标签页 -->
        <el-tab-pane :label="$t('login.tabs.register')" name="register">
          <el-form @submit.prevent="() => {}">
            <!-- 用户名输入框 -->
            <el-form-item>
              <el-input
                v-model="registerForm.username"
                :placeholder="$t('login.usernamePlaceholder')"
                prefix-icon="User"
                size="large"
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 邮箱输入框 -->
            <el-form-item>
              <el-input
                v-model="registerForm.email"
                :placeholder="$t('login.emailPlaceholder')"
                prefix-icon="Message"
                size="large"
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 密码输入框 -->
            <el-form-item>
              <el-input
                v-model="registerForm.password"
                type="password"
                :placeholder="$t('login.passwordPlaceholder')"
                prefix-icon="Lock"
                size="large"
                show-password
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 确认密码输入框 -->
            <el-form-item>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                :placeholder="$t('login.confirmPasswordPlaceholder')"
                prefix-icon="Lock"
                size="large"
                show-password
                @keydown.enter.prevent
              />
            </el-form-item>
            
            <!-- 验证码输入框和获取验证码按钮 -->
            <el-form-item>
              <div class="code-input-group">
                <el-input
                  v-model="registerForm.code"
                  :placeholder="$t('login.codePlaceholder')"
                  prefix-icon="Key"
                  size="large"
                  @keydown.enter.prevent
                />
                <el-button
                  native-type="button"
                  :disabled="registerCountdown > 0"
                  size="large"
                  @click.prevent.stop="handleSendRegisterCode"
                >
                  {{ registerCountdown > 0 ? $t('login.retryAfter', { seconds: registerCountdown }) : $t('login.getCode') }}
                </el-button>
              </div>
            </el-form-item>
            
            <!-- 注册按钮 -->
            <el-form-item>
              <el-button
                :loading="registerLoading"
                type="primary"
                size="large"
                class="login-button"
                native-type="button"
                @click.prevent="handleRegister"
              >
                {{ $t('login.registerButton') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/stores/user';
import { login, sendLoginCode, register, sendVerificationCode } from '@/api/auth';
import LangSwitch from '@/components/LangSwitch.vue';

const { t } = useI18n();
const router = useRouter();
const userStore = useUserStore();

// 当前标签页
const activeTab = ref('login');

// 登录表单
const email = ref('');
const password = ref('');
const code = ref('');
const countdown = ref(0);
const loading = ref(false);

// 注册表单
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
});
const registerCountdown = ref(0);
const registerLoading = ref(false);

// 发送验证码
const handleSendCode = (event) => {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  
  if (!email.value) {
    ElMessage.warning(t('login.messages.enterEmail'));
    return false;
  }
  
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    ElMessage.warning(t('login.messages.invalidEmail'));
    return false;
  }
  
  sendLoginCode(email.value)
    .then(() => {
      ElMessage.success(t('login.messages.codeSent'));
      
      // 开始倒计时
      countdown.value = 60;
      const timer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          clearInterval(timer);
        }
      }, 1000);
    })
    .catch((error) => {
      console.error('Failed to send code:', error);
    });
  
  return false;
};

// 登录
const handleLogin = (event) => {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  
  // 验证表单
  if (!email.value) {
    ElMessage.warning(t('login.messages.enterEmail'));
    return false;
  }
  
  if (!password.value) {
    ElMessage.warning(t('login.messages.enterPassword'));
    return false;
  }
  
  if (!code.value) {
    ElMessage.warning(t('login.messages.enterCode'));
    return false;
  }
  
  loading.value = true;
  
  login({
    identifier: email.value,
    password: password.value,
    code: code.value
  })
    .then((res) => {
      // 保存 token 和用户信息
      userStore.setToken(res.data.token);
      userStore.setUserInfo(res.data.userInfo);
      
      ElMessage.success(t('login.messages.loginSuccess'));
      router.push('/');
    })
    .catch((error) => {
      console.error('Login failed:', error);
    })
    .finally(() => {
      loading.value = false;
    });
  
  return false;
};

// 发送注册验证码
const handleSendRegisterCode = (event) => {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  
  if (!registerForm.email) {
    ElMessage.warning(t('login.messages.enterEmail'));
    return false;
  }
  
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.warning(t('login.messages.invalidEmail'));
    return false;
  }
  
  sendVerificationCode(registerForm.email)
    .then(() => {
      ElMessage.success(t('login.messages.codeSent'));
      
      // 开始倒计时
      registerCountdown.value = 60;
      const timer = setInterval(() => {
        registerCountdown.value--;
        if (registerCountdown.value <= 0) {
          clearInterval(timer);
        }
      }, 1000);
    })
    .catch((error) => {
      console.error('Failed to send code:', error);
    });
  
  return false;
};

// 注册
const handleRegister = (event) => {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  
  // 验证表单
  if (!registerForm.username) {
    ElMessage.warning(t('login.messages.enterUsername'));
    return false;
  }
  
  // 验证用户名长度
  if (registerForm.username.length < 3 || registerForm.username.length > 20) {
    ElMessage.warning(t('login.messages.usernameLength'));
    return false;
  }
  
  // 验证用户名格式
  const usernameRegex = /^[a-zA-Z0-9_-]+$/;
  if (!usernameRegex.test(registerForm.username)) {
    ElMessage.warning(t('login.messages.usernameFormat'));
    return false;
  }
  
  if (!registerForm.email) {
    ElMessage.warning(t('login.messages.enterEmail'));
    return false;
  }
  
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.warning(t('login.messages.invalidEmail'));
    return false;
  }
  
  if (!registerForm.password) {
    ElMessage.warning(t('login.messages.enterPassword'));
    return false;
  }
  
  // 验证密码长度
  if (registerForm.password.length < 6 || registerForm.password.length > 20) {
    ElMessage.warning(t('login.messages.passwordLength'));
    return false;
  }
  
  if (!registerForm.confirmPassword) {
    ElMessage.warning(t('login.messages.confirmPasswordRequired'));
    return false;
  }
  
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning(t('login.messages.passwordMismatch'));
    return false;
  }
  
  if (!registerForm.code) {
    ElMessage.warning(t('login.messages.enterCode'));
    return false;
  }
  
  // 验证验证码格式
  const codeRegex = /^\d{6}$/;
  if (!codeRegex.test(registerForm.code)) {
    ElMessage.warning(t('login.messages.codeFormat'));
    return false;
  }
  
  registerLoading.value = true;
  
  register({
    username: registerForm.username,
    email: registerForm.email,
    password: registerForm.password,
    confirmPassword: registerForm.confirmPassword,
    verificationCode: registerForm.code
  })
    .then(() => {
      ElMessage.success(t('login.messages.registerSuccess'));
      // 切换到登录标签页
      activeTab.value = 'login';
      // 清空注册表单
      registerForm.username = '';
      registerForm.email = '';
      registerForm.password = '';
      registerForm.confirmPassword = '';
      registerForm.code = '';
    })
    .catch((error) => {
      console.error('Register failed:', error);
    })
    .finally(() => {
      registerLoading.value = false;
    });
  
  return false;
};
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 450px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 60px;
  margin-bottom: 10px;
}

.login-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #333;
}

.login-header p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.lang-switch-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
}

.lang-switch-wrapper :deep(.lang-trigger) {
  color: #666;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
}

.lang-switch-wrapper :deep(.lang-trigger:hover) {
  background: #e8e8e8;
  color: #333;
}

.lang-switch-wrapper :deep(.globe-icon) {
  color: #666;
}

.login-tabs {
  margin-bottom: 20px;
}

.code-input-group {
  display: flex;
  gap: 10px;
}

.code-input-group .el-input {
  flex: 1;
}

.login-button {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}
</style>
