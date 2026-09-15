<!--
  文件说明：ECharts 图表通用组件。
  库存总览、报表、入库趋势等图表都复用这里，体现统计图表功能的组件化封装。
-->
<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup>
import * as echarts from 'echarts';
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  option: {
    type: Object,
    required: true
  }
});

const chartRef = ref(null);
let chart;
let observer;

function render() {
  if (!chart || !props.option) return;
  chart.setOption(props.option, true);
}

onMounted(async () => {
  await nextTick();
  chart = echarts.init(chartRef.value);
  render();
  observer = new ResizeObserver(() => chart?.resize());
  observer.observe(chartRef.value);
});

watch(() => props.option, render, { deep: true });

onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
});
</script>
