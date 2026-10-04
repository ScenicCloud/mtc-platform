<template>
  <div class="test-design-container">
    <!-- 顶部栏 -->
    <div class="header">
      <div class="header-left">
        <el-button text @click="goBack">
          &larr; 返回
        </el-button>
        <div class="project-name">{{ projectName }}</div>
      </div>
      <div class="header-right">
        <span>{{ userStore.user?.username }}</span>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs-bar">
      <el-tabs v-model="activeTab" class="main-tabs">
        <el-tab-pane label="测试用例" name="testCases" />
        <el-tab-pane label="测试脚本" name="testScripts" />
        <el-tab-pane label="测试数据" name="testData" />
      </el-tabs>
    </div>

    <div class="main-content">
      <!-- 左侧：需求输入区 -->
      <div class="left-panel">
        <div class="panel-header">
          <span>需求描述</span>
        </div>
        <div class="panel-body">
          <el-input
            v-model="requirement"
            type="textarea"
            :rows="8"
            placeholder="请输入需求描述，系统将基于此生成测试用例、测试脚本和测试数据..."
            maxlength="3000"
            show-word-limit
            resize="none"
          />
          <div class="base-url-section">
            <div class="field-label">
              基础 URL
              <span class="field-hint">（可选）生成的测试脚本会使用此地址</span>
            </div>
            <el-input
              v-model="baseUrl"
              placeholder="例如：https://your-app.com 或 http://localhost:3000"
              clearable
            />
          </div>
          <div class="actions">
            <el-button
              type="primary"
              :disabled="!canGenerate"
              :loading="isStreaming"
              @click="handleGenerate"
            >
              {{ getGenerateButtonText() }}
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
          <div v-if="statusText" class="status">
            {{ statusText }}
          </div>
        </div>

        <!-- 用例选择区（脚本/数据标签时显示） -->
        <div v-if="activeTab !== 'testCases'" class="panel-section">
          <div class="panel-section-header">选择用例</div>
          <div class="case-selector">
            <el-checkbox
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            >
              全选
            </el-checkbox>
            <div class="case-list">
              <el-checkbox
                v-for="tc in testCases"
                :key="tc.id"
                v-model="selectedCaseIds"
                :label="tc.id"
              >
                {{ tc.title }}
              </el-checkbox>
            </div>
            <div v-if="testCases.length === 0" class="empty-tip">
              请先生成或保存测试用例
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：结果展示区 -->
      <div class="right-panel">
        <!-- 测试用例标签 -->
        <div v-show="activeTab === 'testCases'" class="tab-content">
          <div class="toolbar">
            <span class="result-count">共 {{ testCases.length }} 条用例</span>
            <div class="toolbar-actions">
              <el-button
                type="primary"
                :disabled="testCases.length === 0 || isStreaming"
                @click="handleSaveToProject"
              >
                保存到项目
              </el-button>
            </div>
          </div>

          <!-- 流式生成时的原始输出 -->
          <div v-if="isStreaming && rawOutput" class="stream-output">
            <div class="output-label">生成中（原始数据）</div>
            <div class="output-content">
              {{ rawOutput }}
              <span class="cursor">▌</span>
            </div>
          </div>

          <!-- 用例表格 -->
          <div v-else class="case-table-wrapper">
            <el-table
              :data="testCases"
              stripe
              border
              style="width: 100%"
              @row-click="handleRowClick"
              row-key="id"
            >
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="case-detail">
                    <div class="detail-section">
                      <div class="detail-label">前置条件</div>
                      <div class="detail-content">{{ row.precondition || '无' }}</div>
                    </div>
                    <div class="detail-section">
                      <div class="detail-label">测试步骤</div>
                      <div class="detail-content">
                        <div
                          v-for="(step, idx) in row.steps"
                          :key="idx"
                          class="step-item"
                        >
                          <span class="step-order">步骤 {{ step.order }}：</span>
                          <span class="step-action">{{ step.action }}</span>
                          <div class="step-expected">预期：{{ step.expected }}</div>
                        </div>
                        <div v-if="!row.steps || row.steps.length === 0">
                          无
                        </div>
                      </div>
                    </div>
                    <div class="detail-section">
                      <div class="detail-label">预期结果</div>
                      <div class="detail-content">{{ row.expectedResult || '无' }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="用例标题" min-width="200" />
              <el-table-column prop="module" label="模块" width="120" />
              <el-table-column label="优先级" width="100">
                <template #default="{ row }">
                  <el-tag :type="getPriorityTagType(row.priority)" size="small">
                    {{ row.priority }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <el-empty
              v-if="testCases.length === 0 && !isStreaming"
              description="暂无测试用例，请输入需求后点击生成"
            />
          </div>
        </div>

        <!-- 测试脚本标签 -->
        <div v-show="activeTab === 'testScripts'" class="tab-content">
          <div class="toolbar">
            <span class="result-count">Playwright TypeScript 脚本</span>
            <div class="toolbar-actions">
              <el-button :disabled="!testScriptCode" @click="handleCopyScript">
                复制
              </el-button>
              <el-button :disabled="!testScriptCode" @click="handleDownloadScript">
                下载
              </el-button>
            </div>
          </div>

          <div v-if="isStreaming && activeTab === 'testScripts' && rawOutput" class="stream-output">
            <div class="output-label">生成中</div>
            <div class="output-content code-output">
              {{ rawOutput }}
              <span class="cursor">▌</span>
            </div>
          </div>

          <div v-else class="code-wrapper">
            <pre v-if="testScriptCode" class="code-block"><code>{{ testScriptCode }}</code></pre>
            <el-empty
              v-else
              description="请选择用例后生成测试脚本"
            />
          </div>
        </div>

        <!-- 测试数据标签 -->
        <div v-show="activeTab === 'testData'" class="tab-content">
          <div class="toolbar">
            <span class="result-count">测试数据 (JSON)</span>
            <div class="toolbar-actions">
              <el-button :disabled="!testDataJson" @click="handleCopyData">
                复制
              </el-button>
            </div>
          </div>

          <div v-if="isStreaming && activeTab === 'testData' && rawOutput" class="stream-output">
            <div class="output-label">生成中</div>
            <div class="output-content code-output">
              {{ rawOutput }}
              <span class="cursor">▌</span>
            </div>
          </div>

          <div v-else class="code-wrapper">
            <pre v-if="testDataJson" class="code-block"><code>{{ testDataJson }}</code></pre>
            <el-empty
              v-else
              description="请选择用例后生成测试数据"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { getProject } from '../api/project'
import {
  generateTestCases,
  generateTestScripts,
  generateTestData,
  type SseStreamResult,
} from '../api/testDesign'
import {
  batchCreateTestCases,
  getTestCases,
  type TestCase,
} from '../api/testCase'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = computed(() => Number(route.params.id))
const projectName = ref('')
const activeTab = ref('testCases')

// 需求输入
const requirement = ref('')
const isStreaming = ref(false)
const statusText = ref('')
const rawOutput = ref('')
let sseRequest: SseStreamResult | null = null

// 测试用例
const testCases = ref<TestCase[]>([])

// 基础 URL
const baseUrl = ref('')

// 测试脚本
const testScriptCode = ref('')

// 测试数据
const testDataJson = ref('')

// 用例选择
const selectedCaseIds = ref<number[]>([])
const selectAll = ref(false)

const isIndeterminate = computed(() => {
  return (
    selectedCaseIds.value.length > 0 &&
    selectedCaseIds.value.length < testCases.value.length
  )
})

const canGenerate = computed(() => {
  if (isStreaming.value) return false
  if (activeTab.value === 'testCases') {
    return requirement.value.trim().length > 0
  }
  return selectedCaseIds.value.length > 0
})

const getPriorityTagType = (priority: string) => {
  const map: Record<string, string> = {
    P0: 'danger',
    P1: 'warning',
    P2: 'primary',
    P3: 'info',
  }
  return map[priority] || 'info'
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    draft: 'info',
    active: 'success',
    deprecated: 'danger',
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    draft: '草稿',
    active: '启用',
    deprecated: '废弃',
  }
  return map[status] || status
}

const getGenerateButtonText = () => {
  if (activeTab.value === 'testCases') {
    return isStreaming.value ? '生成中...' : '生成测试用例'
  } else if (activeTab.value === 'testScripts') {
    return isStreaming.value ? '生成中...' : '生成测试脚本'
  } else {
    return isStreaming.value ? '生成中...' : '生成测试数据'
  }
}

const loadProject = async () => {
  try {
    const res = await getProject(projectId.value)
    projectName.value = res.data.name
  } catch {
    // 错误已在拦截器提示
  }
}

const loadExistingTestCases = async () => {
  try {
    const res = await getTestCases(projectId.value, { page: 1, pageSize: 100 })
    testCases.value = res.data.records
  } catch {
    // 错误已在拦截器提示
  }
}

const handleRowClick = (row: TestCase) => {
  // 行点击时展开/收起由 el-table expand 列控制
}

const handleSelectAll = (val: boolean) => {
  if (val) {
    selectedCaseIds.value = testCases.value.map((tc) => tc.id)
  } else {
    selectedCaseIds.value = []
  }
}

// ========== SSE 生成逻辑 ==========

const handleGenerate = () => {
  if (!canGenerate.value) return

  rawOutput.value = ''
  statusText.value = ''
  isStreaming.value = true

  if (activeTab.value === 'testCases') {
    generateCasesStream()
  } else if (activeTab.value === 'testScripts') {
    generateScriptsStream()
  } else {
    generateDataStream()
  }
}

const generateCasesStream = () => {
  testCases.value = []
  sseRequest = generateTestCases(
    {
      projectId: projectId.value,
      requirement: requirement.value,
    },
    userStore.token
  )

  let accumulatedJson = ''

  sseRequest.onEvent((event) => {
    if (event.event === 'meta') {
      statusText.value = '正在生成测试用例...'
    } else if (event.event === 'delta') {
      rawOutput.value += event.data
      accumulatedJson += event.data
      // 尝试解析增量 JSON 构建用例列表
      tryParseTestCases(accumulatedJson)
    } else if (event.event === 'done') {
      statusText.value = '生成完成'
      // 最终解析一次
      tryParseTestCases(accumulatedJson, true)
    } else if (event.event === 'error') {
      try {
        const err = JSON.parse(event.data)
        statusText.value = `错误：${err.message || '未知错误'}`
      } catch {
        statusText.value = `错误：${event.data}`
      }
    }
  })

  sseRequest.onError((msg) => {
    isStreaming.value = false
    statusText.value = msg
    sseRequest = null
    if (msg !== '已取消') {
      ElMessage.error(msg)
    }
  })

  sseRequest.onDone(() => {
    isStreaming.value = false
    sseRequest = null
  })
}

const generateScriptsStream = () => {
  testScriptCode.value = ''
  const selectedCases = testCases.value.filter((tc) => selectedCaseIds.value.includes(tc.id))
  sseRequest = generateTestScripts(
    {
      projectId: projectId.value,
      testCases: selectedCases.map((tc) => ({
        title: tc.title,
        module: tc.module,
        priority: tc.priority,
        precondition: tc.precondition,
        steps: typeof tc.steps === 'string' ? tc.steps : '',
        expectedResult: tc.expectedResult,
      })),
      module: '',
      baseUrl: baseUrl.value,
    },
    userStore.token
  )

  sseRequest.onEvent((event) => {
    if (event.event === 'meta') {
      statusText.value = '正在生成测试脚本...'
    } else if (event.event === 'delta') {
      rawOutput.value += event.data
      testScriptCode.value += event.data
    } else if (event.event === 'done') {
      statusText.value = '生成完成'
    } else if (event.event === 'error') {
      try {
        const err = JSON.parse(event.data)
        statusText.value = `错误：${err.message || '未知错误'}`
      } catch {
        statusText.value = `错误：${event.data}`
      }
    }
  })

  sseRequest.onError((msg) => {
    isStreaming.value = false
    statusText.value = msg
    sseRequest = null
    if (msg !== '已取消') {
      ElMessage.error(msg)
    }
  })

  sseRequest.onDone(() => {
    isStreaming.value = false
    sseRequest = null
  })
}

const generateDataStream = () => {
  testDataJson.value = ''
  const selectedCases = testCases.value.filter((tc) => selectedCaseIds.value.includes(tc.id))
  sseRequest = generateTestData(
    {
      projectId: projectId.value,
      testCases: selectedCases.map((tc) => ({
        title: tc.title,
        module: tc.module,
        priority: tc.priority,
        precondition: tc.precondition,
        steps: typeof tc.steps === 'string' ? tc.steps : '',
        expectedResult: tc.expectedResult,
      })),
      module: '',
    },
    userStore.token
  )

  sseRequest.onEvent((event) => {
    if (event.event === 'meta') {
      statusText.value = '正在生成测试数据...'
    } else if (event.event === 'delta') {
      rawOutput.value += event.data
      testDataJson.value += event.data
    } else if (event.event === 'done') {
      statusText.value = '生成完成'
    } else if (event.event === 'error') {
      try {
        const err = JSON.parse(event.data)
        statusText.value = `错误：${err.message || '未知错误'}`
      } catch {
        statusText.value = `错误：${event.data}`
      }
    }
  })

  sseRequest.onError((msg) => {
    isStreaming.value = false
    statusText.value = msg
    sseRequest = null
    if (msg !== '已取消') {
      ElMessage.error(msg)
    }
  })

  sseRequest.onDone(() => {
    isStreaming.value = false
    sseRequest = null
  })
}

// 尝试从累积的 JSON 文本中解析测试用例
const tryParseTestCases = (jsonStr: string, force = false) => {
  // 简单策略：尝试找完整的数组
  try {
    const trimmed = jsonStr.trim()
    // 尝试找到最后一个完整的用例对象
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) {
      testCases.value = parsed.map((item: any, idx: number) => ({
        id: item.id ?? idx + 1,
        projectId: projectId.value,
        title: item.title || '',
        module: item.module || '',
        priority: item.priority || 'P2',
        status: item.status || 'draft',
        precondition: item.precondition || '',
        steps: item.steps || [],
        expectedResult: item.expectedResult || '',
        createdAt: item.createdAt || new Date().toISOString(),
        updatedAt: item.updatedAt || new Date().toISOString(),
      }))
    }
  } catch {
    // 解析失败，尝试增量方式：提取已完成的 JSON 对象
    if (force) {
      // 最后尝试：找最后一个完整的 } 之前的内容
      const lastBrace = jsonStr.lastIndexOf('}')
      if (lastBrace !== -1) {
        const partial = jsonStr.substring(0, lastBrace + 1)
        // 尝试包装成数组
        try {
          const wrapped = `[${partial}]`
          const parsed = JSON.parse(wrapped.replace(/\}\s*$/, '}').replace(/\}\s*\[/g, '},['))
          // 简化处理：忽略 force 时的复杂解析
        } catch {
          // 忽略
        }
      }
    }
  }
}

