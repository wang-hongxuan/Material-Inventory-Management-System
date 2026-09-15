<!--
  文件说明：登录和自助注册页面。
  这里展示现代 SaaS 登录页、自助注册、验证码校验、JWT 登录和注册后自动进入系统。
-->
<template>
  <main class="auth-page">
    <section class="auth-card">
      <aside class="welcome-panel">
        <div class="arc arc-top"></div>
        <div class="arc arc-bottom"></div>

        <div class="welcome-content">
          <div class="cube-icon" aria-hidden="true">
            <span class="cube-top"></span>
            <span class="cube-left"></span>
            <span class="cube-right"></span>
          </div>
          <h1>欢迎回来！</h1>
          <p>
            高效管理库存、入库出库、数据报表<br />
            让物资管理更简单
          </p>
          <el-button class="pill-button" type="primary" @click="switchMode('signin')">SIGN IN</el-button>
        </div>
      </aside>

      <section class="form-panel">
        <transition name="fade" mode="out-in">
          <div v-if="mode === 'signup'" key="signup" class="form-wrap">
            <header class="form-head">
              <h2>创建账号</h2>
              <p>or use email for registration</p>
            </header>

            <div class="plain-form signup-form">
              <el-input v-model.trim="signupForm.name" placeholder="Name" />
              <el-input v-model.trim="signupForm.email" placeholder="Email" />
              <el-input v-model="signupForm.password" placeholder="Password" show-password type="password" />
              <el-input
                v-model="signupForm.confirmPassword"
                placeholder="Confirm Password"
                show-password
                type="password"
              />
              <el-button class="primary-action signup-action" type="primary" :loading="signupLoading" @click="submitSignup">
                SIGN UP
              </el-button>
            </div>

            <p class="switch-link">
              已有账号？
              <button type="button" @click="switchMode('signin')">去登录</button>
            </p>
          </div>

          <div v-else key="signin" class="form-wrap login-wrap">
            <header class="form-head login-title">
              <h2>用户登录</h2>
            </header>

            <div class="login-form">
              <label>
                <span>账号</span>
                <el-input v-model.trim="loginForm.username" placeholder="请输入账号" />
              </label>

              <label>
                <span>密码</span>
                <el-input v-model="loginForm.password" placeholder="请输入密码" show-password type="password" />
              </label>

              <label>
                <span>验证码</span>
                <div class="captcha-row">
                  <el-input v-model.trim="loginForm.captcha" placeholder="请输入验证码" maxlength="4" />
                  <div class="captcha-box">{{ captcha }}</div>
                  <button class="text-button" type="button" @click="refreshCaptcha">换一张</button>
                </div>
              </label>

              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <button class="text-button" type="button" @click="showForgotTip">忘记密码？</button>
              </div>

              <el-button class="primary-action login-action" type="primary" :loading="loading" @click="submitLogin">
                登录
              </el-button>

              <button class="admin-link" type="button" @click="showAdminTip">联系管理员</button>

              <p class="switch-link">
                没有账号？
                <button type="button" @click="switchMode('signup')">创建账号</button>
              </p>
            </div>
          </div>
        </transition>
      </section>
    </section>
  </main>
</template>

<script setup>
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api';
import { setAuth } from '../utils/auth';

const router = useRouter();
const mode = ref('signup');
const loading = ref(false);
const signupLoading = ref(false);
const rememberMe = ref(false);
const captcha = ref('');

const signupForm = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
});

// 切换右侧表单状态：注册表单和登录表单共用同一个登录页。
function switchMode(nextMode) {
  mode.value = nextMode;
  if (nextMode === 'signin') refreshCaptcha();
}

// 随机生成 4 位数字验证码，并清空用户已经输入的验证码。
function refreshCaptcha() {
  captcha.value = String(Math.floor(1000 + Math.random() * 9000));
  loginForm.captcha = '';
}

// 提交自助注册：先做前端校验，再调用后端注册接口，成功后写入 token 并进入系统。
async function submitSignup() {
  if (!signupForm.name) {
    ElMessage.warning('Name 不能为空');
    return;
  }
  if (!signupForm.email) {
    ElMessage.warning('Email 不能为空');
    return;
  }
  if (!signupForm.password) {
    ElMessage.warning('Password 不能为空');
    return;
  }
  if (!signupForm.confirmPassword) {
    ElMessage.warning('Confirm Password 不能为空');
    return;
  }
  if (signupForm.password !== signupForm.confirmPassword) {
    ElMessage.error('两次密码输入不一致');
    return;
  }

  signupLoading.value = true;
  try {
    const { token, user } = await authApi.register({
      name: signupForm.name,
      email: signupForm.email,
      password: signupForm.password
    });
    setAuth(token, user);
    ElMessage.success('注册成功，已自动登录');
    router.push('/');
  } finally {
    signupLoading.value = false;
  }
}

// 提交登录：校验账号、密码和验证码，通过后调用后端登录接口获取 JWT。
async function submitLogin() {
  if (!loginForm.username) {
    ElMessage.warning('账号不能为空');
    return;
  }
  if (!loginForm.password) {
    ElMessage.warning('密码不能为空');
    return;
  }
  if (!loginForm.captcha) {
    ElMessage.warning('验证码不能为空');
    return;
  }
  if (loginForm.captcha !== captcha.value) {
    ElMessage.error('验证码错误');
    refreshCaptcha();
    return;
  }

  loading.value = true;
  try {
    const { token, user } = await authApi.login({
      username: loginForm.username,
      password: loginForm.password
    });
    setAuth(token, user);
    router.push('/');
  } finally {
    loading.value = false;
  }
}

