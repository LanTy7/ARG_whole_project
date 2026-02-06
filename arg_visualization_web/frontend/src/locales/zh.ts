export default {
  app: {
    title: '抗性基因识别系统',
    name: 'ARG Identification'
  },
  nav: {
    home: '首页',
    upload: '文件上传',
    visualization: '结果可视化',
    history: '历史记录',
    introduction: '平台介绍',
    admin: '管理功能',
    login: '登录',
    logout: '退出'
  },
  // 侧边栏
  sidebar: {
    title: '抗性基因识别',
    subtitle: 'ARG Identification'
  },
  // 首页
  home: {
    welcome: '欢迎使用抗性基因检测系统',
    subtitle: '基于深度学习的抗性基因识别和可视化平台',
    startUpload: '开始上传文件',
    viewHistory: '查看历史记录',
    stats: {
      totalFiles: '上传文件数',
      completedTasks: '完成任务数',
      runningTasks: '运行中任务'
    }
  },
  // 上传页面
  upload: {
    title: '基因序列上传',
    inputType: '输入类型',
    inputTypes: {
      sequence: '蛋白质序列',
      file: 'FASTA 文件',
      mag: 'MAG 文件夹'
    },
    magName: 'MAG 名称',
    magNamePlaceholder: '可选，不填则自动生成',
    fileType: '文件类型',
    fileTypePlaceholder: '请选择文件类型',
    fileTypes: {
      autoDetect: '自动检测',
      fasta: 'FASTA',
      faa: 'FAA (蛋白质序列)'
    },
    description: '文件描述',
    descriptionPlaceholder: '请输入文件描述（可选）',
    uploadContent: '上传内容',
    sequencePlaceholder: '在此粘贴或输入蛋白质序列（FASTA格式）',
    sequenceTip: '文本内容将直接被上传到服务器进行分析',
    dragText: '将文件拖到此处，或',
    clickUpload: '点击上传',
    fileTip: '支持 .fasta / .fa / .faa 格式，文件大小不超过 500MB',
    magUploadMode: {
      folder: '选择文件夹',
      files: '选择多个文件'
    },
    magFolderText: '点击选择 MAG 文件夹，或将文件夹拖到此处',
    magFilesText: '点击选择多个文件，或将文件拖到此处',
    magFileTip: '支持 .fa / .fasta / .fna 格式',
    selected: '已选择',
    filesCount: '{count} 个文件',
    clear: '清空',
    moreFiles: '还有 {count} 个文件...',
    magInfo: {
      title: 'MAG 分析说明：',
      step1: '1. 上传包含原始核酸序列文件（.fa/.fasta/.fna）的文件夹',
      step2: '2. 系统将使用 Prodigal 进行基因预测，转换为蛋白质序列',
      step3: '3. 然后进行抗性基因（ARG）识别和分类'
    },
    uploading: '上传中...',
    uploadAndAnalyzeMag: '上传并分析 MAG',
    startUpload: '开始上传',
    reset: '重置',
    progress: {
      title: '上传进度',
      magTitle: 'MAG 分析进度',
      uploading: '正在上传文件...',
      magUploading: '正在上传 MAG 文件...',
      processing: '文件上传中...',
      magProcessing: 'MAG 分析任务已创建，正在处理...',
      success: '上传成功！',
      taskCreated: '分析任务已创建 (ID: {id})'
    },
    steps: {
      prodigal: 'Prodigal 预处理',
      arg: 'ARG 分析'
    },
    myFiles: '我的文件',
    refresh: '刷新',
    table: {
      filename: '文件名',
      fileType: '文件类型',
      fileSize: '文件大小',
      status: '状态',
      uploadTime: '上传时间',
      actions: '操作'
    },
    status: {
      uploaded: '已上传',
      processing: '处理中',
      failed: '失败',
      deleted: '已删除'
    },
    actions: {
      analyze: '分析',
      delete: '删除'
    },
    messages: {
      onlyOneFile: '每次只能上传一个文件',
      noValidFiles: '未找到有效的 FASTA 文件（.fa/.fasta/.fna）',
      selectedFiles: '已选择 {count} 个有效文件',
      enterText: '请输入要上传的文本内容',
      selectFile: '请选择要上传的文件',
      fileTooLarge: '文件大小 {size} MB 超过限制（最大 500 MB）',
      uploadSuccess: '文件上传成功',
      uploadFailed: '上传失败，请重试',
      serverError: '服务器错误，请稍后重试',
      fileSizeExceeded: '文件大小超过限制',
      selectMagFiles: '请选择要上传的 MAG 文件',
      magUploadSuccess: 'MAG 上传成功，共 {count} 个文件',
      magUploadFailed: 'MAG 上传失败，请重试',
      taskCreated: '抗性基因分析任务已创建',
      createTaskFailed: '创建任务失败',
      fileDeleted: '文件已删除',
      unsupportedFormat: '不支持的文件格式: {name}'
    },
    confirmAnalyze: '文件上传成功，是否立即开始抗性基因分析？',
    confirmAnalyzeTitle: '提示',
    analyzeNow: '立即分析',
    analyzeLater: '稍后分析',
    confirmAnalyzeFile: '确定要对该文件进行抗性基因分析吗？',
    confirmAnalyzeFileTitle: '确认',
    startAnalyze: '开始分析',
    confirmDelete: '确定要删除该文件吗？删除后无法恢复。',
    confirmDeleteTitle: '警告',
    magTaskCreated: 'MAG 分析任务已创建，是否前往历史记录查看进度？',
    magTaskCreatedTitle: '任务已创建',
    viewProgress: '查看进度',
    stayHere: '留在此页'
  },
  // 登录页面
  login: {
    title: '抗性基因识别系统',
    subtitle: 'ARG Identification System',
    tabs: {
      login: '登录',
      register: '注册'
    },
    email: '邮箱',
    emailPlaceholder: '请输入邮箱',
    password: '密码',
    passwordPlaceholder: '请输入密码',
    code: '验证码',
    codePlaceholder: '请输入验证码',
    getCode: '获取验证码',
    retryAfter: '{seconds}秒后重试',
    loginButton: '登录',
    username: '用户名',
    usernamePlaceholder: '请输入用户名',
    confirmPassword: '确认密码',
    confirmPasswordPlaceholder: '请确认密码',
    registerButton: '注册',
    messages: {
      enterEmail: '请先输入邮箱',
      invalidEmail: '请输入正确的邮箱格式',
      codeSent: '验证码已发送到您的邮箱',
      enterPassword: '请输入密码',
      enterCode: '请输入验证码',
      loginSuccess: '登录成功',
      enterUsername: '请输入用户名',
      usernameLength: '用户名长度必须在3-20个字符之间',
      usernameFormat: '用户名只能包含字母、数字、下划线和连字符',
      passwordLength: '密码长度必须在6-20个字符之间',
      confirmPasswordRequired: '请确认密码',
      passwordMismatch: '两次输入的密码不一致',
      codeFormat: '验证码必须是6位数字',
      registerSuccess: '注册成功，请登录'
    }
  },
  // 历史记录页面
  history: {
    title: '历史记录',
    search: '搜索任务...',
    searchPlaceholder: '输入任务名称或描述进行搜索',
    noTasks: '暂无任务',
    table: {
      taskId: '任务ID',
      taskName: '任务名称',
      inputType: '输入类型',
      status: '状态',
      progress: '进度',
      createdAt: '创建时间',
      completedAt: '完成时间',
      actions: '操作'
    },
    taskTypes: {
      sequence: '序列',
      file: '文件',
      mag: 'MAG'
    },
    status: {
      pending: '等待中',
      running: '运行中',
      completed: '已完成',
      failed: '失败',
      cancelled: '已取消'
    },
    actions: {
      view: '查看结果',
      cancel: '取消',
      delete: '删除',
      download: '下载'
    },
    confirmCancel: '确定要取消该任务吗？',
    confirmCancelTitle: '确认取消',
    confirmDelete: '确定要删除该任务吗？删除后无法恢复。',
    confirmDeleteTitle: '确认删除',
    messages: {
      cancelSuccess: '任务已取消',
      cancelFailed: '取消任务失败',
      deleteSuccess: '任务已删除',
      deleteFailed: '删除任务失败',
      noResult: '该任务没有分析结果'
    }
  },
  // 可视化页面
  visualization: {
    title: '结果可视化',
    selectTask: '选择任务',
    selectTaskPlaceholder: '请选择一个任务查看结果',
    emptyDescription: '请先选择一个已完成的 ARG 分析任务',
    noTasks: '暂无已完成的任务',
    info: {
      taskId: '任务ID',
      taskName: '任务名称',
      analysisType: '分析类型',
      argDetection: '抗性基因检测 (ARG)',
      resultSummary: '识别结果',
      resultCount: '共 {total} 条序列，其中 {arg} 条预测为抗性基因'
    },
    tab: {
      detail: '预测结果详情',
      charts: '可视化图表'
    },
    detail: {
      title: '抗性基因预测结果',
      summaryDesc: '共分析 {total} 条序列，其中 {arg} 条预测为抗性基因，{nonArg} 条预测为非抗性基因',
      downloadArg: '下载 ARG 预测结果',
      legendTitle: '表格颜色说明：',
      legendArg: '绿色 = 预测为抗性基因',
      legendNonArg: '灰色 = 预测为非抗性基因',
      noResults: '没有 ARG 预测结果',
      searchPlaceholder: '搜索序列 ID...',
      filterType: '筛选类型',
      showCount: '显示 {filtered} / {total} 条'
    },
    filterAll: '全部',
    filterArgOnly: '仅 ARG',
    filterNonArgOnly: '仅非 ARG',
    chartsPage: {
      title: 'ARG 预测结果可视化',
      downloadCharts: '下载图表图片',
      noData: '没有 ARG 预测结果可供可视化',
      pieTitle: 'ARG 与非 ARG 序列分布',
      pieDesc: '抗性基因与非抗性基因的数量占比',
      barTitle: 'ARG 分类统计',
      barDesc: '抗性基因的序列中，各个 ARG 类别的数量分布',
      noCategory: '暂无分类信息'
    },
    downloadCard: {
      title: '下载分析结果',
      magTip: '这是一个 MAG 分析任务，可下载 Prodigal 预处理结果',
      argFileName: 'ARG 预测结果',
      argFileDesc: '包含所有序列的抗性基因预测结果 (TSV 格式)',
      downloadBtn: '下载',
      mergedFileName: '合并后的蛋白质序列',
      mergedFileDesc: 'Prodigal 预测后合并的所有蛋白质序列 (FAA 格式)',
      prodigalFileName: 'Prodigal 预测结果',
      prodigalFileDescSuffix: '(ZIP 打包)',
      prodigalFileDescEn: 'Prodigal 预测结果打包 (ZIP 打包)',
      allFileName: '下载全部结果',
      allFileDesc: '打包下载所有分析结果文件 (ZIP 格式)',
      downloadAllBtn: '一键下载全部'
    },
    chartPie: {
      tooltipCount: '数量',
      tooltipPercent: '占比',
      seriesName: 'ARG 分布',
      argName: '抗性基因 (ARG)',
      nonArgName: '非抗性基因',
      labelUnit: '条'
    },
    chartBar: {
      tooltipCount: '数量',
      yAxisName: '数量',
      seriesName: 'ARG 类别数量'
    },
    summary: {
      title: '分析摘要',
      totalSequences: '总序列数',
      argSequences: 'ARG 序列数',
      nonArgSequences: '非 ARG 序列数',
      argRatio: 'ARG 占比',
      categories: 'ARG 类别数'
    },
    charts: {
      distribution: 'ARG 类型分布',
      confidence: '预测置信度分布',
      sequenceLength: '序列长度分布'
    },
    table: {
      title: '序列详情',
      sequenceId: '序列ID',
      isArg: 'ARG',
      argClass: 'ARG 类型',
      binaryProb: '识别置信度',
      classProb: '分类置信度',
      actions: '操作'
    },
    topClasses: '分类置信度分布 (Top 5)',
    yes: '是',
    no: '否',
    blast: 'BLAST',
    blastDrawer: {
      title: 'BLAST 比对结果',
      loadingText: '正在进行 BLAST 比对...',
      querySection: '查询序列',
      queryId: '序列 ID',
      queryLength: '序列长度',
      predClass: '预测分类',
      hitsSection: '比对结果',
      hitsCount: '找到 {count} 个匹配',
      noSignificantMatch: '未找到显著匹配',
      subjectId: '匹配序列',
      identity: '一致性',
      alignLength: '比对长度',
      evalue: 'E 值',
      bitScore: '比分',
      hitDetail: '匹配详情',
      hitIdLabel: '匹配序列 ID',
      description: '描述',
      identityLabel: '序列一致性',
      bitScoreLabel: '比分 (Bit Score)',
      alignLengthLabel: '比对长度',
      queryRange: '查询序列区域',
      subjectRange: '目标序列区域',
      queryLengthLabel: '查询序列长度',
      subjectLengthLabel: '目标序列长度',
      close: '关闭',
      reRunBlast: '重新比对',
      queryInfo: '查询序列信息',
      noHits: '未找到匹配序列',
      hitId: '匹配ID'
    },
    download: {
      all: '下载全部结果',
      argOnly: '仅下载 ARG',
      csv: '导出 CSV'
    },
    loading: '加载中...',
    messages: {
      loadFailed: '加载任务结果失败',
      loadSuccess: '数据加载成功',
      blastFailed: 'BLAST 比对失败',
      downloadFailed: '下载失败',
      downloadSuccess: '下载成功',
      preparingDownload: '正在准备下载...',
      chartsGenerating: '正在生成图片...',
      chartsDownloadSuccess: '图表图片下载成功',
      pieDownloadSuccess: '饼图下载成功',
      noChartsToDownload: '没有可下载的图表'
    }
  },
  // 平台介绍页面
  introduction: {
    title: '平台介绍',
    subtitle: 'ARG Analysis Platform',
    hero: {
      title: 'ARG 抗性基因智能分析平台',
      subtitle: '基于深度学习的抗性基因识别与分类系统',
      description: '利用 BiLSTM 深度学习模型，快速准确地识别基因组序列中的抗性基因，并进行分类预测，为微生物耐药性研究提供强有力的分析工具。'
    },
    background: {
      title: '抗性基因背景知识',
      subtitle: 'Antibiotic Resistance Genes Background',
      definition: {
        title: '什么是抗性基因？',
        content: '抗性基因（Antibiotic Resistance Genes, ARGs）是指存在于细菌基因组中，能够使细菌对抗生素产生耐药性的特定基因。这些基因通过编码特定的蛋白质，产生多种耐药机制，严重威胁人类公共健康。'
      },
      mechanisms: {
        title: '耐药机制',
        enzyme: {
          title: '酶解作用',
          desc: '产生 β-内酰胺酶等降解抗生素分子，使其失去活性'
        },
        pump: {
          title: '外排泵',
          desc: '将抗生素主动从细胞内排出，降低细胞内药物浓度'
        },
        target: {
          title: '靶点修饰',
          desc: '改变抗生素作用的靶点结构，降低药物结合能力'
        },
        wall: {
          title: '细胞壁改变',
          desc: '改变细胞壁或膜结构，阻止抗生素进入细胞'
        }
      },
      importance: {
        title: '抗性基因研究的重要性',
        clinical: {
          title: '临床诊断',
          desc: '帮助医生了解病原菌耐药性，选择有效治疗方案'
        },
        epidemiology: {
          title: '流行病学监测',
          desc: '追踪抗性基因传播路径，制定精准防控策略'
        },
        drug: {
          title: '新药研发',
          desc: '揭示耐药机制，助力开发新型抗生素药物'
        },
        environment: {
          title: '环境监测',
          desc: '评估环境抗性基因分布，防范潜在生态风险'
        }
      },
      categories: {
        title: '抗性基因分类体系'
      }
    },
    workflow: {
      title: '平台使用流程',
      subtitle: 'Analysis Workflow',
      steps: {
        upload: {
          title: '数据上传',
          desc: '用户选择输入方式上传序列数据'
        },
        process: {
          title: '序列处理',
          desc: '对 MAG 文件进行基因预测和序列预处理'
        },
        analyze: {
          title: '模型分析',
          desc: '调用 BiLSTM 模型进行抗性基因识别与分类'
        },
        output: {
          title: '结果输出',
          desc: '生成分析报告和可视化图表'
        }
      },
      inputs: {
        title: '三种输入方式'
      }
    },
    model: {
      title: '深度学习模型介绍',
      subtitle: 'BiLSTM Deep Learning Model',
      overview: {
        title: 'BiLSTM 双模型架构',
        content: '本平台采用基于双向长短期记忆网络 (BiLSTM) 的深度学习模型，包含两个级联模型：二分类模型用于识别抗性基因，多分类模型用于分类抗性基因类型。'
      },
      binary: {
        title: '二分类模型',
        desc: '判断输入序列是否为抗性基因',
        specs: {
          task: '任务类型',
          output: '输出',
          structure: '网络结构',
          accuracy: '准确率'
        }
      },
      multi: {
        title: '多分类模型',
        desc: '对识别出的 ARG 进行 14 分类',
        specs: {
          task: '任务类型',
          output: '输出',
          structure: '网络结构',
          categories: '分类数量'
        }
      },
      encoding: {
        title: '序列特征编码方式'
      }
    },
    tech: {
      title: '技术栈架构',
      subtitle: 'Technology Stack',
      frontend: '前端',
      backend: '后端',
      ml: '深度学习',
      infra: '基础设施'
    }
  },
  // 管理页面
  admin: {
    title: '管理功能',
    systemStats: '系统统计',
    tabs: {
      users: '用户管理',
      files: '文件管理',
      logs: '登录日志',
      system: '系统信息'
    },
    stats: {
      totalUsers: '总用户数',
      totalFiles: '总文件数',
      totalTasks: '总任务数',
      totalLogins: '总登录次数'
    },
    users: {
      title: '用户列表',
      search: '搜索用户...',
      searchPlaceholder: '搜索用户名或用户ID',
      table: {
        userId: '用户ID',
        username: '用户名',
        email: '邮箱',
        role: '角色',
        status: '状态',
        fileCount: '文件数',
        taskCount: '任务数',
        createdAt: '创建时间',
        lastLogin: '最后登录',
        actions: '操作'
      },
      roles: {
        admin: '管理员',
        user: '普通用户'
      },
      status: {
        active: '正常',
        banned: '已封禁',
        disabled: '禁用'
      },
      notLoggedIn: '未登录',
      actions: {
        setAdmin: '设为管理员',
        removeAdmin: '移除管理员',
        disable: '禁用',
        enable: '启用',
        delete: '删除'
      },
      confirmDelete: '确定要删除用户 "{name}" 吗？此操作将删除该用户的所有文件、任务和相关数据，且无法恢复！',
      deleteSuccess: '用户删除成功',
      deleting: '正在删除…',
      noMatch: '未找到匹配的用户'
    },
    files: {
      title: '文件管理',
      searchUser: '用户ID或用户名',
      searchFile: '文件ID或文件名',
      table: {
        fileId: '文件ID',
        userId: '用户ID',
        username: '用户名',
        filename: '文件名',
        fileSize: '文件大小',
        fileType: '文件类型',
        status: '状态',
        uploadTime: '上传时间',
        actions: '操作'
      },
      status: {
        uploaded: '已上传',
        analyzing: '分析中',
        completed: '已完成',
        deleted: '已删除',
        failed: '失败'
      },
      confirmDelete: '确定要删除文件 "{name}" 吗？此操作将删除该文件及其所有相关任务和分析结果，且无法恢复！',
      deleteSuccess: '文件删除成功',
      deleting: '正在删除…',
      noMatch: '未找到匹配的文件'
    },
    logs: {
      title: '登录日志',
      table: {
        userId: '用户ID',
        username: '用户名',
        ip: 'IP地址',
        location: '位置',
        loginTime: '登录时间',
        status: '状态'
      },
      status: {
        success: '成功',
        failed: '失败'
      }
    },
    system: {
      title: '系统信息',
      version: '系统版本',
      uptime: '运行时间',
      memory: '内存使用',
      disk: '磁盘使用'
    },
    dangerAction: '危险操作',
    confirmDelete: '确定删除',
    messages: {
      getUsersFailed: '获取用户列表失败',
      getFilesFailed: '获取文件列表失败',
      searchUsersFailed: '搜索用户失败',
      searchFilesFailed: '搜索文件失败',
      deleteUserFailed: '删除用户失败',
      deleteFileFailed: '删除文件失败'
    }
  },
  // 通用
  common: {
    index: '索引',
    confirm: '确定',
    cancel: '取消',
    delete: '删除',
    save: '保存',
    edit: '编辑',
    add: '添加',
    search: '搜索',
    reset: '重置',
    loading: '加载中...',
    noData: '暂无数据',
    success: '操作成功',
    error: '操作失败',
    warning: '警告',
    tip: '提示',
    yes: '是',
    no: '否',
    all: '全部',
    total: '共 {count} 条',
    aa: 'aa'
  },
  // 确认对话框
  confirmDialog: {
    logout: '确定要退出登录吗？'
  }
}
