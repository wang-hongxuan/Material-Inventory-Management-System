<!--
  文件说明：系统用户管理页面。
  这里体现管理员创建账号、编辑用户、启用停用、删除用户和分页查询。
-->
<template>
  <div class="page">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model.trim="filters.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="用户名、姓名"
          style="width: 260px"
          @keyup.enter="search"
          @clear="search"
        />
      </div>
      <div class="toolbar-right">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <section class="panel">
      <el-table :data="items" v-loading="loading" row-key="id" empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="140" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'success' : 'info'" effect="plain">
              {{ row.role === 'admin' ? '管理员' : '操作员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="154" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          :current-page="filters.page"
          :page-size="filters.pageSize"
          @update:current-page="filters.page = $event"
          @update:page-size="filters.pageSize = $event"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :disabled="Boolean(editingId)" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            show-password
            type="password"
            :placeholder="editingId ? '不修改可留空' : '至少 6 位'"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="操作员" value="operator" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { usersApi } from '../api';
import { formatDateTime } from '../utils/format';

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);
const formRef = ref(null);
const items = ref([]);
const total = ref(0);
const filters = reactive({
  keyword: '',
  page: 1,
  pageSize: 10
});
const form = reactive(blankForm());

// 密码校验：新增用户必须填密码，编辑用户时留空表示不修改密码。
function validatePassword(rule, value, callback) {
  if (!editingId.value && !value) callback(new Error('请输入密码'));
  else if (value && value.length < 6) callback(new Error('密码至少需要 6 位'));
  else callback();
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }]
};

// 创建一份空用户表单数据。
function blankForm() {
  return {
    username: '',
    name: '',
    password: '',
    role: 'operator',
    status: 1
  };
}

// 加载用户分页列表。
async function loadData() {
  loading.value = true;
  try {
    const data = await usersApi.list(filters);
    items.value = data.items;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

// 按关键字查询用户。
function search() {
  filters.page = 1;
  loadData();
}

// 重置用户表单和编辑状态。
function resetForm() {
  editingId.value = null;
  Object.assign(form, blankForm());
  formRef.value?.clearValidate();
}

// 打开新增用户弹窗。
function openCreate() {
  resetForm();
  dialogVisible.value = true;
}

// 打开编辑用户弹窗，并回填当前行数据。
function openEdit(row) {
  resetForm();
  editingId.value = row.id;
  Object.assign(form, {
    username: row.username,
    name: row.name,
    password: '',
    role: row.role,
    status: row.status
  });
  dialogVisible.value = true;
}

// 保存用户：editingId 存在时编辑，否则新增。
async function submit() {
  await formRef.value.validate();
  saving.value = true;
  try {
    const payload = { ...form };
    if (editingId.value) await usersApi.update(editingId.value, payload);
    else await usersApi.create(payload);
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

// 删除用户；后端会拦截删除当前登录账号的操作。
async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '删除确认', { type: 'warning' });
  await usersApi.remove(row.id);
  ElMessage.success('删除成功');
  loadData();
}

// 页面进入时加载用户列表。
onMounted(loadData);
</script>
