<template>
  <div class="introduction-container">
    <!-- Hero 区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-decoration">◆</span>
          {{ $t('introduction.hero.title') }}
          <span class="title-decoration">◆</span>
        </h1>
        <p class="hero-subtitle">
          {{ $t('introduction.hero.subtitle') }}
        </p>
        <p class="hero-description">
          {{ $t('introduction.hero.description') }}
        </p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">14</span>
            <span class="stat-label">{{ isEnglish ? 'ARG Categories' : 'ARG 分类类型' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">90%+</span>
            <span class="stat-label">{{ isEnglish ? 'Model Accuracy' : '模型准确率' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">3</span>
            <span class="stat-label">{{ isEnglish ? 'Input Methods' : '输入方式' }}</span>
          </div>
        </div>
      </div>
      <div class="hero-decoration">
        <div class="dna-animation">
          <span v-for="n in 8" :key="n" class="dna-strand"></span>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 第一部分：抗性基因背景知识 -->
      <section class="section-block">
        <div class="section-header">
          <div class="section-number">01</div>
          <div class="section-title-group">
            <h2 class="section-title">{{ $t('introduction.background.title') }}</h2>
            <p class="section-subtitle">{{ $t('introduction.background.subtitle') }}</p>
          </div>
        </div>

        <el-row :gutter="40">
          <el-col :span="24">
            <div class="definition-card">
              <div class="definition-icon">
                <el-icon :size="32"><Warning /></el-icon>
              </div>
              <div class="definition-content">
                <h3>{{ $t('introduction.background.definition.title') }}</h3>
                <p>{{ $t('introduction.background.definition.content') }}</p>
              </div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="24" class="mechanism-row">
          <el-col :xs="24" :sm="12" :lg="6">
            <div class="mechanism-card">
              <div class="mechanism-icon enzyme">
                <el-icon :size="28"><Monitor /></el-icon>
              </div>
              <h4>{{ $t('introduction.background.mechanisms.enzyme.title') }}</h4>
              <p>{{ $t('introduction.background.mechanisms.enzyme.desc') }}</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <div class="mechanism-card">
              <div class="mechanism-icon pump">
                <el-icon :size="28"><Top /></el-icon>
              </div>
              <h4>{{ $t('introduction.background.mechanisms.pump.title') }}</h4>
              <p>{{ $t('introduction.background.mechanisms.pump.desc') }}</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <div class="mechanism-card">
              <div class="mechanism-icon target">
                <el-icon :size="28"><Aim /></el-icon>
              </div>
              <h4>{{ $t('introduction.background.mechanisms.target.title') }}</h4>
              <p>{{ $t('introduction.background.mechanisms.target.desc') }}</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <div class="mechanism-card">
              <div class="mechanism-icon wall">
                <el-icon :size="28"><Grid /></el-icon>
              </div>
              <h4>{{ $t('introduction.background.mechanisms.wall.title') }}</h4>
              <p>{{ $t('introduction.background.mechanisms.wall.desc') }}</p>
            </div>
          </el-col>
        </el-row>

        <div class="importance-section">
          <h3 class="subsection-title">
            <el-icon><Lightning /></el-icon>
            {{ $t('introduction.background.importance.title') }}
          </h3>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :lg="6">
              <div class="importance-item">
                <div class="importance-icon">
                  <el-icon :size="24"><FirstAidKit /></el-icon>
                </div>
                <h4>{{ $t('introduction.background.importance.clinical.title') }}</h4>
                <p>{{ $t('introduction.background.importance.clinical.desc') }}</p>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <div class="importance-item">
                <div class="importance-icon">
                  <el-icon :size="24"><DataLine /></el-icon>
                </div>
                <h4>{{ $t('introduction.background.importance.epidemiology.title') }}</h4>
                <p>{{ $t('introduction.background.importance.epidemiology.desc') }}</p>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <div class="importance-item">
                <div class="importance-icon">
                  <el-icon :size="24"><Box /></el-icon>
                </div>
                <h4>{{ $t('introduction.background.importance.drug.title') }}</h4>
                <p>{{ $t('introduction.background.importance.drug.desc') }}</p>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <div class="importance-item">
                <div class="importance-icon">
                  <el-icon :size="24"><Odometer /></el-icon>
                </div>
                <h4>{{ $t('introduction.background.importance.environment.title') }}</h4>
                <p>{{ $t('introduction.background.importance.environment.desc') }}</p>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="arg-categories">
          <h3 class="subsection-title">
            <el-icon><CollectionTag /></el-icon>
            {{ $t('introduction.background.categories.title') }}
          </h3>
          <div class="categories-grid">
            <div v-for="category in argCategories" :key="category.name" class="category-tag">
              <span class="category-name">{{ category.name }}</span>
              <span class="category-desc">{{ category.desc }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 第二部分：网站流程 -->
      <section class="section-block">
        <div class="section-header">
          <div class="section-number">02</div>
          <div class="section-title-group">
            <h2 class="section-title">{{ $t('introduction.workflow.title') }}</h2>
            <p class="section-subtitle">{{ $t('introduction.workflow.subtitle') }}</p>
          </div>
        </div>

        <div class="workflow-diagram">
          <div class="workflow-step" v-for="(step, index) in workflowSteps" :key="index">
            <div class="step-connector" v-if="index > 0">
              <svg viewBox="0 0 40 20" class="connector-svg">
                <path d="M0,10 L25,10 L25,5 L40,10 L25,15 L25,10" fill="none" stroke="currentColor" stroke-width="2"/>
              </svg>
            </div>
            <div class="step-card" :class="'step-' + (index + 1)">
              <div class="step-number">{{ index + 1 }}</div>
              <div class="step-icon">
                <el-icon :size="32"><component :is="step.icon" /></el-icon>
              </div>
              <h4 class="step-title">{{ step.title }}</h4>
              <p class="step-desc">{{ step.desc }}</p>
              <div class="step-detail" v-if="step.detail">
                <el-tag v-for="tag in step.detail" :key="tag" size="small" type="info">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="input-methods">
          <h3 class="subsection-title">
            <el-icon><Upload /></el-icon>
            {{ $t('introduction.workflow.inputs.title') }}
          </h3>
          <el-row :gutter="24">
            <el-col :xs="24" :lg="8">
              <div class="input-card">
                <div class="input-icon text">
                  <el-icon :size="36"><Edit /></el-icon>
                </div>
                <h4>{{ $t('upload.inputTypes.sequence') }}</h4>
                <p>{{ isEnglish ? 'Paste protein sequence directly, supports FASTA format' : '直接粘贴蛋白质序列，支持 FASTA 格式' }}</p>
                <el-tag type="success" size="small">{{ isEnglish ? 'Quick Analysis' : '快速分析' }}</el-tag>
              </div>
            </el-col>
            <el-col :xs="24" :lg="8">
              <div class="input-card">
                <div class="input-icon file">
                  <el-icon :size="36"><Document /></el-icon>
                </div>
                <h4>{{ isEnglish ? 'Upload FASTA File' : '上传 FASTA 文件' }}</h4>
                <p>{{ isEnglish ? 'Batch upload protein sequence files for large-scale analysis' : '批量上传蛋白质序列文件，进行大规模分析' }}</p>
                <el-tag type="warning" size="small">{{ isEnglish ? 'Batch Processing' : '批量处理' }}</el-tag>
              </div>
            </el-col>
            <el-col :xs="24" :lg="8">
              <div class="input-card">
                <div class="input-icon mag">
                  <el-icon :size="36"><FolderOpened /></el-icon>
                </div>
                <h4>{{ isEnglish ? 'Upload MAG Files' : '上传 MAG 文件' }}</h4>
                <p>{{ isEnglish ? 'Upload Metagenome-Assembled Genomes (MAG) for automatic gene prediction' : '上传宏基因组组装基因组 (MAG)，自动进行基因预测' }}</p>
                <el-tag type="primary" size="small">{{ isEnglish ? 'Prodigal Preprocessing' : 'Prodigal 预处理' }}</el-tag>
              </div>
            </el-col>
          </el-row>
        </div>
      </section>

      <!-- 第三部分：模型介绍 -->
      <section class="section-block">
        <div class="section-header">
          <div class="section-number">03</div>
          <div class="section-title-group">
            <h2 class="section-title">{{ $t('introduction.model.title') }}</h2>
            <p class="section-subtitle">{{ $t('introduction.model.subtitle') }}</p>
          </div>
        </div>

        <div class="model-overview">
          <div class="model-intro">
            <h3>{{ $t('introduction.model.overview.title') }}</h3>
            <p>{{ $t('introduction.model.overview.content') }}</p>
          </div>

          <div class="architecture-diagram">
            <div class="arch-layer input-layer">
              <div class="layer-label">{{ isEnglish ? 'Input Layer' : '输入层' }}</div>
              <div class="layer-content">
                <div class="seq-box">{{ isEnglish ? 'Protein Seq' : '蛋白质序列' }}</div>
                <div class="seq-box">...</div>
                <div class="seq-box">{{ isEnglish ? 'Protein Seq' : '蛋白质序列' }}</div>
              </div>
            </div>
            <div class="arch-arrow">
              <el-icon><DArrowRight /></el-icon>
            </div>
            <div class="arch-layer encoding-layer">
              <div class="layer-label">{{ isEnglish ? 'Encoding Layer' : '编码层' }}</div>
              <div class="layer-content">
                <div class="encoding-box">K-mer</div>
                <div class="encoding-box">{{ isEnglish ? 'Codon' : '密码子' }}</div>
                <div class="encoding-box">{{ isEnglish ? 'AA Embedding' : '氨基酸嵌入' }}</div>
              </div>
            </div>
            <div class="arch-arrow">
              <el-icon><DArrowRight /></el-icon>
            </div>
            <div class="arch-layer lstm-layer">
              <div class="layer-label">BiLSTM</div>
              <div class="layer-content lstm-content">
                <div class="lstm-box forward">
                  <span>LSTM →</span>
                </div>
                <div class="lstm-merge">
                  <el-icon><Connection /></el-icon>
                </div>
                <div class="lstm-box backward">
                  <span>← LSTM</span>
                </div>
              </div>
            </div>
            <div class="arch-arrow">
              <el-icon><DArrowRight /></el-icon>
            </div>
            <div class="arch-layer classifier-layer">
              <div class="layer-label">{{ isEnglish ? 'Classifier' : '分类层' }}</div>
              <div class="layer-content">
                <div class="classifier-box binary">
                  <span>{{ isEnglish ? 'Binary' : '二分类' }}</span>
                  <small>ARG / Non-ARG</small>
                </div>
                <div class="classifier-box multi">
                  <span>{{ isEnglish ? 'Multi-class' : '多分类' }}</span>
                  <small>{{ isEnglish ? '14 ARG Types' : '14 种 ARG 类型' }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-row :gutter="30" class="model-features">
          <el-col :xs="24" :lg="12">
            <div class="feature-card">
              <div class="feature-header">
                <div class="feature-icon binary-icon">
                  <el-icon :size="24"><CircleCheck /></el-icon>
                </div>
                <h3>{{ $t('introduction.model.binary.title') }}</h3>
              </div>
              <div class="feature-content">
                <p class="feature-desc">{{ $t('introduction.model.binary.desc') }}</p>
                <div class="feature-specs">
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.binary.specs.task') }}</span>
                    <span class="spec-value">{{ isEnglish ? 'Binary Classification' : '二分类 (Binary)' }}</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.binary.specs.output') }}</span>
                    <span class="spec-value">{{ isEnglish ? 'ARG Probability (0-1)' : 'ARG 概率 (0-1)' }}</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.binary.specs.structure') }}</span>
                    <span class="spec-value">BiLSTM + Global Pooling + FC</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.binary.specs.accuracy') }}</span>
                    <span class="spec-value">> 90%</span>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :lg="12">
            <div class="feature-card">
              <div class="feature-header">
                <div class="feature-icon multi-icon">
                  <el-icon :size="24"><Grid /></el-icon>
                </div>
                <h3>{{ $t('introduction.model.multi.title') }}</h3>
              </div>
              <div class="feature-content">
                <p class="feature-desc">{{ $t('introduction.model.multi.desc') }}</p>
                <div class="feature-specs">
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.multi.specs.task') }}</span>
                    <span class="spec-value">{{ isEnglish ? 'Multi-class Classification' : '多分类 (Multi-class)' }}</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.multi.specs.output') }}</span>
                    <span class="spec-value">{{ isEnglish ? '14 Class Probabilities' : '14 类别概率分布' }}</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.multi.specs.structure') }}</span>
                    <span class="spec-value">BiLSTM + Global Pooling + FC</span>
                  </div>
                  <div class="spec-item">
                    <span class="spec-label">{{ $t('introduction.model.multi.specs.categories') }}</span>
                    <span class="spec-value">{{ isEnglish ? '14 Classes' : '14 类' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <div class="encoding-section">
          <h3 class="subsection-title">
            <el-icon><Key /></el-icon>
            {{ $t('introduction.model.encoding.title') }}
          </h3>
          <el-row :gutter="24">
            <el-col :xs="24" :lg="8">
              <div class="encoding-card">
                <div class="encoding-icon">
                  <el-icon :size="28"><Menu /></el-icon>
                </div>
                <h4>K-mer {{ isEnglish ? 'Encoding' : '编码' }}</h4>
                <p>{{ isEnglish ? 'Extract consecutive k amino acid combination features from protein sequences to capture local sequence patterns' : '提取蛋白质序列中连续的 k 个氨基酸组合特征，捕获局部序列模式' }}</p>
              </div>
            </el-col>
            <el-col :xs="24" :lg="8">
              <div class="encoding-card">
                <div class="encoding-icon">
                  <el-icon :size="28"><Connection /></el-icon>
                </div>
                <h4>{{ isEnglish ? 'Codon Encoding' : '密码子编码' }}</h4>
                <p>{{ isEnglish ? 'Convert nucleotide triplet codons into feature vectors, preserving genetic information' : '将核苷酸三联体密码子转换为特征向量，保留遗传信息' }}</p>
              </div>
            </el-col>
            <el-col :xs="24" :lg="8">
              <div class="encoding-card">
                <div class="encoding-icon">
                  <el-icon :size="28"><Star /></el-icon>
                </div>
                <h4>{{ isEnglish ? 'Amino Acid Embedding' : '氨基酸嵌入' }}</h4>
                <p>{{ isEnglish ? 'Use embedding layers to map amino acid symbols to dense vectors, learning semantic information' : '使用嵌入层将氨基酸符号映射为密集向量，学习语义信息' }}</p>
              </div>
            </el-col>
          </el-row>
        </div>
      </section>

      <!-- 第四部分：技术栈 -->
      <section class="section-block">
        <div class="section-header">
          <div class="section-number">04</div>
          <div class="section-title-group">
            <h2 class="section-title">{{ $t('introduction.tech.title') }}</h2>
            <p class="section-subtitle">{{ $t('introduction.tech.subtitle') }}</p>
          </div>
        </div>

        <div class="tech-stack">
          <div class="tech-layer">
            <div class="tech-label">{{ $t('introduction.tech.frontend') }}</div>
            <div class="tech-items">
              <el-tag v-for="tech in frontendTech" :key="tech" type="success" effect="dark">
                {{ tech }}
              </el-tag>
            </div>
          </div>
          <div class="tech-arrow">
            <el-icon><ArrowDown /></el-icon>
          </div>
          <div class="tech-layer">
            <div class="tech-label">{{ $t('introduction.tech.backend') }}</div>
            <div class="tech-items">
              <el-tag v-for="tech in backendTech" :key="tech" type="primary" effect="dark">
                {{ tech }}
              </el-tag>
            </div>
          </div>
          <div class="tech-arrow">
            <el-icon><ArrowDown /></el-icon>
          </div>
          <div class="tech-layer">
            <div class="tech-label">{{ $t('introduction.tech.ml') }}</div>
            <div class="tech-items">
              <el-tag v-for="tech in mlTech" :key="tech" type="warning" effect="dark">
                {{ tech }}
              </el-tag>
            </div>
          </div>
          <div class="tech-arrow">
            <el-icon><ArrowDown /></el-icon>
          </div>
          <div class="tech-layer">
            <div class="tech-label">{{ $t('introduction.tech.infra') }}</div>
            <div class="tech-items">
              <el-tag v-for="tech in infraTech" :key="tech" type="danger" effect="dark">
                {{ tech }}
              </el-tag>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Warning, Monitor, Top, Aim, Grid, Lightning,
  FirstAidKit, DataLine, Odometer, CollectionTag,
  Upload, Edit, Document, FolderOpened, CircleCheck,
  Setting, Cpu, DataAnalysis,
  Key, Menu, Connection, Star,
  DArrowRight, ArrowDown, Box
} from '@element-plus/icons-vue'

const { t, locale } = useI18n()

// 判断当前是否为英文
const isEnglish = computed(() => locale.value === 'en')

// ARG 分类数据
const argCategories = [
  { name: 'MLS', desc: '大环内酯类、林可霉素、链阳菌素' },
  { name: 'Aminoglycoside', desc: '氨基糖苷类抗生素' },
  { name: 'Bacitracin', desc: '杆菌肽类' },
  { name: 'Beta-lactam', desc: 'β-内酰胺类' },
  { name: 'Chloramphenicol', desc: '氯霉素类' },
  { name: 'Fosfomycin', desc: '磷霉素类' },
  { name: 'Glycopeptide', desc: '糖肽类' },
  { name: 'Multidrug', desc: '多重耐药' },
  { name: 'Polymyxin', desc: '多粘菌素类' },
  { name: 'Quinolone', desc: '喹诺酮类' },
  { name: 'Sulfonamide', desc: '磺胺类' },
  { name: 'Tetracycline', desc: '四环素类' },
  { name: 'Trimethoprim', desc: '甲氧苄啶类' },
  { name: 'Others', desc: '其他类型' }
]

// 工作流程步骤
const workflowSteps = computed(() => [
  {
    title: t('introduction.workflow.steps.upload.title'),
    desc: t('introduction.workflow.steps.upload.desc'),
    icon: 'Upload',
    detail: ['FASTA', 'MAG', isEnglish.value ? 'Paste' : '粘贴']
  },
  {
    title: t('introduction.workflow.steps.process.title'),
    desc: t('introduction.workflow.steps.process.desc'),
    icon: 'Setting',
    detail: ['Prodigal', isEnglish.value ? 'QC' : '质量控制']
  },
  {
    title: t('introduction.workflow.steps.analyze.title'),
    desc: t('introduction.workflow.steps.analyze.desc'),
    icon: 'Cpu',
    detail: [isEnglish.value ? 'Binary' : '二分类', isEnglish.value ? 'Multi-class' : '多分类']
  },
  {
    title: t('introduction.workflow.steps.output.title'),
    desc: t('introduction.workflow.steps.output.desc'),
    icon: 'DataAnalysis',
    detail: [isEnglish.value ? 'Charts' : '统计图表', 'TSV', 'BLAST']
  }
])

// 技术栈
const frontendTech = ['Vue 3', 'Element Plus', 'Pinia', 'ECharts', 'Axios']
const backendTech = ['Spring Boot', 'JDK 17', 'MyBatis', 'MySQL', 'Redis']
const mlTech = ['PyTorch', 'BiLSTM', 'Deep Learning', 'Docker']
const infraTech = ['Docker', 'Linux', 'BLAST', 'Prodigal']
</script>

<style scoped>
.introduction-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

/* Hero 区域 */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60px 40px;
  background: linear-gradient(135deg, var(--theme-bg) 0%, var(--theme-bg-2) 40%, var(--theme-bg-3) 100%);
  border-radius: 20px;
  margin-bottom: 50px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--theme-border-2);
}

.hero-content {
  flex: 1;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--theme-text);
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-decoration {
  color: var(--theme-text);
  font-size: 24px;
}

.hero-subtitle {
  font-size: 20px;
  color: var(--theme-text);
  margin: 0 0 20px 0;
  font-weight: 500;
}

.hero-description {
  font-size: 16px;
  color: var(--theme-text-muted);
  line-height: 1.8;
  max-width: 600px;
  margin: 0 0 32px 0;
}

.hero-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--theme-text);
}

.stat-label {
  font-size: 14px;
  color: var(--theme-text-muted);
}

.hero-decoration {
  position: absolute;
  right: 60px;
  top: 50%;
  transform: translateY(-50%);
}

.dna-animation {
  display: flex;
  gap: 8px;
}

.dna-strand {
  width: 8px;
  height: 120px;
  background: linear-gradient(to top, transparent, rgba(120, 90, 70, 0.4), transparent);
  animation: dnaWave 1.5s ease-in-out infinite;
}

.dna-strand:nth-child(odd) {
  animation-delay: 0.2s;
}

@keyframes dnaWave {
  0%, 100% { transform: scaleY(1); opacity: 0.5; }
  50% { transform: scaleY(1.3); opacity: 1; }
}

/* 内容区域 */
.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 60px;
}

.section-block {
  background: linear-gradient(135deg, var(--theme-bg) 0%, var(--theme-bg-2) 60%, var(--theme-bg-3) 100%);
  border-radius: 16px;
  padding: 40px;
  border: 1px solid var(--theme-border-2);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(120, 90, 70, 0.25);
}

.section-number {
  font-size: 48px;
  font-weight: 800;
  color: var(--theme-text-faint);
  line-height: 1;
}

.section-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--theme-text);
  margin: 0 0 8px 0;
}

