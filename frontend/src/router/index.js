// 文件说明：前端路由配置和登录权限守卫。
// 这里体现登录后才能访问业务页面，管理员才能访问用户管理页面。
import { createRouter, createWebHistory } from 'vue-router';
import { authState, isAdmin } from '../utils/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '库存总览' }
  },
  {
    path: '/materials',
    name: 'materials',
    component: () => import('../views/Materials.vue'),
    meta: { title: '物资管理' }
  },
  {
    path: '/inbound',
    name: 'inbound',
    component: () => import('../views/Inbound.vue'),
    meta: { title: '入库管理' }
  },
  {
    path: '/outbound',
    name: 'outbound',
    component: () => import('../views/Outbound.vue'),
    meta: { title: '出库管理' }
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('../views/Reports.vue'),
    meta: { title: '库存报表' }
  },
  {
    path: '/assistant',
    name: 'assistant',
    component: () => import('../views/AiAssistant.vue'),
    meta: { title: 'AI 助手' }
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('../views/Users.vue'),
    meta: { title: '用户管理', admin: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (!to.meta.public && !authState.token) return '/login';
  if (to.meta.admin && !isAdmin()) return '/';
  if (to.path === '/login' && authState.token) return '/';
  return true;
});

export default router;