// 忘记密码提示，本系统演示版本由管理员重置密码。
function showForgotTip() {
  ElMessage.info('请联系管理员重置密码');
}

// 演示账号提示，便于验收时快速进入系统。
function showAdminTip() {
  ElMessage.info('管理员账号：admin');
}

// 页面加载时先生成一张验证码。
onMounted(refreshCaptcha);
</script>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 32px;
  background: #eef3f8;
}

.auth-card {
  display: grid;
  width: min(980px, 100%);
  min-height: 600px;
  grid-template-columns: 40% 60%;
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 16px;
  background: #f7fafd;
  box-shadow: 0 18px 45px rgba(38, 55, 87, 0.12);
}

.welcome-panel {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #eaf1f8;
}

.arc {
  position: absolute;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  border: 1px solid rgba(218, 227, 238, 0.78);
  background: rgba(255, 255, 255, 0.34);
}

.arc-top {
  top: -96px;
  right: -72px;
}

.arc-bottom {
  bottom: -110px;
  left: -110px;
}

.welcome-content {
  position: relative;
  z-index: 1;
  width: 72%;
  color: #111827;
}

.cube-icon {
  position: relative;
  width: 48px;
  height: 48px;
  margin-bottom: 24px;
}

.cube-icon span {
  position: absolute;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: #3f6df6;
  box-shadow: 0 10px 24px rgba(63, 109, 246, 0.22);
}

.cube-top {
  top: 1px;
  left: 12px;
  transform: rotate(45deg) skew(-10deg, -10deg);
  opacity: 0.92;
}

.cube-left {
  top: 18px;
  left: 4px;
  transform: skewY(28deg);
  opacity: 0.78;
}

.cube-right {
  top: 18px;
  right: 4px;
  transform: skewY(-28deg);
  opacity: 1;
}

.welcome-content h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 780;
  letter-spacing: 0;
}

.welcome-content p {
  margin: 18px 0 34px;
  color: #6b778c;
  font-size: 15px;
  line-height: 1.8;
}

.pill-button {
  width: 172px;
  height: 44px;
  border: 0;
  border-radius: 999px;
  background: #3f6df6;
  font-weight: 760;
  letter-spacing: 0.5px;
  box-shadow: 0 12px 24px rgba(63, 109, 246, 0.24);
}

.form-panel {
  display: grid;
  place-items: center;
  padding: 42px 58px;
  background: #ffffff;
}

.form-wrap {
  width: 360px;
  max-width: 100%;
}

.form-head {
  margin-bottom: 24px;
  text-align: center;
}

.form-head h2 {
  margin: 0;
  color: #111827;
  font-size: 26px;
  font-weight: 780;
  letter-spacing: 0;
}

.form-head p {
  margin: 14px 0 0;
  color: #98a2b3;
  font-size: 13px;
}

.plain-form {
  display: grid;
  gap: 16px;
}

.signup-form :deep(.el-input__wrapper),
.login-form :deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dbe3ef inset;
}

.signup-form :deep(.el-input__wrapper.is-focus),
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #3f6df6 inset,
    0 0 0 3px rgba(63, 109, 246, 0.1);
}

.signup-action {
  justify-self: center;
  width: 172px;
  margin-top: 22px;
}

.primary-action {
  height: 44px;
  border: 0;
  border-radius: 999px;
  background: #3f6df6;
  font-weight: 760;
  letter-spacing: 0.4px;
  box-shadow: 0 10px 22px rgba(63, 109, 246, 0.22);
}

.login-wrap {
  width: 360px;
}

.login-title {
  margin-bottom: 30px;
}

.login-title h2 {
  font-size: 24px;
}

.login-form {
  display: grid;
  gap: 18px;
}

.login-form label {
  display: grid;
  gap: 9px;
}

.login-form label > span {
  color: #1f2937;
  font-size: 14px;
  font-weight: 650;
}

.login-form :deep(.el-input__wrapper) {
  height: 42px;
  border-radius: 6px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 94px auto;
  align-items: center;
  gap: 12px;
}

.captcha-box {
  display: grid;
  height: 42px;
  place-items: center;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background:
    radial-gradient(circle at 16px 14px, rgba(63, 109, 246, 0.16) 1px, transparent 2px),
    radial-gradient(circle at 58px 26px, rgba(17, 24, 39, 0.12) 1px, transparent 2px),
    #f9fbff;
  color: #254bca;
  font-family: "Segoe UI", Arial, sans-serif;
  font-size: 20px;
  font-weight: 760;
  letter-spacing: 8px;
  text-indent: 8px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -2px;
}

.text-button,
.admin-link,
.switch-link button {
  border: 0;
  background: transparent;
  color: #3f6df6;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.text-button:hover,
.admin-link:hover,
.switch-link button:hover {
  color: #254bca;
}

.login-action {
  width: 100%;
  border-radius: 6px;
}

.admin-link {
  justify-self: center;
  margin-top: 2px;
  font-weight: 650;
}

.switch-link {
  margin: 22px 0 0;
  color: #7b8798;
  font-size: 14px;
  text-align: center;
}

.switch-link button {
  font-weight: 650;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (max-width: 860px) {
  .auth-card {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .welcome-panel {
    min-height: 320px;
  }

  .welcome-content {
    width: 78%;
    text-align: center;
  }

  .cube-icon {
    margin: 0 auto 20px;
  }

  .form-panel {
    padding: 34px 24px 40px;
  }
}

@media (max-width: 520px) {
  .auth-page {
    padding: 16px;
  }

  .captcha-row {
    grid-template-columns: 1fr 88px;
  }

  .captcha-row .text-button {
    justify-self: end;
    grid-column: 2;
  }
}
</style>
