<!--
  文件说明：物资信息管理页面。
  这里体现物资唯一编号、照片上传、分页查询、条件筛选、库存状态标签和右侧抽屉新增编辑。
-->
<template>
  <div class="page">
    <section class="toolbar">
      <div class="filter-row">
        <el-input
          v-model.trim="filters.code"
          :prefix-icon="Search"
          clearable
          placeholder="物资编号"
          style="width: 180px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-input
          v-model.trim="filters.name"
          clearable
          placeholder="物资名称"
          style="width: 180px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.category" clearable placeholder="物资分类" style="width: 160px" @change="search">
          <el-option v-for="item in allCategories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="filters.status" placeholder="库存状态" style="width: 150px" @change="search">
          <el-option label="全部状态" value="" />
          <el-option label="正常" value="normal" />
          <el-option label="低库存" value="low" />
          <el-option label="库存不足" value="out" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增物资</el-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>物资信息管理</h2>
          <p>维护物资基础信息、照片、安全库存与供应商信息</p>
        </div>
        <el-tag effect="plain">共 {{ total }} 项</el-tag>
      </div>

      <el-table :data="displayItems" v-loading="loading" row-key="id" empty-text="暂无物资数据">
        <el-table-column prop="code" label="物资编号" width="112" fixed="left" />
        <el-table-column label="物资图片" width="94">
          <template #default="{ row }">
            <el-image
              v-if="row.photo_url"
              class="table-image"
              :src="assetUrl(row.photo_url)"
              fit="cover"
              :preview-src-list="[assetUrl(row.photo_url)]"
              preview-teleported
            />
            <span v-else class="empty-image">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="物资名称" min-width="140" />
        <el-table-column prop="category" label="分类" width="112" />
        <el-table-column prop="spec" label="规格型号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="76" />
        <el-table-column label="当前库存" width="110">
          <template #default="{ row }">{{ formatNumber(row.stock) }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="safety_stock" label="安全库存" width="104">
          <template #default="{ row }">{{ formatNumber(row.safety_stock) }}</template>
        </el-table-column>
        <el-table-column label="库存状态" width="112">
          <template #default="{ row }">
            <el-tag :type="stockStatus(row)" effect="light">{{ stockLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.updated_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="126" fixed="right">
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

    <el-drawer
      v-model="drawerVisible"
      :title="editingId ? '编辑物资' : '新增物资'"
      size="430px"
      class="material-drawer"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="物资编号" prop="code">
          <el-input v-model.trim="form.code" placeholder="唯一编号，如 MC-10001" />
        </el-form-item>
        <el-form-item label="物资名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入物资名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" allow-create filterable default-first-option style="width: 100%">
            <el-option v-for="item in allCategories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="规格型号">
            <el-input v-model.trim="form.spec" placeholder="如 M6、10x20x1" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model.trim="form.unit" placeholder="个、件、台" />
          </el-form-item>
        </div>
        <el-form-item label="供应商">
          <el-input v-model.trim="form.supplier" placeholder="请输入供应商" />
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model.trim="form.location" placeholder="如 A-01-01" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="安全库存">
            <el-input-number v-model="form.safetyStock" :min="0" :precision="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="当前库存">
            <div class="readonly-field">{{ formatNumber(form.stock) }} {{ form.unit || '' }}</div>
          </el-form-item>
        </div>
        <el-form-item label="物资照片上传">
          <div>
            <el-upload
              :file-list="photoList"
              @update:file-list="syncPhotoList"
              list-type="picture-card"
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              :on-change="handlePhotoChange"
              :on-remove="handlePhotoRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <p class="form-tip">支持 JPG、PNG、WEBP 等图片；保存后自动上传。库存数量通过入库、出库自动计算。</p>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model.trim="form.remark" type="textarea" :rows="3" placeholder="可填写维护说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="form-actions">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { assetUrl, materialsApi } from '../api';
import { formatDateTime, formatNumber, stockLabel, stockStatus } from '../utils/format';

const defaultCategories = ['办公耗材', '防护用品', '电子设备', '维修备件', '实验用品'];
const categories = ref([]);
const allCategories = computed(() => Array.from(new Set([...defaultCategories, ...categories.value])));
const loading = ref(false);
const saving = ref(false);
const items = ref([]);
const total = ref(0);
const filters = reactive({
  code: '',
  name: '',
  category: '',
  status: '',
  page: 1,
  pageSize: 10
});

const drawerVisible = ref(false);
const editingId = ref(null);
const formRef = ref(null);
const photoList = ref([]);
const selectedPhoto = ref(null);
const form = reactive(blankForm());
const rules = {
  code: [{ required: true, message: '请输入物资编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入物资名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
};

// 表格实际展示数据：后端负责主要筛选，库存状态在当前页再细分为正常、低库存、库存不足。
const displayItems = computed(() => {
  if (!filters.status) return items.value;
  return items.value.filter((row) => {
    const label = stockLabel(row);
    if (filters.status === 'normal') return label === '正常';
    if (filters.status === 'low') return label === '低库存';
    if (filters.status === 'out') return label === '库存不足';
    return true;
  });
});

// 创建一份空表单数据，新增和重置时都用它恢复初始状态。
function blankForm() {
  return {
    code: '',
    name: '',
    category: '',
    spec: '',
    unit: '',
    supplier: '',
    location: '',
    safetyStock: 0,
    stock: 0,
    remark: ''
  };
}

// 将页面筛选条件转换为后端 materials 接口需要的查询参数。
function requestParams() {
  const keyword = filters.code || filters.name;
  return {
    keyword,
    category: filters.category,
    lowStock: ['low', 'out'].includes(filters.status),
    page: filters.page,
    pageSize: filters.pageSize
  };
}

// 将抽屉表单转换为后端新增/编辑物资接口需要的请求体。
function toPayload() {
  return {
    code: form.code,
    name: form.name,
    category: form.category,
    spec: form.spec,
    unit: form.unit,
    supplier: form.supplier,
    location: form.location,
    safetyStock: Number(form.safetyStock || 0),
    remark: form.remark
  };
}

// 加载物资分类选项，用于筛选区和抽屉中的分类下拉框。
async function loadCategories() {
  const data = await materialsApi.categories();
  categories.value = data.items || [];
}

// 加载物资分页列表，并同步刷新分类选项。
async function loadData() {
  loading.value = true;
  try {
    const data = await materialsApi.list(requestParams());
    items.value = data.items || [];
    total.value = data.total || 0;
    await loadCategories();
  } finally {
    loading.value = false;
  }
}

// 查询按钮：重置页码到第一页后重新加载数据。
function search() {
  filters.page = 1;
  loadData();
}

// 清空筛选条件，并重新加载完整物资列表。
function resetFilters() {
  Object.assign(filters, {
    code: '',
    name: '',
    category: '',
    status: '',
    page: 1,
    pageSize: filters.pageSize
  });
  loadData();
}

// 重置抽屉表单，同时清空编辑状态和待上传照片。
function resetForm() {
  Object.assign(form, blankForm());
  editingId.value = null;
  photoList.value = [];
  selectedPhoto.value = null;
  formRef.value?.clearValidate();
}

// 打开新增物资抽屉。
function openCreate() {
  resetForm();
  drawerVisible.value = true;
}

// 打开编辑物资抽屉，并把当前行数据回填到表单。
function openEdit(row) {
  resetForm();
  editingId.value = row.id;
  Object.assign(form, {
    code: row.code,
    name: row.name,
    category: row.category,
    spec: row.spec,
    unit: row.unit,
    supplier: row.supplier,
    location: row.location,
    safetyStock: Number(row.safety_stock || 0),
    stock: Number(row.stock || 0),
    remark: row.remark || ''
  });
  if (row.photo_url) {
    photoList.value = [{ name: '当前照片', url: assetUrl(row.photo_url) }];
  }
  drawerVisible.value = true;
}

// 选择照片时只保留最后一张，真正上传发生在点击保存之后。
function handlePhotoChange(file, files) {
  selectedPhoto.value = file;
  photoList.value = files.slice(-1);
}

// 移除待上传照片。
function handlePhotoRemove() {
  selectedPhoto.value = null;
  photoList.value = [];
}

// 保存物资：先保存基础信息，再上传照片，最后刷新表格。
async function submit() {
  await formRef.value.validate();
  saving.value = true;
  try {
    const saved = editingId.value
      ? await materialsApi.update(editingId.value, toPayload())
      : await materialsApi.create(toPayload());
    if (selectedPhoto.value?.raw) {
      await materialsApi.uploadPhoto(saved.id, selectedPhoto.value.raw);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

// 删除物资：已有出入库记录的物资会被后端拦截，避免破坏历史流水。
async function remove(row) {
  await ElMessageBox.confirm(`确定删除物资「${row.name}」吗？已有出入库记录的物资会被系统拦截。`, '删除确认', {
    type: 'warning'
  });
  await materialsApi.remove(row.id);
  ElMessage.success('删除成功');
  loadData();
}

// 页面进入时自动加载物资数据。
onMounted(loadData);
</script>
