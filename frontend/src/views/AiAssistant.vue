<!--
  文件说明：AI 助手页面。
  这里提供系统使用问答入口，前端只调用本系统后端接口，真正的智谱 API Key 保存在后端本地配置或环境变量中。
-->
<template>
  <div class="page assistant-page">
    <section class="panel assistant-intro">
      <div class="panel-title">
        <div>
          <h2>AI 助手</h2>
          <p>可查询实时库存、低库存预警、出入库记录，也可咨询系统操作和答辩问题</p>
        </div>
        <el-tag type="primary" effect="plain">实时数据</el-tag>
      </div>

      <div class="prompt-grid">
        <button v-for="item in quickPrompts" :key="item" class="prompt-card" @click="usePrompt(item)">
          {{ item }}
        </button>
      </div>
    </section>

    <section class="panel chat-panel">
      <div class="chat-header">
        <div>
          <h2>库存智能问答</h2>
          <p>每次提问都会由后端读取当前数据库数据，再交给 AI 生成回答</p>
        </div>
        <el-button :icon="Delete" @click="clearChat">清空</el-button>
      </div>

      <div ref="messageBoxRef" class="message-box">
        <div
          v-for="(item, index) in messages"
          :key="index"
          class="message-row"
          :class="item.role"
        >
          <div class="message-avatar">{{ item.role === 'user' ? userInitial : 'AI' }}</div>
          <div class="message-bubble">
            <span class="message-role">{{ item.role === 'user' ? '我' : 'AI 助手' }}</span>
            <p>{{ item.content }}</p>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model.trim="draft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入问题，例如：当前低库存物资有哪些？"
          @keydown.enter.exact.prevent="send"
        />
        <div class="input-actions">
          <span>Enter 发送，Shift + Enter 换行</span>
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="send">发送</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { Delete, Promotion } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, nextTick, ref } from 'vue';
import { aiApi } from '../api';
import { authState } from '../utils/auth';

const sending = ref(false);
const draft = ref('');
const messageBoxRef = ref(null);
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是物资库存管理系统的 AI 助手。你可以问我当前库存总览、某个物资库存、低库存预警、最近入库/出库记录、待审批出库申请，也可以咨询系统操作和答辩问题。'
  }
]);

const quickPrompts = [
  '当前库存总览是什么？',
  '现在有哪些低库存物资？',
  '待审批出库申请有多少？',
  '最近入库记录有哪些？',
  '最近出库记录有哪些？',
  '按分类统计库存情况',
  '如何新增物资？',
  '如何提交出库申请？',
  '管理员如何审核出库申请？',
  '库存不足怎么办？'
];

const userInitial = computed(() => {
  const name = authState.user?.name || authState.user?.username || '我';
  return name.slice(0, 1).toUpperCase();
});

// 点击快捷问题时填入输入框，方便答辩前快速试问。
function usePrompt(prompt) {
  draft.value = prompt;
}

// 把对话滚动到底部，确保最新回复可见。
async function scrollToBottom() {
  await nextTick();
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
  }
}

// 发送问题到后端 AI 接口。后端会读取本地配置或环境变量中的智谱 API Key 并调用模型。
async function send() {
  const content = draft.value.trim();
  if (!content) {
    ElMessage.warning('请输入问题');
    return;
  }

  messages.value.push({ role: 'user', content });
  draft.value = '';
  sending.value = true;
  await scrollToBottom();

  try {
    const history = messages.value
      .slice(-10, -1)
      .filter((item) => item.role === 'user' || item.role === 'assistant');
    const data = await aiApi.chat({ message: content, history });
    messages.value.push({ role: 'assistant', content: data.reply || '我暂时没有生成回答，请稍后再试。' });
  } finally {
    sending.value = false;
    await scrollToBottom();
  }
}

// 清空当前页面内的聊天上下文，不影响后端和数据库。
function clearChat() {
  messages.value = [
    {
      role: 'assistant',
      content: '对话已清空。你可以继续问我实时库存、出入库记录、审批状态或系统使用相关的问题。'
    }
  ];
}
</script>

<style scoped>
.assistant-page {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 120px);
}

.assistant-intro {
  align-self: start;
}

.prompt-grid {
  display: grid;
  gap: 12px;
}

.prompt-card {
  width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #f8fafc;
  color: #1f2937;
  padding: 14px 16px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.prompt-card:hover {
  border-color: #3f6df6;
  background: #eef4ff;
  color: #2f55d9;
}

.chat-panel {
  display: flex;
  min-height: 640px;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-bottom: 1px solid #eef2f7;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.chat-header p {
  margin: 6px 0 0;
  color: #8a94a6;
  font-size: 13px;
}

.message-box {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 50%;
  background: #eef4ff;
  color: #3f6df6;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
}

.message-row.user .message-avatar {
  background: #3f6df6;
  color: #fff;
}

.message-bubble {
  max-width: min(680px, 78%);
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  padding: 12px 14px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.message-row.user .message-bubble {
  background: #eef4ff;
  border-color: #dbe7ff;
}

.message-role {
  display: block;
  margin-bottom: 6px;
  color: #8a94a6;
  font-size: 12px;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #1f2937;
}

.input-area {
  border-top: 1px solid #eef2f7;
  padding: 16px 18px;
  background: #fff;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.input-actions span {
  color: #8a94a6;
  font-size: 12px;
}

@media (max-width: 980px) {
  .assistant-page {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: 560px;
  }
}
</style>