.section-subtitle {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 2px;
}

/* 定义卡片 */
.definition-card {
  display: flex;
  gap: 24px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  margin-bottom: 30px;
}

.definition-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #ff6b6b, #ffa502);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.definition-content h3 {
  font-size: 20px;
  color: var(--theme-text);
  margin: 0 0 12px 0;
}

.definition-content p {
  font-size: 15px;
  color: var(--theme-text-muted);
  line-height: 1.8;
  margin: 0;
}

/* 机制卡片 */
.mechanism-row {
  margin-bottom: 30px;
}

.mechanism-card {
  padding: 24px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.15);
  text-align: center;
  height: 100%;
  transition: all 0.3s;
}

.mechanism-card:hover {
  background: rgba(240, 220, 202, 0.7);
  border-color: rgba(120, 90, 70, 0.3);
  transform: translateY(-4px);
}

.mechanism-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.mechanism-icon.enzyme { background: linear-gradient(135deg, #667eea, #764ba2); }
.mechanism-icon.pump { background: linear-gradient(135deg, #f093fb, #f5576c); }
.mechanism-icon.target { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.mechanism-icon.wall { background: linear-gradient(135deg, #43e97b, #38f9d7); }

.mechanism-icon {
  color: #fff;
}

.mechanism-card h4 {
  font-size: 18px;
  color: var(--theme-text);
  margin: 0 0 12px 0;
}

.mechanism-card p {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0;
  line-height: 1.6;
}

/* 重要性区块 */
.importance-section {
  margin-bottom: 30px;
}

.subsection-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  color: var(--theme-text);
  margin: 0 0 24px 0;
}

.subsection-title .el-icon {
  color: var(--theme-status-success);
}

.importance-item {
  padding: 24px;
  background: var(--theme-bg-overlay);
  border-radius: 12px;
  border: 1px solid var(--theme-border-2);
  text-align: center;
  height: 100%;
  transition: all 0.3s;
}

.importance-item:hover {
  background: var(--theme-bg-overlay-3);
  border-color: var(--theme-border-3);
}

.importance-icon {
  width: 48px;
  height: 48px;
  background: var(--theme-status-success);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: var(--theme-text-on-dark);
}

.importance-item h4 {
  font-size: 16px;
  color: var(--theme-text);
  margin: 0 0 8px 0;
}

.importance-item p {
  font-size: 13px;
  color: var(--theme-text-muted);
  margin: 0;
  line-height: 1.6;
}

/* ARG 分类标签 */
.arg-categories {
  margin-top: 30px;
}

.categories-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-tag {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  min-width: 140px;
}

.category-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text);
  margin-bottom: 4px;
}

.category-desc {
  font-size: 12px;
  color: var(--theme-text-muted);
}

/* 工作流程图 */
.workflow-diagram {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 30px 0;
  overflow-x: auto;
}

.workflow-step {
  display: flex;
  align-items: flex-start;
  flex: 1;
}

.step-connector {
  width: 60px;
  display: flex;
  align-items: center;
  color: var(--theme-text-lighter);
}

.connector-svg {
  width: 40px;
  height: 20px;
}

.step-card {
  flex: 1;
  padding: 24px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  text-align: center;
  position: relative;
  min-width: 180px;
}

.step-1 { border-top: 3px solid var(--theme-status-success); }
.step-2 { border-top: 3px solid #00ff88; }
.step-3 { border-top: 3px solid #ffaa00; }
.step-4 { border-top: 3px solid #ff6b6b; }

.step-number {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 28px;
  background: var(--theme-status-success);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--theme-text-on-dark);
}

.step-icon {
  margin: 16px 0;
  color: var(--theme-status-success);
}

.step-title {
  font-size: 18px;
  color: var(--theme-text);
  margin: 0 0 8px 0;
}

.step-desc {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0 0 12px 0;
}

.step-detail {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

/* 输入方式卡片 */
.input-methods {
  margin-top: 40px;
}

.input-card {
  padding: 30px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  text-align: center;
  height: 100%;
  transition: all 0.3s;
}

.input-card:hover {
  background: rgba(120, 90, 70, 0.12);
  border-color: rgba(120, 90, 70, 0.35);
  transform: translateY(-4px);
}

.input-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.input-icon.text { background: linear-gradient(135deg, #00ff88, #00cc6a); }
.input-icon.file { background: linear-gradient(135deg, #ffaa00, #ff6b00); }
.input-icon.mag { background: var(--theme-status-success); }

.input-icon {
  color: var(--theme-text-on-dark);
}

.input-card h4 {
  font-size: 18px;
  color: var(--theme-text);
  margin: 0 0 12px 0;
}

.input-card p {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0 0 16px 0;
  line-height: 1.6;
}

/* 模型架构图 */
.model-overview {
  margin-bottom: 40px;
}

.model-intro {
  text-align: center;
  padding: 30px;
  background: var(--theme-bg-overlay-2);
  border-radius: 12px;
  margin-bottom: 40px;
}

.model-intro h3 {
  font-size: 24px;
  color: var(--theme-text);
  margin: 0 0 16px 0;
}

.model-intro p {
  font-size: 16px;
  color: var(--theme-text-muted);
  line-height: 1.8;
  margin: 0;
}

.architecture-diagram {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 30px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  overflow-x: auto;
}

.arch-layer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.layer-label {
  font-size: 12px;
  color: var(--theme-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.layer-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.seq-box, .encoding-box {
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(120, 90, 70, 0.25), rgba(120, 90, 70, 0.2));
  border-radius: 6px;
  font-size: 12px;
  color: #fff;
  white-space: nowrap;
}

.encoding-layer .layer-content {
  flex-direction: row;
}

.lstm-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lstm-box {
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 12px;
  color: #fff;
}

.lstm-box.forward { background: linear-gradient(135deg, #00ff88, #00cc6a); }
.lstm-box.backward { background: linear-gradient(135deg, #ff6b6b, #ff4757); }

.lstm-merge {
  color: var(--theme-text);
}

.classifier-box {
  padding: 12px 24px;
  border-radius: 8px;
  text-align: center;
}

.classifier-box.binary { background: linear-gradient(135deg, #ffaa00, #ff6b00); }
.classifier-box.multi { background: linear-gradient(135deg, #9b59b6, #8e44ad); }

.classifier-box span {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.classifier-box small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}

.arch-arrow {
  color: var(--theme-text-muted);
  font-size: 24px;
  padding: 0 16px;
}

/* 模型特性卡片 */
.model-features {
  margin-bottom: 40px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  overflow: hidden;
  height: 100%;
}

.feature-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: rgba(120, 90, 70, 0.12);
  border-bottom: 1px solid rgba(120, 90, 70, 0.2);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.binary-icon { background: linear-gradient(135deg, #ffaa00, #ff6b00); }
.multi-icon { background: linear-gradient(135deg, #9b59b6, #8e44ad); }

.feature-icon {
  color: #fff;
}

.feature-header h3 {
  font-size: 20px;
  color: var(--theme-text);
  margin: 0;
}

.feature-content {
  padding: 24px;
}

.feature-desc {
  font-size: 15px;
  color: var(--theme-text-muted);
  margin: 0 0 20px 0;
}

.feature-specs {
  display: grid;
  gap: 12px;
}

.spec-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
}

.spec-label {
  font-size: 13px;
  color: var(--theme-text-muted);
}

.spec-value {
  font-size: 13px;
  color: var(--theme-text);
  font-weight: 500;
}

/* 编码方式卡片 */
.encoding-section {
  margin-top: 40px;
}

.encoding-card {
  padding: 30px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  text-align: center;
  height: 100%;
  transition: all 0.3s;
}

.encoding-card:hover {
  background: rgba(120, 90, 70, 0.12);
  border-color: rgba(120, 90, 70, 0.35);
}

.encoding-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: #fff;
}

.encoding-card h4 {
  font-size: 18px;
  color: var(--theme-text);
  margin: 0 0 12px 0;
}

.encoding-card p {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0;
  line-height: 1.6;
}

/* 技术栈 */
.tech-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.tech-layer {
  width: 100%;
  max-width: 600px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(120, 90, 70, 0.2);
  text-align: center;
}

.tech-label {
  font-size: 14px;
  color: var(--theme-text-muted);
  text-transform: uppercase;
  letter-spacing: 2px;
  margin-bottom: 16px;
}

.tech-items {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.tech-arrow {
  color: var(--theme-text-lighter);
  font-size: 24px;
  padding: 16px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    flex-direction: column;
    padding: 40px 24px;
    text-align: center;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-stats {
    justify-content: center;
  }

  .hero-decoration {
    display: none;
  }

  .section-block {
    padding: 24px;
  }

  .workflow-diagram {
    flex-direction: column;
    gap: 20px;
  }

  .workflow-step {
    width: 100%;
  }

  .step-connector {
    display: none;
  }

  .architecture-diagram {
    flex-direction: column;
    gap: 20px;
  }

  .arch-arrow {
    transform: rotate(90deg);
    padding: 10px 0;
  }
}
</style>
