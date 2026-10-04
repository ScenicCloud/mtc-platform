<template>
  <div class="projects-container">
    <!-- 顶部栏 -->
    <div class="header">
      <div class="logo">MTC · 智能测试平台</div>
      <div class="user-info">
        <span>{{ userStore.user?.username }}</span>
        <el-button type="text" @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧边栏：项目列表 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3>项目列表</h3>
          <el-button type="primary" size="small" @click="openCreateDialog">
            新建项目
          </el-button>
        </div>

        <div class="project-list">
          <div
            v-for="project in projectList"
            :key="project.id"
            class="project-item"
            :class="{ active: selectedProject?.id === project.id }"
            @click="selectProject(project)"
          >
            <div class="project-name">{{ project.name }}</div>
            <div class="project-desc">{{ project.description || '暂无描述' }}</div>
            <div class="project-date">{{ formatDate(project.createdAt) }}</div>
          </div>
          <div v-if="projectList.length === 0 && !loading" class="empty-tip">
            暂无项目，点击上方按钮创建
          </div>
          <div v-if="loading" class="loading-tip">加载中...</div>
        </div>
      </div>

      <!-- 右侧主区域：项目详情 -->
      <div class="content">
        <template v-if="selectedProject">
          <div class="project-header">
            <div>
              <h2 class="project-title">{{ selectedProject.name }}</h2>
              <p class="project-description">
                {{ selectedProject.description || '暂无描述' }}
              </p>
            </div>
            <div class="project-actions">
              <el-button type="primary" @click="goToTestDesign">
                测试设计
              </el-button>
              <el-button @click="handleEdit">编辑</el-button>
              <el-button type="danger" plain @click="handleDelete">
                删除
              </el-button>
            </div>
          </div>

          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-number">{{ selectedProject.testCaseCount || 0 }}</div>
              <div class="stat-label">测试用例</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ selectedProject.testScriptCount || 0 }}</div>
              <div class="stat-label">测试脚本</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ selectedProject.testDataCount || 0 }}</div>
              <div class="stat-label">测试数据</div>
            </div>
          </div>

          <div class="project-meta">
            <div class="meta-item">
              <span class="meta-label">创建时间：</span>
              <span>{{ formatDate(selectedProject.createdAt) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">更新时间：</span>
              <span>{{ formatDate(selectedProject.updatedAt) }}</span>
            </div>
          </div>
        </template>

        <div v-else class="empty-state">
          <el-empty description="请选择或创建一个项目">
            <el-button type="primary" @click="openCreateDialog">新建项目</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 新建 / 编辑项目弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑项目' : '新建项目'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入项目名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入项目描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '../store/user'
import {
  getProjects,
  createProject,
  updateProject,
  deleteProject,
  type Project,
  type ProjectCreateDTO,
} from '../api/project'

const router = useRouter()
const userStore = useUserStore()

const projectList = ref<Project[]>([])
const selectedProject = ref<Project | null>(null)
const loading = ref(false)

// 弹窗相关
const dialogVisible = ref(false)
const isEditMode = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<ProjectCreateDTO>({
  name: '',
  description: '',
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
  ],
}

const formatDate = (dateVal: string | number) => {
  // 后端可能返回秒级时间戳（数字）或 ISO 字符串
  const ts = typeof dateVal === 'number' ? dateVal * 1000 : dateVal
  return new Date(ts).toLocaleString('zh-CN')
}

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await getProjects({ page: 1, pageSize: 100 })
    projectList.value = res.data.records
    if (res.data.records.length > 0 && !selectedProject.value) {
      selectedProject.value = res.data.records[0]
    }
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}

const selectProject = (project: Project) => {
  selectedProject.value = project
}

const goToTestDesign = () => {
  if (selectedProject.value) {
    router.push(`/projects/${selectedProject.value.id}/test-design`)
  }
}

const openCreateDialog = () => {
  isEditMode.value = false
  formData.value = { name: '', description: '' }
  dialogVisible.value = true
}

const handleEdit = () => {
  if (!selectedProject.value) return
  isEditMode.value = true
  formData.value = {
    name: selectedProject.value.name,
    description: selectedProject.value.description,
  }
  dialogVisible.value = true
}

const handleDelete = async () => {
  if (!selectedProject.value) return
  try {
    await ElMessageBox.confirm(
      `确定要删除项目「${selectedProject.value.name}」吗？删除后数据不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await deleteProject(selectedProject.value.id)
    ElMessage.success('删除成功')
    const idx = projectList.value.findIndex((p) => p.id === selectedProject.value!.id)
    projectList.value.splice(idx, 1)
    selectedProject.value = projectList.value.length > 0 ? projectList.value[0] : null
  } catch {
    // 用户取消或请求失败
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.value = { name: '', description: '' }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEditMode.value && selectedProject.value) {
        const res = await updateProject(selectedProject.value.id, formData.value)
        ElMessage.success('更新成功')
        // 更新列表和选中项
        const idx = projectList.value.findIndex((p) => p.id === res.data.id)
        if (idx !== -1) projectList.value[idx] = res.data
        selectedProject.value = res.data
      } else {
        const res = await createProject(formData.value)
        ElMessage.success('创建成功')
        projectList.value.unshift(res.data)
        selectedProject.value = res.data
      }
      dialogVisible.value = false
    } catch {
      // 错误已在拦截器提示
    } finally {
      submitting.value = false
    }
  })
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped>
.projects-container {
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
  width: 280px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.project-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.project-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: background 0.2s;
  background: #fff;
  border: 1px solid #e4e7ed;
}

.project-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.project-item.active {
  background: #409eff;
  border-color: #409eff;
  color: white;
}

.project-item.active .project-desc,
.project-item.active .project-date {
  color: rgba(255, 255, 255, 0.8);
}

.project-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-date {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-tip,
.loading-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 13px;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.project-title {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.project-description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.project-actions {
  display: flex;
  gap: 12px;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.project-meta {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 20px;
}

.meta-item {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.meta-item:last-child {
  margin-bottom: 0;
}

.meta-label {
  color: #909399;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
