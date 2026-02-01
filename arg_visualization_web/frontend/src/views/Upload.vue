<template>
  <div class="upload-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('upload.title') }}</span>
        </div>
      </template>
      
      <el-form :model="uploadForm" label-width="120px" class="upload-form">
        <!-- 输入类型选择 -->
        <el-form-item :label="$t('upload.inputType')">
          <el-radio-group v-model="inputType" @change="handleInputTypeChange">
            <el-radio-button value="sequence">{{ $t('upload.inputTypes.sequence') }}</el-radio-button>
            <el-radio-button value="file">{{ $t('upload.inputTypes.file') }}</el-radio-button>
            <el-radio-button value="mag">{{ $t('upload.inputTypes.mag') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- MAG 名称（仅 MAG 模式） -->
        <el-form-item v-if="inputType === 'mag'" :label="$t('upload.magName')">
          <el-input
            v-model="uploadForm.magName"
            :placeholder="$t('upload.magNamePlaceholder')"
          />
        </el-form-item>
        
        <!-- 文件类型（非 MAG 模式） -->
        <el-form-item v-if="inputType !== 'mag'" :label="$t('upload.fileType')">
          <el-select v-model="uploadForm.fileType" :placeholder="$t('upload.fileTypePlaceholder')" style="width: 100%">
            <el-option :label="$t('upload.fileTypes.autoDetect')" value="auto-detect" />
            <el-option :label="$t('upload.fileTypes.fasta')" value="fasta" />
            <el-option :label="$t('upload.fileTypes.faa')" value="faa" />
          </el-select>
        </el-form-item>
        
        <el-form-item :label="$t('upload.description')">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="2"
            :placeholder="$t('upload.descriptionPlaceholder')"
          />
        </el-form-item>
        
        <el-form-item :label="$t('upload.uploadContent')">
          <div class="upload-content-wrapper">
            <!-- 序列输入模式 -->
            <div v-if="inputType === 'sequence'">
              <el-input
                v-model="uploadForm.textContent"
                type="textarea"
                :rows="8"
                :placeholder="$t('upload.sequencePlaceholder')"
              />
              <div class="el-upload__tip text-upload-tip">
                {{ $t('upload.sequenceTip') }}
              </div>
            </div>

            <!-- 单文件上传模式 -->
            <div v-else-if="inputType === 'file'">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-exceed="handleExceed"
                drag
                class="upload-demo"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">
                  {{ $t('upload.dragText') }}<em>{{ $t('upload.clickUpload') }}</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    {{ $t('upload.fileTip') }}
                  </div>
                </template>
              </el-upload>
            </div>

            <!-- MAG 文件夹上传模式 -->
            <div v-else-if="inputType === 'mag'">
              <!-- 上传方式选择 -->
              <div class="mag-upload-mode">
                <el-radio-group v-model="magUploadMode" size="small">
                  <el-radio-button value="folder">{{ $t('upload.magUploadMode.folder') }}</el-radio-button>
                  <el-radio-button value="files">{{ $t('upload.magUploadMode.files') }}</el-radio-button>
                </el-radio-group>
              </div>

              <!-- 文件夹上传区域 -->
              <div 
                class="folder-upload-area"
                @click="triggerFolderInput"
                @dragover.prevent="onDragOver"
                @dragleave.prevent="onDragLeave"
                @drop.prevent="onDropFolder"
                :class="{ 'drag-over': isDragOver }"
              >
                <el-icon class="folder-icon"><FolderOpened /></el-icon>
                <div class="upload-text">
                  <span v-if="magUploadMode === 'folder'">
                    {{ $t('upload.magFolderText') }}
                  </span>
                  <span v-else>
                    {{ $t('upload.magFilesText') }}
                  </span>
                </div>
                <div class="upload-tip">
                  {{ $t('upload.magFileTip') }}
                </div>
              </div>
              
              <!-- 隐藏的文件夹选择器 -->
              <input
                ref="folderInputRef"
                type="file"
                webkitdirectory
                directory
                multiple
                style="display: none"
                @change="handleFolderSelect"
                accept=".fa,.fasta,.fna"
              />
              
              <!-- 隐藏的多文件选择器 -->
              <input
                ref="filesInputRef"
                type="file"
                multiple
                style="display: none"
                @change="handleFilesSelect"
                accept=".fa,.fasta,.fna"
              />
              
              <!-- MAG 文件列表预览 -->
              <div v-if="magFileList.length > 0" class="mag-file-preview">
                <div class="mag-file-header">
                  <span>
                    <el-icon><FolderOpened /></el-icon>
                    {{ magFolderName || $t('upload.selected') }} - {{ magFileList.length }} {{ $t('upload.filesCount', { count: '' }).replace('{count}', '') }}
                  </span>
                  <el-button type="danger" link size="small" @click="clearMagFiles">
                    {{ $t('upload.clear') }}
                  </el-button>
                </div>
                <div class="mag-file-list">
                  <el-tag 
                    v-for="(file, index) in magFileList.slice(0, 10)" 
                    :key="index"
                    closable
                    @close="removeMagFile(index)"
                    class="mag-file-tag"
                  >
                    {{ file.name }}
                  </el-tag>
                  <el-tag v-if="magFileList.length > 10" type="info">
                    {{ $t('upload.moreFiles', { count: magFileList.length - 10 }) }}
                  </el-tag>
                </div>
              </div>

              <!-- MAG 说明 -->
              <el-alert
                type="info"
                :closable="false"
                class="mag-info-alert"
              >
                <template #title>
                  <div class="mag-info-content">
                    <strong>{{ $t('upload.magInfo.title') }}</strong>
                    <p>{{ $t('upload.magInfo.step1') }}</p>
                    <p>{{ $t('upload.magInfo.step2') }}</p>
                    <p>{{ $t('upload.magInfo.step3') }}</p>
                  </div>
                </template>
              </el-alert>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="uploading"
            :disabled="!canUpload"
            @click="handleUpload"
          >
            <el-icon><Upload /></el-icon>
            {{ uploading ? $t('upload.uploading') : (inputType === 'mag' ? $t('upload.uploadAndAnalyzeMag') : $t('upload.startUpload')) }}
          </el-button>
          <el-button @click="handleReset">{{ $t('upload.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 上传进度 -->
    <el-card v-if="uploadProgress.show" class="progress-card">
      <template #header>
        <div class="card-header">
          <span>{{ inputType === 'mag' ? $t('upload.progress.magTitle') : $t('upload.progress.title') }}</span>
        </div>
      </template>
      
      <div class="progress-content">
        <el-progress
          :percentage="uploadProgress.percentage"
          :status="uploadProgress.status"
        />
        <p class="progress-text">{{ uploadProgress.text }}</p>
        <!-- MAG 两阶段进度显示 -->
        <div v-if="inputType === 'mag' && uploadProgress.stage" class="stage-info">
          <el-steps :active="uploadProgress.stage - 1" simple>
            <el-step :title="$t('upload.steps.prodigal')" />
            <el-step :title="$t('upload.steps.arg')" />
          </el-steps>
        </div>
      </div>
    </el-card>
    
    <!-- 我的文件列表 -->
    <el-card class="files-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('upload.myFiles') }}</span>
          <el-button type="primary" link @click="refreshFiles">
            <el-icon><Refresh /></el-icon>
            {{ $t('upload.refresh') }}
          </el-button>
        </div>
      </template>
      
      <el-table :data="files" v-loading="loadingFiles">
        <el-table-column prop="originalFilename" :label="$t('upload.table.filename')" min-width="200" />
        <el-table-column prop="fileType" :label="$t('upload.table.fileType')" width="120" />
        <el-table-column :label="$t('upload.table.fileSize')" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('upload.table.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('upload.table.uploadTime')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('upload.table.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAnalyze(row)">
              {{ $t('upload.actions.analyze') }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              {{ $t('upload.actions.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Upload, Refresh, FolderOpened } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { uploadGenomeFile, getUserFiles, deleteFile } from '@/api/file'
import { createTask } from '@/api/task'
import { uploadMag } from '@/api/mag'

const { t } = useI18n()
const router = useRouter()

const uploadRef = ref()
const magUploadRef = ref()
const folderInputRef = ref()
const filesInputRef = ref()
const uploading = ref(false)
const loadingFiles = ref(false)
const files = ref([])
const inputType = ref('sequence')
const magUploadMode = ref('folder')
const magFileList = ref([])
const magFolderName = ref('')
const isDragOver = ref(false)

const uploadForm = reactive({
  file: null,
  fileType: 'auto-detect',
  description: '',
  textContent: '',
  magName: ''
})

const uploadProgress = reactive({
  show: false,
  percentage: 0,
  status: '',
  text: '',
  stage: 0
})

const canUpload = computed(() => {
  if (inputType.value === 'sequence') {
    return !!uploadForm.textContent && uploadForm.textContent.trim().length > 0
  } else if (inputType.value === 'file') {
    return !!uploadForm.file
  } else if (inputType.value === 'mag') {
    return magFileList.value.length > 0
  }
  return false
})

// 切换输入类型
const handleInputTypeChange = () => {
  uploadForm.file = null
  uploadForm.textContent = ''
  magFileList.value = []
  magFolderName.value = ''
  uploadRef.value?.clearFiles()
  magUploadRef.value?.clearFiles()
}

// 单文件改变
const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

// 超出文件数量限制
const handleExceed = () => {
  ElMessage.warning(t('upload.messages.onlyOneFile'))
}

// 触发文件夹选择
const triggerFolderInput = () => {
  if (magUploadMode.value === 'folder') {
    folderInputRef.value?.click()
  } else {
    filesInputRef.value?.click()
  }
}

// 处理文件夹选择
const handleFolderSelect = (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  processSelectedFiles(files, true)
  event.target.value = ''
}

// 处理多文件选择
const handleFilesSelect = (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  processSelectedFiles(files, false)
  event.target.value = ''
}

// 处理选中的文件
const processSelectedFiles = (files, isFolder) => {
  const validExtensions = ['fa', 'fasta', 'fna']
  const validFiles = []
  let folderName = ''
  
  for (const file of files) {
    const ext = file.name.split('.').pop()?.toLowerCase()
    
    if (validExtensions.includes(ext)) {
      validFiles.push(file)
      
      if (isFolder && file.webkitRelativePath && !folderName) {
        const pathParts = file.webkitRelativePath.split('/')
        if (pathParts.length > 1) {
          folderName = pathParts[0]
        }
      }
    }
  }
  
  if (validFiles.length === 0) {
    ElMessage.warning(t('upload.messages.noValidFiles'))
    return
  }
  
  magFileList.value = validFiles
  magFolderName.value = folderName || ''
  
  if (folderName && !uploadForm.magName) {
    uploadForm.magName = folderName
  }
  
  ElMessage.success(t('upload.messages.selectedFiles', { count: validFiles.length }))
}

// 拖拽相关
const onDragOver = () => {
  isDragOver.value = true
}

const onDragLeave = () => {
  isDragOver.value = false
}

const onDropFolder = async (event) => {
  isDragOver.value = false
  
  const items = event.dataTransfer.items
  if (!items || items.length === 0) return
  
  const validExtensions = ['fa', 'fasta', 'fna']
  const validFiles = []
  let folderName = ''
  
  for (const item of items) {
    if (item.kind === 'file') {
      const entry = item.webkitGetAsEntry?.()
      
      if (entry) {
        if (entry.isDirectory) {
          folderName = entry.name
          const files = await readDirectoryFiles(entry)
          for (const file of files) {
            const ext = file.name.split('.').pop()?.toLowerCase()
            if (validExtensions.includes(ext)) {
              validFiles.push(file)
            }
          }
        } else if (entry.isFile) {
          const file = item.getAsFile()
          if (file) {
            const ext = file.name.split('.').pop()?.toLowerCase()
            if (validExtensions.includes(ext)) {
              validFiles.push(file)
            }
          }
        }
      } else {
        const file = item.getAsFile()
        if (file) {
          const ext = file.name.split('.').pop()?.toLowerCase()
          if (validExtensions.includes(ext)) {
            validFiles.push(file)
          }
        }
      }
    }
  }
  
  if (validFiles.length === 0) {
    ElMessage.warning(t('upload.messages.noValidFiles'))
    return
  }
  
  magFileList.value = validFiles
  magFolderName.value = folderName
  
  if (folderName && !uploadForm.magName) {
    uploadForm.magName = folderName
  }
  
  ElMessage.success(t('upload.messages.selectedFiles', { count: validFiles.length }))
}

// 递归读取文件夹中的文件
const readDirectoryFiles = (directoryEntry) => {
  return new Promise((resolve) => {
    const files = []
    const dirReader = directoryEntry.createReader()
    
    const readEntries = () => {
      dirReader.readEntries(async (entries) => {
        if (entries.length === 0) {
          resolve(files)
          return
        }
        
        for (const entry of entries) {
          if (entry.isFile) {
            const file = await getFileFromEntry(entry)
            if (file) files.push(file)
          } else if (entry.isDirectory) {
            const subFiles = await readDirectoryFiles(entry)
            files.push(...subFiles)
          }
        }
        
        readEntries()
      })
    }
    
    readEntries()
  })
}

// 从 FileEntry 获取 File 对象
const getFileFromEntry = (fileEntry) => {
  return new Promise((resolve) => {
    fileEntry.file(
      (file) => resolve(file),
      () => resolve(null)
    )
  })
}

// 移除单个 MAG 文件
const removeMagFile = (index) => {
  magFileList.value.splice(index, 1)
  magUploadRef.value?.clearFiles()
}

// 清空所有 MAG 文件
const clearMagFiles = () => {
  magFileList.value = []
  magFolderName.value = ''
  magUploadRef.value?.clearFiles()
}

// 上传文件
const handleUpload = async () => {
  if (inputType.value === 'mag') {
    await handleMagUpload()
  } else {
    await handleNormalUpload()
  }
}

// 普通上传（序列/单文件）
const handleNormalUpload = async () => {
  if (inputType.value === 'sequence') {
    if (!uploadForm.textContent || !uploadForm.textContent.trim()) {
      ElMessage.warning(t('upload.messages.enterText'))
      return
    }
    const text = uploadForm.textContent.trim()
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    const virtualFileName = `pasted_sequence_${Date.now()}.fasta`
    uploadForm.file = new File([blob], virtualFileName, { type: 'text/plain;charset=utf-8' })
  } else if (inputType.value === 'file') {
    if (!uploadForm.file) {
      ElMessage.warning(t('upload.messages.selectFile'))
      return
    }
  }
  
  const maxFileSize = 524288000
  if (uploadForm.file.size > maxFileSize) {
    const fileSizeMB = (uploadForm.file.size / (1024 * 1024)).toFixed(2)
    ElMessage.error(t('upload.messages.fileTooLarge', { size: fileSizeMB }))
    return
  }
  
  try {
    uploading.value = true
    uploadProgress.show = true
    uploadProgress.percentage = 0
    uploadProgress.status = ''
    uploadProgress.text = t('upload.progress.uploading')
    uploadProgress.stage = 0
    
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    formData.append('fileType', uploadForm.fileType)
    if (uploadForm.description) {
      formData.append('description', uploadForm.description)
    }
    
    const res = await uploadGenomeFile(formData)
    
    uploadProgress.percentage = 100
    uploadProgress.status = 'success'
    uploadProgress.text = t('upload.progress.success')
    
    ElMessage.success(t('upload.messages.uploadSuccess'))
    handleReset()
    await refreshFiles()
    
    try {
      await ElMessageBox.confirm(t('upload.confirmAnalyze'), t('upload.confirmAnalyzeTitle'), {
        confirmButtonText: t('upload.analyzeNow'),
        cancelButtonText: t('upload.analyzeLater'),
        type: 'success'
      })
      await startAnalysis(res.data.fileId)
    } catch {
      // 用户选择稍后分析
    }
  } catch (error) {
    console.error('Upload failed:', error)
    uploadProgress.status = 'exception'
    
    let errorMessage = t('upload.messages.uploadFailed')
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.response?.status === 413) {
      errorMessage = t('upload.messages.fileSizeExceeded')
    } else if (error.response?.status >= 500) {
      errorMessage = t('upload.messages.serverError')
    }
    
    uploadProgress.text = errorMessage
    ElMessage.error(errorMessage)
  } finally {
    uploading.value = false
    setTimeout(() => {
      uploadProgress.show = false
    }, 3000)
  }
}

// MAG 上传
const handleMagUpload = async () => {
  if (magFileList.value.length === 0) {
    ElMessage.warning(t('upload.messages.selectMagFiles'))
    return
  }
  
  try {
    uploading.value = true
    uploadProgress.show = true
    uploadProgress.percentage = 0
    uploadProgress.status = ''
    uploadProgress.text = t('upload.progress.magUploading')
    uploadProgress.stage = 1
    
    const formData = new FormData()
    
    for (const file of magFileList.value) {
      formData.append('files', file)
    }
    
    if (uploadForm.magName) {
      formData.append('magName', uploadForm.magName)
    }
    if (uploadForm.description) {
      formData.append('description', uploadForm.description)
    }
    formData.append('autoAnalyze', 'true')
    
    uploadProgress.percentage = 20
    uploadProgress.text = t('upload.progress.processing')
    
    const res = await uploadMag(formData)
    
    uploadProgress.percentage = 50
    uploadProgress.text = t('upload.progress.magProcessing')
    uploadProgress.stage = 1
    
    ElMessage.success(t('upload.messages.magUploadSuccess', { count: res.data.fileCount }))
    
    if (res.data.task) {
      uploadProgress.percentage = 100
      uploadProgress.status = 'success'
      uploadProgress.text = t('upload.progress.taskCreated', { id: res.data.task.taskId })
      
      handleReset()
      
      try {
        await ElMessageBox.confirm(
          t('upload.magTaskCreated'),
          t('upload.magTaskCreatedTitle'),
          {
            confirmButtonText: t('upload.viewProgress'),
            cancelButtonText: t('upload.stayHere'),
            type: 'success'
          }
        )
        router.push('/history')
      } catch {
        // 用户选择留在此页
      }
    }
    
  } catch (error) {
    console.error('MAG upload failed:', error)
    uploadProgress.status = 'exception'
    
    let errorMessage = t('upload.messages.magUploadFailed')
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    }
    
    uploadProgress.text = errorMessage
    ElMessage.error(errorMessage)
  } finally {
    uploading.value = false
    setTimeout(() => {
      if (uploadProgress.status !== 'exception') {
        uploadProgress.show = false
      }
    }, 5000)
  }
}

// 开始分析
const startAnalysis = async (fileId) => {
  try {
    await createTask({ fileId, analysisType: 'arg' })
    ElMessage.success(t('upload.messages.taskCreated'))
    router.push('/history')
  } catch (error) {
    console.error('Failed to create task:', error)
    ElMessage.error(t('upload.messages.createTaskFailed'))
  }
}

// 重置表单
const handleReset = () => {
  uploadForm.file = null
  uploadForm.fileType = 'auto-detect'
  uploadForm.description = ''
  uploadForm.textContent = ''
  uploadForm.magName = ''
  magFileList.value = []
  magFolderName.value = ''
  magUploadMode.value = 'folder'
  inputType.value = 'sequence'
  uploadRef.value?.clearFiles()
  magUploadRef.value?.clearFiles()
}

// 刷新文件列表
const refreshFiles = async () => {
  try {
    loadingFiles.value = true
    const res = await getUserFiles()
    files.value = res.data
  } catch (error) {
    console.error('Failed to get files:', error)
    files.value = []
  } finally {
    loadingFiles.value = false
  }
}

// 分析文件
const handleAnalyze = async (row) => {
  try {
    await ElMessageBox.confirm(t('upload.confirmAnalyzeFile'), t('upload.confirmAnalyzeFileTitle'), {
      confirmButtonText: t('upload.startAnalyze'),
      cancelButtonText: t('common.cancel'),
      type: 'info'
    })
    await startAnalysis(row.fileId)
  } catch {
    // 用户取消
  }
}

// 删除文件
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(t('upload.confirmDelete'), t('upload.confirmDeleteTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    await deleteFile(row.fileId)
    ElMessage.success(t('upload.messages.fileDeleted'))
    await refreshFiles()
  } catch {
    // 用户取消或删除失败
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'UPLOADED': 'success',
    'PROCESSING': 'warning',
    'FAILED': 'danger',
    'DELETED': 'info'
  }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    'UPLOADED': t('upload.status.uploaded'),
    'PROCESSING': t('upload.status.processing'),
    'FAILED': t('upload.status.failed'),
    'DELETED': t('upload.status.deleted')
  }
  return texts[status] || status
}

onMounted(() => {
  refreshFiles()
})
</script>

<style scoped>
.upload-container {
  max-width: 1200px;
  margin: 0 auto;
}

:deep(.el-card) {
  background: linear-gradient(135deg, rgba(10, 25, 47, 0.8) 0%, rgba(17, 34, 64, 0.8) 100%);
  border: 1px solid rgba(0, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 255, 255, 0.1);
}

:deep(.el-card__header) {
  background: rgba(0, 255, 255, 0.05);
  border-bottom: 1px solid rgba(0, 255, 255, 0.2);
  color: #00ffff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.upload-form {
  max-width: 800px;
}

.upload-content-wrapper {
  width: 100%;
}

:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.8);
}

:deep(.el-input__wrapper) {
  background: rgba(0, 255, 255, 0.05);
  border: 1px solid rgba(0, 255, 255, 0.2);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(0, 255, 255, 0.5);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

:deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-select__wrapper) {
  background: rgba(0, 255, 255, 0.05);
  border: 1px solid rgba(0, 255, 255, 0.2);
  box-shadow: none;
}

:deep(.el-select__wrapper:hover),
:deep(.el-select__wrapper.is-focused) {
  border-color: rgba(0, 255, 255, 0.5);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

:deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

:deep(.el-select__selected-item) {
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-button--default) {
  background: rgba(0, 255, 255, 0.1);
  border: 1px solid rgba(0, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.8);
}

:deep(.el-button--default:hover) {
  background: rgba(0, 255, 255, 0.2);
  border-color: rgba(0, 255, 255, 0.5);
  color: #00ffff;
}

:deep(.el-textarea__inner) {
  background: rgba(0, 255, 255, 0.05);
  border: 1px solid rgba(0, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-textarea__inner:hover),
:deep(.el-textarea__inner:focus) {
  border-color: rgba(0, 255, 255, 0.5);
}

/* Radio Button 样式 */
:deep(.el-radio-button__inner) {
  background: rgba(0, 255, 255, 0.05);
  border-color: rgba(0, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.8);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(0, 255, 255, 0.3);
  border-color: #00ffff;
  color: #00ffff;
  box-shadow: -1px 0 0 0 #00ffff;
}

.upload-demo {
  width: 100%;
}

:deep(.el-upload-dragger) {
  padding: 40px;
  background: rgba(0, 255, 255, 0.05);
  border: 2px dashed rgba(0, 255, 255, 0.3);
}

:deep(.el-upload-dragger:hover) {
  border-color: rgba(0, 255, 255, 0.6);
  background: rgba(0, 255, 255, 0.08);
}

:deep(.el-upload__text) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.el-upload__text em) {
  color: #00ffff;
}

:deep(.el-upload__tip) {
  color: rgba(255, 255, 255, 0.6);
}

/* MAG 上传模式切换 */
.mag-upload-mode {
  margin-bottom: 16px;
}

/* 文件夹上传区域 */
.folder-upload-area {
  padding: 40px 20px;
  background: rgba(0, 255, 255, 0.05);
  border: 2px dashed rgba(103, 194, 58, 0.4);
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.folder-upload-area:hover {
  border-color: rgba(103, 194, 58, 0.7);
  background: rgba(103, 194, 58, 0.08);
}

.folder-upload-area.drag-over {
  border-color: #67C23A;
  background: rgba(103, 194, 58, 0.15);
  transform: scale(1.01);
}

.folder-upload-area .folder-icon {
  font-size: 48px;
  color: rgba(103, 194, 58, 0.7);
  margin-bottom: 12px;
}

.folder-upload-area .upload-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  margin-bottom: 8px;
}

.folder-upload-area .upload-tip {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.mag-file-preview {
  margin-top: 16px;
  padding: 12px;
  background: rgba(0, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(0, 255, 255, 0.2);
}

.mag-file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.mag-file-header .el-icon {
  margin-right: 6px;
  color: #67C23A;
}

.mag-file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mag-file-tag {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mag-info-alert {
  margin-top: 16px;
}

.mag-info-alert :deep(.el-alert__title) {
  font-size: 13px;
  line-height: 1.6;
}

.mag-info-content p {
  margin: 4px 0;
  font-weight: normal;
}

.progress-card {
  margin-top: 24px;
}

.progress-content {
  padding: 20px;
}

.progress-text {
  margin-top: 16px;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
}

.stage-info {
  margin-top: 20px;
}

:deep(.el-steps--simple) {
  background: rgba(0, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px 20px;
}

:deep(.el-step__title) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

:deep(.el-step__title.is-process) {
  color: #00ffff;
  font-weight: 600;
}

:deep(.el-step__title.is-finish) {
  color: #67C23A;
}

.files-card {
  margin-top: 24px;
}

:deep(.el-table) {
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
}

:deep(.el-table th.el-table__cell) {
  background: rgba(0, 255, 255, 0.1);
  color: #00ffff;
  border-bottom: 1px solid rgba(0, 255, 255, 0.3);
}

:deep(.el-table tr) {
  background: transparent;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0, 255, 255, 0.1);
}

:deep(.el-table__body tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.text-upload-tip {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  margin-top: 8px;
}
</style>
