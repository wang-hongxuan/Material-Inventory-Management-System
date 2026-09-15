<!--
  文件说明：系统登录后的整体布局壳层。
  这里体现前后端分离系统的主界面结构，包括浅色侧边栏、顶部栏、路由出口、退出登录和 Swagger 文档入口。
-->
<template>
  <router-view v-if="isPublicPage" />
  <el-container v-else class="app-shell">
    <el-aside class="sidebar" width="228px">
      <div class="brand">
        <div class="brand-mark">
          <el-icon><Box /></el-icon>
        </div>
        <div>
          <strong>物资库存管理系统</strong>
          <span>Inventory Desk</span>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="nav-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="side-status">
        <span class="status-dot"></span>
        <div>
          <strong>后端接口在线</strong>
          <small>SpringBoot + MySQL</small>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">
          <h1>{{ route.meta.title || '库存总览' }}</h1>
          <p>{{ today }} · 库存、出入库、报表统一维护</p>
        </div>

        <div class="topbar-actions">
          <el-input
            class="topbar-search"
            :prefix-icon="Search"
            placeholder="搜索物资编号、名称、库位..."
            clearable
          />
          <el-tooltip content="接口文档" placement="bottom">
            <el-button class="icon-button" :icon="Document" circle @click="openDocs" />
          </el-tooltip>
          <el-button class="icon-button notify-button" :icon="Bell" circle />
          <div class="user-chip">
            <div class="avatar">{{ userInitial }}</div>
            <div>
              <strong>{{ authState.user?.name || authState.user?.username || '用户' }}</strong>
              <span>{{ isAdmin() ? '管理员' : '操作员' }}</span>
            </div>
          </div>
          <el-tooltip content="退出登录" placement="bottom">
            <el-button class="icon-button" :icon="SwitchButton" circle @click="logout" />
          </el-tooltip>
        </div>
      </el-header>

      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {
  Bell,
  Box,
  ChatDotRound,
  DataAnalysis,
  Document,
  Goods,
  House,
  Search,
  SwitchButton,
  TopRight,
  User
} from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authState, clearAuth, isAdmin } from './utils/auth';

const route = useRoute();
const router = useRouter();
const isPublicPage = computed(() => route.meta.public);
const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date());

const userInitial = computed(() => {
  const name = authState.user?.name || authState.user?.username || '用';
  return name.slice(0, 1).toUpperCase();
});

const menus = computed(() => {
  const base = [
    { path: '/', label: '工作台', icon: House },
    { path: '/materials', label: '物资信息管理', icon: Goods },
    { path: '/inbound', label: '入库管理', icon: Box },
    { path: '/outbound', label: '出库管理', icon: TopRight },
    { path: '/reports', label: '分析报表', icon: DataAnalysis },
    { path: '/assistant', label: 'AI 助手', icon: ChatDotRound }
  ];
  if (isAdmin()) base.push({ path: '/users', label: '用户管理', icon: User });
  return base;
});

function logout() {
  clearAuth();
  router.push('/login');
}

function openDocs() {
  window.open('http://localhost:3000/api/docs', '_blank');
}
</script>
