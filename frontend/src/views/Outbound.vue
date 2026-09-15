<!--
  文件说明：出库管理页面。
  这里体现管理员直接出库、操作员出库申请、管理员审批、库存不足红色提示、负库存拦截和本月出库统计。
-->
<template>
  <div class="page">
    <div class="record-layout">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h2>{{ isAdmin() ? '出库登记' : '出库申请' }}</h2>
            <p>{{ isAdmin() ? '管理员登记后直接扣减库存' : '提交后进入待审批，管理员通过后扣减库存' }}</p>
          </div>
          <el-tag :type="canOutbound ? 'success' : 'info'" effect="plain">
            {{ canOutbound ? (isAdmin() ? '可直接出库' : '可提交申请') : '待校验' }}
          </el-tag>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="物资编号 / 物资名称" prop="materialId">
            <el-select v-model="form.materialId" filterable placeholder="请选择出库物资" style="width: 100%">
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
            <el-form-item label="出库仓库">
              <el-input v-model.trim="form.warehouse" placeholder="如 主仓库" />
            </el-form-item>
            <el-form-item label="出库数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="当前库存">
              <div class="readonly-field">
                {{ selectedMaterial ? `${formatNumber(selectedMaterial.stock)} ${selectedMaterial.unit}` : '选择物资后显示' }}
              </div>
            </el-form-item>
            <el-form-item label="安全库存">
              <div class="readonly-field">
                {{ selectedMaterial ? `${formatNumber(selectedMaterial.safety_stock)} ${selectedMaterial.unit}` : '选择物资后显示' }}
              </div>
            </el-form-item>
            <el-form-item label="领用人" prop="recipient">
              <el-input v-model.trim="form.recipient" placeholder="请输入领用人或部门" />
            </el-form-item>
            <el-form-item label="经办人">
              <div class="readonly-field">{{ operatorName }}</div>
            </el-form-item>
            <el-form-item label="出库时间">
              <div class="readonly-field">{{ operationTime }}</div>
            </el-form-item>
            <el-form-item label="备注" class="full">
              <el-input v-model.trim="form.remark" type="textarea" :rows="3" placeholder="填写出库说明" />
            </el-form-item>
          </div>

          <div v-if="isStockInsufficient" class="stock-warning">
            <el-icon><Warning /></el-icon>
            <div>
              <strong>库存不足，无法出库</strong>
              <span>当前库存不足，请调整出库数量或选择其他仓库</span>
            </div>
          </div>

          <div class="form-actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :disabled="!canOutbound" :loading="saving" @click="submit">
              {{ isAdmin() ? '确认出库' : '提交申请' }}
            </el-button>
          </div>
        </el-form>
      </section>

      <div class="content-grid">
        <section class="panel">
          <div class="panel-title">
            <div>
              <h2>出库明细</h2>
              <p>当前登记单的库存校验结果</p>
            </div>
          </div>
          <el-table :data="detailRows" empty-text="请选择物资并填写出库数量">
            <el-table-column prop="code" label="物资编号" width="110" />
            <el-table-column prop="name" label="物资名称" min-width="120" />
            <el-table-column label="出库数量" width="110">
              <template #default="{ row }">{{ formatNumber(row.quantity) }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="110">
              <template #default="{ row }">{{ formatNumber(row.stock) }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.ok ? 'success' : 'danger'" effect="light">
                  {{ row.ok ? '可申请' : '库存不足' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h2>本月出库统计</h2>
              <p>只统计审批通过的真实出库</p>
            </div>
          </div>
          <div class="mini-stat-grid">
            <div class="mini-card">
              <span>出库总量</span>
              <strong>{{ formatNumber(monthStats.quantity) }}</strong>
            </div>
            <div class="mini-card">
              <span>已通过单据</span>
              <strong>{{ monthStats.count }}</strong>
            </div>
            <div class="mini-card">
              <span>待审批</span>
              <strong>{{ monthStats.pending }}</strong>
            </div>
            <div class="mini-card">
              <span>最近通过</span>
              <strong>{{ monthStats.latest }}</strong>
            </div>
          </div>
        </section>
      </div>
    </div>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>出库记录表格</h2>
          <p>支持按物资、领用人、用途和日期查询，管理员可审批待处理申请</p>
        </div>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <div class="toolbar" style="box-shadow: none; margin-bottom: 14px">
        <div class="filter-row">
          <el-input
            v-model.trim="filters.keyword"
            :prefix-icon="Search"
            clearable
            placeholder="物资、领用人、用途"
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

      <el-table :data="items" v-loading="loading" empty-text="暂无出库记录">
        <el-table-column prop="material_code" label="物资编号" width="112" />
        <el-table-column prop="material_name" label="物资名称" min-width="150" />
        <el-table-column label="出库数量" width="120">
          <template #default="{ row }">{{ formatNumber(row.quantity) }} {{ row.material_unit }}</template>
        </el-table-column>
        <el-table-column prop="purpose" label="出库仓库 / 用途" min-width="150" show-overflow-tooltip />
        <el-table-column prop="recipient" label="领用人" min-width="130" />
        <el-table-column prop="operator" label="经办人" width="110" />
        <el-table-column label="审批状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row)" effect="light">{{ statusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approved_by" label="审批人" width="110">
          <template #default="{ row }">{{ row.approved_by || '-' }}</template>
        </el-table-column>
        <el-table-column label="出库时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="审批时间" width="160">
          <template #default="{ row }">{{ row.approved_at ? formatDateTime(row.approved_at) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="isAdmin()" label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isPending(row)" type="primary" link @click="approve(row)">通过</el-button>
            <el-button v-if="isPending(row)" type="warning" link @click="reject(row)">驳回</el-button>
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
import { Refresh, Search, Warning } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { materialsApi, outboundApi } from '../api';
import { authState, isAdmin } from '../utils/auth';
import { formatDateTime, formatNumber } from '../utils/format';

const loading = ref(false);
const saving = ref(false);
const items = ref([]);
const total = ref(0);
const monthRecords = ref([]);
const materialOptions = ref([]);
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
  recipient: '',
  remark: ''
});

const operatorName = computed(() => authState.user?.name || authState.user?.username || '当前用户');
const operationTime = computed(() => formatDateTime(new Date().toLocaleString('sv-SE')));

// 根据表单选择的 materialId 获取完整物资信息，用于展示库存和安全库存。
const selectedMaterial = computed(() => materialOptions.value.find((item) => item.id === form.materialId));

// 判断当前填写的出库数量是否超过库存，用于显示红色库存不足提示。
const isStockInsufficient = computed(() => {
  if (!selectedMaterial.value || !form.quantity) return false;
  return Number(form.quantity) > Number(selectedMaterial.value.stock || 0);
});

// 判断表单是否允许提交出库：必须选中物资、数量大于 0、且数量不能超过库存。
const canOutbound = computed(() => {
  if (!selectedMaterial.value) return false;
  const quantity = Number(form.quantity || 0);
  return quantity > 0 && quantity <= Number(selectedMaterial.value.stock || 0);
});

// 出库明细表数据，展示当前登记单的物资、数量、库存和校验状态。
const detailRows = computed(() => {
  if (!selectedMaterial.value) return [];
  const quantity = Number(form.quantity || 0);
  return [
    {
      code: selectedMaterial.value.code,
      name: selectedMaterial.value.name,
      quantity,
      unit: selectedMaterial.value.unit,
      stock: Number(selectedMaterial.value.stock || 0),
      safetyStock: Number(selectedMaterial.value.safety_stock || 0),
      ok: canOutbound.value
    }
  ];
});

// 本月出库统计卡片数据，只汇总审批通过的真实出库数量；待审批单独展示数量。
const monthStats = computed(() => {
  const approvedRows = monthRecords.value.filter((row) => isApproved(row));
  const count = approvedRows.length;
  const pending = monthRecords.value.filter((row) => isPending(row)).length;
  const quantity = approvedRows.reduce((sum, row) => sum + Number(row.quantity || 0), 0);
  return {
    count,
    pending,
    quantity,
    average: count ? quantity / count : 0,
    latest: approvedRows[0] ? formatDateTime(approvedRows[0].approved_at || approvedRows[0].created_at).slice(5) : '-'
  };
});

const rules = {
  materialId: [{ required: true, message: '请选择出库物资', trigger: 'change' }],
  quantity: [
    {
      validator: (rule, value, callback) => {
        if (!value || Number(value) <= 0) callback(new Error('出库数量必须大于 0'));
        else if (selectedMaterial.value && Number(value) > Number(selectedMaterial.value.stock || 0)) {
          callback(new Error('库存不足，无法出库'));
        } else callback();
      },
      trigger: 'blur'
    }
  ],
  recipient: [{ required: true, message: '请输入领用人或领用部门', trigger: 'blur' }]
};

// 将筛选条件转换为出库记录查询接口参数。
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

// 将 Date 对象格式化为后端接口使用的 YYYY-MM-DD。
function dateText(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// 生成本月统计接口参数，范围是本月 1 日到今天。
function monthParams() {
  const now = new Date();
  return {
    from: dateText(new Date(now.getFullYear(), now.getMonth(), 1)),
    to: dateText(now),
    page: 1,
    pageSize: 100
  };
}

// 统一读取出库审批状态。旧数据库记录可能没有 status 字段，按已通过处理。
function outboundStatus(row) {
  return row.status || row.approval_status || 'approved';
}

// 判断出库申请是否待审批，用于控制管理员审批按钮显示。
function isPending(row) {
  return outboundStatus(row) === 'pending';
}

// 判断出库记录是否已经通过审批，通过记录删除时需要回滚库存。
function isApproved(row) {
  return outboundStatus(row) === 'approved';
}

// 把后端状态值转换为页面上的中文标签。
function statusLabel(row) {
  const labels = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已驳回'
  };
  return labels[outboundStatus(row)] || '已通过';
}

// 根据审批状态选择 Element Plus 标签颜色。
function statusType(row) {
  const types = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  };
  return types[outboundStatus(row)] || 'success';
}

// 加载出库记录、物资选项和本月统计所需记录。
async function loadData() {
  loading.value = true;
  try {
    const [records, materials, monthData] = await Promise.all([
      outboundApi.list(params()),
      materialsApi.list({ page: 1, pageSize: 100 }),
      outboundApi.list(monthParams())
    ]);
    items.value = records.items || [];
    total.value = records.total || 0;
    materialOptions.value = materials.items || [];
    monthRecords.value = monthData.items || [];
  } finally {
    loading.value = false;
  }
}

// 查询出库记录：回到第一页后重新加载。
function search() {
  filters.page = 1;
  loadData();
}

// 重置出库记录筛选条件。
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

// 重置出库登记表单。
function resetForm() {
  Object.assign(form, {
    materialId: '',
    warehouse: '主仓库',
    quantity: 1,
    recipient: '',
    remark: ''
  });
  formRef.value?.clearValidate();
}

// 提交出库：管理员会直接扣减库存；操作员提交后进入待审批，库存暂不扣减。
async function submit() {
  await formRef.value.validate();
  if (!canOutbound.value) {
    ElMessage.warning('库存不足，无法出库');
    return;
  }
  saving.value = true;
  try {
    await outboundApi.create({
      materialId: form.materialId,
      quantity: form.quantity,
      recipient: form.recipient,
      purpose: form.warehouse,
      remark: form.remark
    });
    ElMessage.success(isAdmin() ? '出库成功，库存已更新' : '出库申请已提交，等待管理员审批');
    resetForm();
    await loadData();
  } finally {
    saving.value = false;
  }
}

// 管理员审批通过：后端会再次校验库存，库存足够时扣减库存。
async function approve(row) {
  await ElMessageBox.confirm('审批通过后将立即扣减库存，确定通过该出库申请吗？', '审批确认', { type: 'warning' });
  await outboundApi.approve(row.id);
  ElMessage.success('审批通过，库存已扣减');
  loadData();
}

// 管理员驳回申请：只记录驳回原因，库存保持不变。
async function reject(row) {
  let remark = '';
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因（可选）', '驳回申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '如：领用信息不完整',
      inputType: 'textarea',
      type: 'warning'
    });
    remark = result.value || '';
  } catch {
    return;
  }
  await outboundApi.reject(row.id, { remark });
  ElMessage.success('已驳回，库存未变化');
  loadData();
}

// 删除出库记录：已通过记录后端会加回库存，待审批和已驳回记录删除时库存不变。
async function remove(row) {
  const message = isApproved(row)
    ? '删除该已通过出库记录会同步回滚库存，确定继续吗？'
    : '删除该出库申请不会改变库存，确定继续吗？';
  await ElMessageBox.confirm(message, '删除确认', { type: 'warning' });
  await outboundApi.remove(row.id);
  ElMessage.success('删除成功');
  loadData();
}

// 页面进入时加载出库相关数据。
onMounted(loadData);
</script>
