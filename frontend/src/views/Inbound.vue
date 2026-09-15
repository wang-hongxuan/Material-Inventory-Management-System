<!--
  文件说明：入库管理页面。
 这里体现入库登记、入库数量校验、提交后自动增加库存、入库记录表格和入库趋势图。
-->
<template>
  <div class="page">
    <div class="record-layout">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>入库登记</h2>
            <p>提交后自动增加该物资库存</p>
          </div>
          <el-tag type="success" effect="plain">自动更新库存</el-tag>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="物资编号 / 物资名称" prop="materialId">
            <el-select v-model="form.materialId" filterable placeholder="请选择入库物资" style="width: 100%">
              <el-option
                v-for="item in materialOptions"
                :key="item.id"
                :label="`${item.code} - ${item.name}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="物资编号">
              <div class="readonly-field">{{ selectedMaterial?.code || '选择物资后显示' }}</div>
            </el-form-item>
            <el-form-item label="物资名称">
              <div class="readonly-field">{{ selectedMaterial?.name || '选择物资后显示' }}</div>
            </el-form-item>
            <el-form-item label="入库仓库">
              <el-input v-model.trim="form.warehouse" placeholder="如 主仓库" />
            </el-form-item>
            <el-form-item label="入库数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="供应商">
              <el-input v-model.trim="form.supplier" placeholder="可填写供应商" />
            </el-form-item>
            <el-form-item label="经办人">
              <div class="readonly-field">{{ operatorName }}</div>
            </el-form-item>
            <el-form-item label="入库时间">
              <div class="readonly-field">{{ operationTime }}</div>
            </el-form-item>
            <el-form-item label="当前库存">
              <div class="readonly-field">
                {{ selectedMaterial ? `${formatNumber(selectedMaterial.stock)} ${selectedMaterial.unit}` : '选择物资后显示' }}
              </div>
            </el-form-item>
            <el-form-item label="备注" class="full">
              <el-input v-model.trim="form.remark" type="textarea" :rows="3" placeholder="填写入库说明" />
            </el-form-item>
          </div>

          <div class="form-actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :loading="saving" @click="submit">提交入库</el-button>
          </div>
        </el-form>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>入库趋势小图表</h2>
            <p>近 14 天入库数量变化</p>
          </div>
        </div>
        <BaseChart class="compact-chart" :option="trendOption" />
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>入库记录表格</h2>
          <p>支持按物资、来源、经办人和日期查询</p>
        </div>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <div class="toolbar" style="box-shadow: none; margin-bottom: 14px">
        <div class="filter-row">
          <el-input
            v-model.trim="filters.keyword"
            :prefix-icon="Search"
            clearable
            placeholder="物资、仓库、供应商、经办人"
            style="width: 260px"
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="filters.materialId" clearable filterable placeholder="物资" style="width: 260px" @change="search">
            <el-option
              v-for="item in materialOptions"
              :key="item.id"
              :label="`${item.code} - ${item.name}`"
              :value="item.id"
            />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="search"
          />
        </div>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        </div>
      </div>

      <el-table :data="items" v-loading="loading" empty-text="暂无入库记录">
        <el-table-column prop="material_code" label="物资编号" width="112" />
        <el-table-column prop="material_name" label="物资名称" min-width="150" />
        <el-table-column label="入库数量" width="120">
          <template #default="{ row }">{{ formatNumber(row.quantity) }} {{ row.material_unit }}</template>
        </el-table-column>
        <el-table-column prop="source" label="入库仓库 / 供应商" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operator" label="经办人" width="110" />
        <el-table-column label="入库时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="isAdmin()" label="操作" width="90" fixed="right">
          <template #default="{ row }">
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
  </div>
</template>

