// 文件说明：前端 API 统一封装文件。
// 这里体现前后端分离，前端通过 Axios 调用 SpringBoot RESTful 接口，并在请求头携带 JWT。
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { authState, clearAuth } from '../utils/auth';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
const AI_REQUEST_TIMEOUT = 60000;

// 创建统一 Axios 实例，所有业务接口都通过它访问后端 RESTful API。
export const api = axios.create({
  baseURL,
  timeout: 12000
});

// 请求拦截器：如果用户已经登录，就把 JWT 放到 Authorization 请求头。
api.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`;
  }
  return config;
});

// 响应拦截器：统一取 response.data，并集中处理接口错误和登录过期。
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试；AI 助手首次回答可能需要更久'
      : error.response?.data?.message || error.message || '请求失败';
    if (error.response?.status === 401) {
      clearAuth();
      if (location.pathname !== '/login') location.href = '/login';
    }
    ElMessage.error(message);
    return Promise.reject(new Error(message));
  }
);

// 把后端返回的相对图片路径转换为浏览器可访问地址。
export function assetUrl(url) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (baseURL.startsWith('http')) {
    const origin = baseURL.replace(/\/api\/?$/, '');
    return `${origin}${url}`;
  }
  return url;
}

// 登录、注册和当前用户相关接口。
export const authApi = {
  login: (payload) => api.post('/auth/login', payload),
  register: (payload) => api.post('/auth/register', payload),
  me: () => api.get('/auth/me')
};

// 管理员用户管理接口。
export const usersApi = {
  list: (params) => api.get('/users', { params }),
  create: (payload) => api.post('/users', payload),
  update: (id, payload) => api.put(`/users/${id}`, payload),
  remove: (id) => api.delete(`/users/${id}`)
};

// 物资基础信息和物资照片上传接口。
export const materialsApi = {
  list: (params) => api.get('/materials', { params }),
  categories: () => api.get('/materials/categories/options'),
  create: (payload) => api.post('/materials', payload),
  update: (id, payload) => api.put(`/materials/${id}`, payload),
  remove: (id) => api.delete(`/materials/${id}`),
  uploadPhoto: (id, file) => {
    const formData = new FormData();
    formData.append('photo', file);
    return api.post(`/materials/${id}/photo`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};

// 入库记录接口，新增入库后端会自动增加库存。
export const inboundApi = {
  list: (params) => api.get('/inbound', { params }),
  create: (payload) => api.post('/inbound', payload),
  remove: (id) => api.delete(`/inbound/${id}`)
};

// 出库记录接口：管理员直接出库扣库存，操作员提交待审批申请。
export const outboundApi = {
  list: (params) => api.get('/outbound', { params }),
  create: (payload) => api.post('/outbound', payload),
  approve: (id) => api.post(`/outbound/${id}/approve`),
  reject: (id, payload) => api.post(`/outbound/${id}/reject`, payload),
  remove: (id) => api.delete(`/outbound/${id}`)
};

// 报表和图表数据接口。
export const reportsApi = {
  dashboard: () => api.get('/reports/dashboard'),
  movement: (params) => api.get('/reports/movement', { params }),
  stockByCategory: () => api.get('/reports/stock-by-category'),
  lowStock: () => api.get('/reports/low-stock')
};

// AI 助手接口，后端会代为调用智谱 API，避免 API Key 暴露在浏览器。
export const aiApi = {
  chat: (payload) => api.post('/ai/chat', payload, { timeout: AI_REQUEST_TIMEOUT })
};
