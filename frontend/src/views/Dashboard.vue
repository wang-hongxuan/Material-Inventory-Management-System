<!--
  文件说明：库存总览工作台页面。
  这里展示库存概览卡片、最近入库/出库记录、低库存预警和 ECharts 统计图表。
-->
<template>
  <div class="page" v-loading="loading">
    <div class="stats-grid">
      <article v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-icon" :class="item.tone">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </article>
    </div>

    <div class="content-grid equal-columns">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>最近入库记录</h2>
            <p>按最新登记时间排序</p>
          </div>
          <el-tag type="success" effect="plain">{{ recentInbound.length }} 条</el-tag>
        </div>
        <el-table :data="recentInbound" empty-text="暂无入库记录">
          <el-table-column prop="material_code" label="物资编号" width="112" />
          <el-table-column prop="material_name" label="物资名称" min-width="130" />
          <el-table-column label="入库数量" width="110">
            <template #default="{ row }">{{ formatNumber(row.quantity) }} {{ row.material_unit }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="经办人" width="96" />
          <el-table-column label="时间" width="145">
            <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>最近出库记录</h2>
            <p>用于快速核对近期领用和审批状态</p>
          </div>
          <el-tag type="warning" effect="plain">{{ recentOutbound.length }} 条</el-tag>
        </div>
        <el-table :data="recentOutbound" empty-text="暂无出库记录">
          <el-table-column prop="material_code" label="物资编号" width="112" />
          <el-table-column prop="material_name" label="物资名称" min-width="130" />
          <el-table-column label="出库数量" width="110">
            <template #default="{ row }">{{ formatNumber(row.quantity) }} {{ row.material_unit }}</template>
          </el-table-column>
          <el-table-column label="状态" width="92">
            <template #default="{ row }">
              <el-tag :type="statusType(row)" effect="light">{{ statusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="recipient" label="领用人" width="110" />
          <el-table-column label="时间" width="145">
            <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <div class="content-grid two-columns">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>近 14 天出入库趋势</h2>
            <p>入库与出库数量对比</p>
          </div>
        </div>
        <BaseChart :option="movementOption" />
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>分类库存占比</h2>
            <p>按物资分类汇总库存</p>
          </div>
        </div>
        <BaseChart :option="categoryOption" />
      </section>
    </div>

    <div class="content-grid two-columns">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>库存 Top 8</h2>
            <p>当前库存较高的物资</p>
          </div>
        </div>
        <BaseChart :option="topStockOption" />
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>低库存预警</h2>
            <p>低于安全库存的物资</p>
          </div>
          <el-tag type="warning" effect="plain">{{ payload.lowStock.length }} 项</el-tag>
        </div>
        <el-table :data="payload.lowStock" height="320" empty-text="暂无低库存物资">
          <el-table-column prop="code" label="编号" width="96" />
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column label="库存" width="112">
            <template #default="{ row }">{{ formatNumber(row.stock) }} {{ row.unit }}</template>
          </el-table-column>
          <el-table-column prop="location" label="库位" min-width="120" />
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup>
import { Box, DataAnalysis, Goods, TopRight, Warning } from '@element-plus/icons-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { inboundApi, outboundApi, reportsApi } from '../api';
import BaseChart from '../components/BaseChart.vue';
import { formatDateTime, formatNumber } from '../utils/format';

const loading = ref(false);
const payload = reactive({
  summary: {
    materialCount: 0,
    totalStock: 0,
    lowStockCount: 0,
    todayInbound: 0,
    todayOutbound: 0
  },
  stockByCategory: [],
  movement: [],
  topStock: [],
  lowStock: []
});
const recentInbound = ref([]);
const recentOutbound = ref([]);

// 出库审批状态展示；旧记录没有 status 时按已通过处理。
function statusLabel(row) {
  const labels = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已驳回'
  };
  return labels[row.status || row.approval_status || 'approved'] || '已通过';
}

function statusType(row) {
  const types = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  };
  return types[row.status || row.approval_status || 'approved'] || 'success';
}

