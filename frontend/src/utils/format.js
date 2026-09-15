// 文件说明：前端通用格式化工具。
// 这里统一处理时间、数字和库存状态显示，保证各页面展示逻辑一致。
// 格式化后端 LocalDateTime 字符串，只展示到分钟。
export function formatDateTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

// 格式化数字：整数不显示小数，非整数保留两位。
export function formatNumber(value) {
  const number = Number(value || 0);
  return Number.isInteger(number) ? String(number) : number.toFixed(2);
}

// 根据当前库存和安全库存返回 Element Plus 标签类型。
export function stockStatus(row) {
  const stock = Number(row.stock || 0);
  const safetyStock = Number(row.safety_stock || 0);
  if (stock <= 0) return 'danger';
  return stock <= safetyStock ? 'warning' : 'success';
}

// 根据当前库存和安全库存返回中文库存状态。
export function stockLabel(row) {
  const stock = Number(row.stock || 0);
  const safetyStock = Number(row.safety_stock || 0);
  if (stock <= 0) return '库存不足';
  return stock <= safetyStock ? '低库存' : '正常';
}