const handleStop = () => {
  if (sseRequest) {
    sseRequest.abort()
    sseRequest = null
  }
}

// ========== 保存到项目 ==========

const handleSaveToProject = async () => {
  if (testCases.value.length === 0) return
  try {
    const data = {
      projectId: projectId.value,
      testCases: testCases.value.map((tc) => ({
        title: tc.title,
        module: tc.module,
        priority: tc.priority,
        precondition: tc.precondition,
        steps: typeof tc.steps === 'string' ? tc.steps : (Array.isArray(tc.steps) ? tc.steps.map((s: any) => `${s.order}. ${s.action}\n预期：${s.expected}`).join('\n') : ''),
        expectedResult: tc.expectedResult,
        status: tc.status || 'draft',
      })),
    }
    const res = await batchCreateTestCases(data)
    ElMessage.success(`成功保存 ${res.data.length} 条测试用例`)
    // 更新本地数据的 id
    testCases.value = res.data
  } catch {
    // 错误已在拦截器提示
  }
}

// ========== 复制/下载 ==========

const handleCopyScript = async () => {
  try {
    await navigator.clipboard.writeText(testScriptCode.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const handleDownloadScript = () => {
  const blob = new Blob([testScriptCode.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `test-script-${Date.now()}.spec.ts`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const handleCopyData = async () => {
  try {
    await navigator.clipboard.writeText(testDataJson.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// ========== 导航 ==========

const goBack = () => {
  router.push('/projects')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadProject()
  loadExistingTestCases()
})
</script>

<style scoped>
.test-design-container {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  border-left: 1px solid #e4e7ed;
  padding-left: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.tabs-bar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}

.main-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel {
  width: 380px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.panel-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions {
  display: flex;
  gap: 12px;
}

.base-url-section {
  margin: 12px 0;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.field-hint {
  font-weight: normal;
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.status {
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 4px;
}

.panel-section {
  border-top: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
  flex: 1;
}

.panel-section-header {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.case-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.case-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 20px 0;
  font-size: 13px;
}

.right-panel {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.result-count {
  font-size: 13px;
  color: #606266;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.stream-output {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  margin: 16px;
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
  background: #fff;
}

.output-content.code-output {
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #1e1e1e;
  color: #d4d4d4;
}

.cursor {
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.case-table-wrapper {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
}

.case-detail {
  padding: 12px 20px;
  background: #fafafa;
  border-radius: 4px;
}

.detail-section {
  margin-bottom: 12px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 6px;
}

.detail-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.step-item {
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 2px solid #409eff;
}

.step-order {
  font-weight: 500;
  color: #409eff;
}

.step-action {
  color: #303133;
}

.step-expected {
  margin-top: 4px;
  color: #67c23a;
  font-size: 12px;
}

.code-wrapper {
  flex: 1;
  padding: 16px 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 16px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 6px;
  overflow: auto;
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
}

.code-block code {
  font-family: inherit;
}
</style>