<script setup>
import { Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { inboundApi, materialsApi, reportsApi } from '../api';
import BaseChart from '../components/BaseChart.vue';
import { authState, isAdmin } from '../utils/auth';
import { formatDateTime, formatNumber } from '../utils/format';

const loading = ref(false);
const saving = ref(false);
const items = ref([]);
const total = ref(0);
const materialOptions = ref([]);
const movement = ref([]);
const formRef = ref(null);
const filters = reactive({
  keyword: '',
  materialId: '',
  dateRange: [],
  page: 1,
  pageSize: 10
});
const form = reactive({
  materialId: '',
  warehouse: '主仓库',
  quantity: 1,
  supplier: '',
  remark: ''
});

const operatorName = computed(() => authState.user?.name || authState.user?.username || '当前用户');
const operationTime = computed(() => formatDateTime(new Date().toLocaleString('sv-SE')));

// 根据表单选择的 materialId 找到完整物资对象，用于显示编号、名称和当前库存。
const selectedMaterial = computed(() => materialOptions.value.find((item) => item.id === form.materialId));

const rules = {
  materialId: [{ required: true, message: '请选择入库物资', trigger: 'change' }],
  quantity: [
    {
      validator: (rule, value, callback) => {
        if (!value || Number(value) <= 0) callback(new Error('入库数量必须大于 0'));
        else callback();
      },
      trigger: 'blur'
    }
  ]
};

// 入库趋势图配置，ECharts 会根据 movement 数据自动刷新图表。
const trendOption = computed(() => ({
  color: ['#22c55e'],
  tooltip: { trigger: 'axis' },
  grid: { left: 38, right: 18, top: 24, bottom: 32 },
  xAxis: {
    type: 'category',
    data: movement.value.map((item) => item.day.slice(5)),
    axisLine: { lineStyle: { color: '#dbe3ef' } },
    axisTick: { show: false },
    axisLabel: { color: '#8a94a6' }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#8a94a6' },
    splitLine: { lineStyle: { color: '#eef2f7' } }
  },
  series: [
    {
      name: '入库',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      lineStyle: { width: 3 },
      areaStyle: { opacity: 0.08 },
      data: movement.value.map((item) => item.inbound)
    }
  ]
}));

// 选择物资后，如果供应商为空，就自动带出物资档案里的供应商。
watch(selectedMaterial, (material) => {
  if (material && !form.supplier) form.supplier = material.supplier || '';
});

// 将筛选条件转换为入库记录查询接口参数。
function params() {
  return {
    keyword: filters.keyword,
    materialId: filters.materialId,
    from: filters.dateRange?.[0],
    to: filters.dateRange?.[1],
    page: filters.page,
    pageSize: filters.pageSize
  };
}

// 将入库仓库和供应商合并为后端入库记录的 source 字段。
function sourceText() {
  return [form.warehouse, form.supplier].filter(Boolean).join(' / ');
}

// 加载物资下拉选项，登记入库时需要选择具体物资。
async function loadMaterials() {
  const data = await materialsApi.list({ page: 1, pageSize: 100 });
  materialOptions.value = data.items || [];
}

// 加载近 14 天出入库趋势，当前页面只取其中的入库数据绘图。
async function loadTrend() {
  const data = await reportsApi.movement({ days: 14 });
  movement.value = data.items || [];
}

// 加载入库记录、物资选项和趋势图数据。
async function loadData() {
  loading.value = true;
  try {
    const [records] = await Promise.all([inboundApi.list(params()), loadMaterials(), loadTrend()]);
    items.value = records.items || [];
    total.value = records.total || 0;
  } finally {
    loading.value = false;
  }
}

// 查询入库记录：回到第一页后重新加载。
function search() {
  filters.page = 1;
  loadData();
}

// 重置入库记录筛选条件。
function resetFilters() {
  Object.assign(filters, {
    keyword: '',
    materialId: '',
    dateRange: [],
    page: 1,
    pageSize: filters.pageSize
  });
  loadData();
}

// 重置入库登记表单。
function resetForm() {
  Object.assign(form, {
    materialId: '',
    warehouse: '主仓库',
    quantity: 1,
    supplier: '',
    remark: ''
  });
  formRef.value?.clearValidate();
}

// 提交入库单：后端会在同一事务中新增入库记录并增加库存。
async function submit() {
  await formRef.value.validate();
  saving.value = true;
  try {
    await inboundApi.create({
      materialId: form.materialId,
      quantity: form.quantity,
      unitPrice: 0,
      source: sourceText(),
      remark: form.remark
    });
    ElMessage.success('入库成功，库存已更新');
    resetForm();
    await loadData();
  } finally {
    saving.value = false;
  }
}

// 删除入库记录：管理员操作，后端会同步回滚该入库数量。
async function remove(row) {
  await ElMessageBox.confirm('删除该入库记录会同步回滚库存，确定继续吗？', '删除确认', { type: 'warning' });
  await inboundApi.remove(row.id);
  ElMessage.success('删除成功');
  loadData();
}

// 页面进入时加载入库相关数据。
onMounted(loadData);
</script>
