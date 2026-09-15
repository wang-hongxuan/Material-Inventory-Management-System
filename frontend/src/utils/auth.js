// 文件说明：前端登录状态管理工具。
// 这里负责保存 JWT、恢复登录用户、退出登录和判断管理员权限。
import { reactive } from 'vue';

// 从 localStorage 读取用户信息；解析失败时返回 null，避免页面崩溃。
function readUser() {
  try {
    return JSON.parse(localStorage.getItem('inventory_user') || 'null');
  } catch {
    return null;
  }
}

// 全局登录状态。刷新页面后会从 localStorage 恢复 token 和用户信息。
export const authState = reactive({
  token: localStorage.getItem('inventory_token') || '',
  user: readUser()
});

// 登录或注册成功后保存 token 和用户信息。
export function setAuth(token, user) {
  authState.token = token;
  authState.user = user;
  localStorage.setItem('inventory_token', token);
  localStorage.setItem('inventory_user', JSON.stringify(user));
}

// 退出登录或 token 过期时清空本地登录状态。
export function clearAuth() {
  authState.token = '';
  authState.user = null;
  localStorage.removeItem('inventory_token');
  localStorage.removeItem('inventory_user');
}

// 判断当前用户是否是管理员，路由和菜单会用它控制用户管理入口。
export function isAdmin() {
  return authState.user?.role === 'admin';
}
