<template>
  <div class="sample-container">
    <!-- 顶部栏 -->
    <div class="header">
      <div class="logo">MTC · 智能测试平台</div>
      <div class="user-info">
        <span>{{ userStore.user?.username }}</span>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧：空间列表 -->
      <div class="sidebar">
        <h3>空间列表（占位）</h3>
        <div
          v-for="space in spaces"
          :key="space.id"
          class="space-item"
          :class="{ active: selectedSpaceId === space.id }"
          @click="selectSpace(space)"
        >
          <div class="space-name">{{ space.name }}</div>
          <div class="space-date">{{ formatDate(space.createdAt) }}</div>
        </div>
      </div>

      <!-- 右侧：分析区 -->
      <div class="content">
        <div class="input-area">
          <el-input
            v-model="prompt"
            type="textarea"
            :rows="4"
            placeholder="输入要分析的内容..."
            maxlength="2000"
            show-word-limit
          />
          <div class="actions">
            <el-button
              type="primary"
              :disabled="!canSubmit"
              :loading="isStreaming"
              @click="handleAnalyze"
            >
              {{ isStreaming ? '生成中...' : '开始分析' }}
            </el-button>
            <el-button
              v-if="isStreaming"
              type="danger"
              plain
              @click="handleStop"
            >
              停止
            </el-button>
          </div>
        </div>

        <div class="output-area">
          <div class="output-label">输出结果</div>
          <div
            class="output-content"
            :class="{ 'output-empty': !outputText }"
          >
            {{ outputText || '点击"开始分析"查看流式输出效果' }}
            <span v-if="isStreaming" class="cursor">▌</span>
          </div>
          <div v-if="statusText" class="status">
            {{ statusText }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import http from '../api/http'
import { createSseRequest, type SseEvent } from '../api/sse'
import type { ApiResult } from '../api/types'

const router = useRouter()
const userStore = useUserStore()

interface SpaceVO {
  id: number
  name: string
  createdAt: string
}

const spaces = ref<SpaceVO[]>([])
const selectedSpaceId = ref<number | null>(null)
const prompt = ref('')
const outputText = ref('')
const isStreaming = ref(false)
const statusText = ref('')
let sseAbort: (() => void) | null = null

const canSubmit = computed(() => {
  return selectedSpaceId.value !== null && prompt.value.trim().length > 0 && !isStreaming.value
})

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadSpaces = async () => {
  try {
    const res = await http.get<any, ApiResult<SpaceVO[]>>('/sample/spaces')
    spaces.value = res.data
    if (res.data.length > 0) {
      selectedSpaceId.value = res.data[0].id
    }
  } catch {
    // 错误已在拦截器提示
  }
}

const selectSpace = (space: SpaceVO) => {
  selectedSpaceId.value = space.id
}

const handleAnalyze = () => {
  if (!canSubmit.value || selectedSpaceId.value === null) return

  outputText.value = ''
  statusText.value = ''
  isStreaming.value = true

  const requestBody = {
    spaceId: selectedSpaceId.value,
    prompt: prompt.value,
  }

  sseAbort = createSseRequest({
    url: '/api/v1/sample/analyze',
    method: 'POST',
    body: requestBody,
    token: userStore.token,
    onEvent: (event: SseEvent) => {
      if (event.event === 'meta') {
        statusText.value = '正在生成...'
      } else if (event.event === 'delta') {
        outputText.value += event.data
      } else if (event.event === 'done') {
        statusText.value = '生成完成'
      } else if (event.event === 'error') {
        try {
          const err = JSON.parse(event.data)
          statusText.value = `错误：${err.message || '未知错误'}`
        } catch {
          statusText.value = `错误：${event.data}`
        }
      } else {
        // 未知事件
        statusText.value = `收到未知事件：${event.event}`
      }
    },
    onError: (msg: string) => {
      isStreaming.value = false
      statusText.value = msg
      sseAbort = null
      if (msg !== '已取消') {
        ElMessage.error(msg)
      }
    },
    onDone: () => {
      isStreaming.value = false
      sseAbort = null
    },
  })
}

const handleStop = () => {
  if (sseAbort) {
    sseAbort()
    sseAbort = null
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadSpaces()
})
</script>

<style scoped>
.sample-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.logo {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
  background: #fafafa;
}

.sidebar h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.space-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.space-item:hover {
  background: #ecf5ff;
}

.space-item.active {
  background: #409eff;
  color: white;
}

.space-name {
  font-size: 14px;
  margin-bottom: 4px;
}

.space-date {
  font-size: 12px;
  opacity: 0.7;
}

.content {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions {
  display: flex;
  gap: 12px;
}

.output-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.output-label {
  padding: 10px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 13px;
  color: #606266;
}

.output-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.output-empty {
  color: #c0c4cc;
}

.cursor {
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.status {
  padding: 8px 16px;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
  background: #fafafa;
}
</style>
