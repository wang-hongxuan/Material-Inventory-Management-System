<!--
  文件说明：库存报表和统计分析页面。
  这里体现通过出入库数据统计库存报表、生成 ECharts 图表，并支持导出 CSV。
-->
<template>
  <div class="page" v-loading="loading">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-segmented v-model="days" :options="[7, 14, 30, 60]" @change="loadMovement" />
        <el-button :icon="Refresh" @click="loadAll">刷新报表</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Download" @click="exportCsv">导出库存 CSV</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <span>物资种类</span>
        <strong>{{ report.summary.materialCount }}</strong>
        <small>基础信息档案</small>
      </div>
      <div class="stat-card">
        <span>库存总量</span>
        <strong>{{ formatNumber(report.summary.totalStock) }}</strong>
        <small>当前库存合计</small>
      </div>
      <div class="stat-card">
        <span>低库存</span>
        <strong>{{ report.summary.lowStockCount }}</strong>
        <small>需要关注补货</small>
      </div>
      <div class="stat-card">
        <span>今日入库</span>
        <strong>{{ formatNumber(report.summary.todayInbound) }}</strong>
        <small>入库记录汇总</small>
      </div>
      <div class="stat-card">
        <span>今日出库</span>
        <strong>{{ formatNumber(report.summary.todayOutbound) }}</strong>
        <small>出库记录汇总</small>
      </div>
    </div>

    <div class="chart-grid">
      <section class="panel">
        <div class="panel-title">
          <h2>出入库数量趋势</h2>
        </div>
        <BaseChart :option="movementOption" />
      </section>
      <section class="panel">
        <div class="panel-title">
          <h2>分类库存统计</h2>
        </div>
        <BaseChart :option="categoryOption" />
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">
        <h2>库存报表明细</h2>
        <el-tag effect="plain">{{ inventory.length }} 项</el-tag>
      </div>
      <el-table :data="inventory" height="360" empty-text="暂无库存数据">
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="spec" label="规格" min-width="150" />
        <el-table-column label="库存" width="120">
          <template #default="{ row }">{{ formatNumber(row.stock) }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="safety_stock" label="安全库存" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="stockStatus(row)" effect="light">{{ stockLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="库位" min-width="120" />
        <el-table-column prop="supplier" label="供应商" min-width="130" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { Download, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { materialsApi, reportsApi } from '../api';
import BaseChart from '../components/BaseChart.vue';
import { formatNumber, stockLabel, stockStatus } from '../utils/format';

const loading = ref(false);
const days = ref(14);
const inventory = ref([]);
const report = reactive({
  summary: {
    materialCount: 0,
    totalStock: 0,
    lowStockCount: 0,
    todayInbound: 0,
    todayOutbound: 0
  },
  stockByCategory: [],
  movement: []
});

// 出入库数量趋势图配置。
const movementOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 38, right: 18, top: 48, bottom: 30 },
  xAxis: { type: 'category', data: report.movement.map((item) => item.day.slice(5)) },
  yAxis: { type: 'value' },
  series: [
    {
      name: '入库',
      type: 'bar',
      stack: 'movement',
      itemStyle: { color: '#10b981' },
      data: report.movement.map((item) => item.inbound)
    },
    {
      name: '出库',
      type: 'bar',
      stack: 'movement',
      itemStyle: { color: '#f59e0b' },
      data: report.movement.map((item) => item.outbound)
    }
  ]
}));

// 分类库存统计图配置。
const categoryOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 80, right: 18, top: 24, bottom: 30 },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    data: report.stockByCategory.map((item) => item.category).reverse()
  },
  series: [
    {
      name: '库存',
      type: 'bar',
      itemStyle: { color: '#2563eb', borderRadius: [0, 4, 4, 0] },
      data: report.stockByCategory.map((item) => item.stock).reverse()
    }
  ]
}));

// 加载报表汇总数据，包括概览、分类库存和默认趋势。
async function loadDashboard() {
  const data = await reportsApi.dashboard();
  Object.assign(report.summary, data.summary);
  report.stockByCategory = data.stockByCategory;
  report.movement = data.movement;
}

// 根据当前 days 选项重新加载出入库趋势。
async function loadMovement() {
  const data = await reportsApi.movement({ days: days.value });
  report.movement = data.items;
}

// 加载库存明细表，导出 CSV 也使用这份数据。
async function loadInventory() {
  const data = await materialsApi.list({ page: 1, pageSize: 100 });
  inventory.value = data.items;
}

// 加载报表页全部数据。
async function loadAll() {
  loading.value = true;
  try {
    await Promise.all([loadDashboard(), loadInventory()]);
    if (days.value !== 14) await loadMovement();
  } finally {
    loading.value = false;
  }
}

// 转义 CSV 单元格，避免逗号和引号破坏文件格式。
function csvCell(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`;
}

// 将库存明细导出为 CSV 文件。
function exportCsv() {
  const rows = [
    ['编号', '名称', '分类', '规格', '单位', '当前库存', '安全库存', '状态', '库位', '供应商'],
    ...inventory.value.map((row) => [
      row.code,
      row.name,
      row.category,
      row.spec,
      row.unit,
      row.stock,
      row.safety_stock,
      stockLabel(row),
      row.location,
      row.supplier
    ])
  ];
  const csv = `\ufeff${rows.map((row) => row.map(csvCell).join(',')).join('\n')}`;
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `库存报表-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  ElMessage.success('库存报表已导出');
}

// 页面进入时加载报表数据。
onMounted(loadAll);
</script>