// 顶部统计卡片数据，由 dashboard 接口返回的 summary 组装而成。
const stats = computed(() => [
  {
    label: '物资总数',
    value: payload.summary.materialCount,
    hint: '已建档物资种类',
    icon: Goods,
    tone: 'blue'
  },
  {
    label: '库存总量',
    value: formatNumber(payload.summary.totalStock),
    hint: '全部物资当前合计',
    icon: DataAnalysis,
    tone: 'green'
  },
  {
    label: '低库存预警',
    value: payload.summary.lowStockCount,
    hint: '需要及时补货',
    icon: Warning,
    tone: 'orange'
  },
  {
    label: '今日入库',
    value: formatNumber(payload.summary.todayInbound),
    hint: '提交后自动增加库存',
    icon: Box,
    tone: 'green'
  },
  {
    label: '今日出库',
    value: formatNumber(payload.summary.todayOutbound),
    hint: '审批通过后扣减库存',
    icon: TopRight,
    tone: 'blue'
  }
]);

const axisStyle = {
  axisLine: { lineStyle: { color: '#dbe3ef' } },
  axisTick: { show: false },
  axisLabel: { color: '#8a94a6' },
  splitLine: { lineStyle: { color: '#eef2f7' } }
};

// 近 14 天出入库趋势图配置，入库用柱状图，出库用折线图。
const movementOption = computed(() => ({
  color: ['#4f7df3', '#22c55e'],
  tooltip: { trigger: 'axis' },
  legend: {
    top: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#667085' },
    data: ['入库', '出库']
  },
  grid: { left: 38, right: 22, top: 48, bottom: 34 },
  xAxis: {
    type: 'category',
    data: payload.movement.map((item) => item.day.slice(5)),
    ...axisStyle
  },
  yAxis: { type: 'value', ...axisStyle },
  series: [
    {
      name: '入库',
      type: 'bar',
      barWidth: 12,
      itemStyle: { borderRadius: [6, 6, 0, 0] },
      data: payload.movement.map((item) => item.inbound)
    },
    {
      name: '出库',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      lineStyle: { width: 3 },
      data: payload.movement.map((item) => item.outbound)
    }
  ]
}));

// 分类库存占比图配置，用于展示各分类库存数量比例。
const categoryOption = computed(() => ({
  color: ['#3f6df6', '#22c55e', '#f59e0b', '#60a5fa', '#94a3b8'],
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#667085' }
  },
  series: [
    {
      name: '库存',
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: { color: '#475569' },
      data: payload.stockByCategory.map((item) => ({
        name: item.category,
        value: item.stock
      }))
    }
  ]
}));

// 库存 Top 8 横向柱状图配置。
const topStockOption = computed(() => ({
  color: ['#3f6df6'],
  tooltip: { trigger: 'axis' },
  grid: { left: 84, right: 20, top: 16, bottom: 32 },
  xAxis: { type: 'value', ...axisStyle },
  yAxis: {
    type: 'category',
    data: payload.topStock.map((item) => item.name).reverse(),
    ...axisStyle
  },
  series: [
    {
      name: '库存',
      type: 'bar',
      barWidth: 14,
      itemStyle: { borderRadius: [0, 8, 8, 0] },
      data: payload.topStock.map((item) => item.stock).reverse()
    }
  ]
}));

// 加载工作台所有数据：统计概览、图表、最近入库和最近出库。
async function loadData() {
  loading.value = true;
  try {
    const [dashboard, inbound, outbound] = await Promise.all([
      reportsApi.dashboard(),
      inboundApi.list({ page: 1, pageSize: 6 }),
      outboundApi.list({ page: 1, pageSize: 6 })
    ]);
    Object.assign(payload.summary, dashboard.summary);
    payload.stockByCategory = dashboard.stockByCategory || [];
    payload.movement = dashboard.movement || [];
    payload.topStock = dashboard.topStock || [];
    payload.lowStock = dashboard.lowStock || [];
    recentInbound.value = inbound.items || [];
    recentOutbound.value = outbound.items || [];
  } finally {
    loading.value = false;
  }
}

// 页面进入时加载工作台数据。
onMounted(loadData);
</script>
