export default {
  app: {
    title: 'ARG Identification System',
    name: 'ARG Identification'
  },
  nav: {
    home: 'Home',
    upload: 'Upload',
    visualization: 'Visualization',
    history: 'History',
    introduction: 'About',
    admin: 'Admin',
    login: 'Login',
    logout: 'Logout'
  },
  // Sidebar
  sidebar: {
    title: 'ARG Identification',
    subtitle: 'ARG Identification'
  },
  // Home page
  home: {
    welcome: 'Welcome to ARG Detection System',
    subtitle: 'Deep Learning-based ARG Recognition and Visualization Platform',
    startUpload: 'Start Upload',
    viewHistory: 'View History',
    stats: {
      totalFiles: 'Uploaded Files',
      completedTasks: 'Completed Tasks',
      runningTasks: 'Running Tasks'
    }
  },
  // Upload page
  upload: {
    title: 'Sequence Upload',
    inputType: 'Input Type',
    inputTypes: {
      sequence: 'Protein Sequence',
      file: 'FASTA File',
      mag: 'MAG Folder'
    },
    magName: 'MAG Name',
    magNamePlaceholder: 'Optional, auto-generated if empty',
    fileType: 'File Type',
    fileTypePlaceholder: 'Select file type',
    fileTypes: {
      autoDetect: 'Auto Detect',
      fasta: 'FASTA',
      faa: 'FAA (Protein Sequence)'
    },
    description: 'Description',
    descriptionPlaceholder: 'Enter file description (optional)',
    uploadContent: 'Upload Content',
    sequencePlaceholder: 'Paste or enter protein sequence here (FASTA format)',
    sequenceTip: 'Text content will be uploaded directly for analysis',
    dragText: 'Drop file here, or',
    clickUpload: 'click to upload',
    fileTip: 'Supports .fasta / .fa / .faa format, max 500MB',
    magUploadMode: {
      folder: 'Select Folder',
      files: 'Select Files'
    },
    magFolderText: 'Click to select MAG folder, or drop folder here',
    magFilesText: 'Click to select multiple files, or drop files here',
    magFileTip: 'Supports .fa / .fasta / .fna format',
    selected: 'Selected',
    filesCount: '{count} files',
    clear: 'Clear',
    moreFiles: '{count} more files...',
    magInfo: {
      title: 'MAG Analysis Info:',
      step1: '1. Upload folder containing nucleotide sequence files (.fa/.fasta/.fna)',
      step2: '2. System will use Prodigal for gene prediction and convert to protein sequences',
      step3: '3. Then perform ARG recognition and classification'
    },
    uploading: 'Uploading...',
    uploadAndAnalyzeMag: 'Upload & Analyze MAG',
    startUpload: 'Start Upload',
    reset: 'Reset',
    progress: {
      title: 'Upload Progress',
      magTitle: 'MAG Analysis Progress',
      uploading: 'Uploading file...',
      magUploading: 'Uploading MAG files...',
      processing: 'Processing...',
      magProcessing: 'MAG analysis task created, processing...',
      success: 'Upload successful!',
      taskCreated: 'Analysis task created (ID: {id})'
    },
    steps: {
      prodigal: 'Prodigal Preprocessing',
      arg: 'ARG Analysis'
    },
    myFiles: 'My Files',
    refresh: 'Refresh',
    table: {
      filename: 'Filename',
      fileType: 'File Type',
      fileSize: 'File Size',
      status: 'Status',
      uploadTime: 'Upload Time',
      actions: 'Actions'
    },
    status: {
      uploaded: 'Uploaded',
      processing: 'Processing',
      failed: 'Failed',
      deleted: 'Deleted'
    },
    actions: {
      analyze: 'Analyze',
      delete: 'Delete'
    },
    messages: {
      onlyOneFile: 'Only one file allowed at a time',
      noValidFiles: 'No valid FASTA files found (.fa/.fasta/.fna)',
      selectedFiles: 'Selected {count} valid files',
      enterText: 'Please enter text content to upload',
      selectFile: 'Please select a file to upload',
      fileTooLarge: 'File size {size} MB exceeds limit (max 500 MB)',
      uploadSuccess: 'File uploaded successfully',
      uploadFailed: 'Upload failed, please retry',
      serverError: 'Server error, please try again later',
      fileSizeExceeded: 'File size exceeds limit',
      selectMagFiles: 'Please select MAG files to upload',
      magUploadSuccess: 'MAG uploaded successfully, {count} files total',
      magUploadFailed: 'MAG upload failed, please retry',
      taskCreated: 'ARG analysis task created',
      createTaskFailed: 'Failed to create task',
      fileDeleted: 'File deleted',
      unsupportedFormat: 'Unsupported format: {name}'
    },
    confirmAnalyze: 'File uploaded successfully. Start ARG analysis now?',
    confirmAnalyzeTitle: 'Confirm',
    analyzeNow: 'Analyze Now',
    analyzeLater: 'Later',
    confirmAnalyzeFile: 'Are you sure to analyze this file for ARG?',
    confirmAnalyzeFileTitle: 'Confirm',
    startAnalyze: 'Start Analysis',
    confirmDelete: 'Are you sure to delete this file? This cannot be undone.',
    confirmDeleteTitle: 'Warning',
    magTaskCreated: 'MAG analysis task created. Go to history to view progress?',
    magTaskCreatedTitle: 'Task Created',
    viewProgress: 'View Progress',
    stayHere: 'Stay Here'
  },
  // Login page
  login: {
    title: 'ARG Identification System',
    subtitle: 'ARG Identification System',
    tabs: {
      login: 'Login',
      register: 'Register'
    },
    email: 'Email',
    emailPlaceholder: 'Enter your email',
    password: 'Password',
    passwordPlaceholder: 'Enter your password',
    code: 'Verification Code',
    codePlaceholder: 'Enter verification code',
    getCode: 'Get Code',
    retryAfter: 'Retry in {seconds}s',
    loginButton: 'Login',
    username: 'Username',
    usernamePlaceholder: 'Enter your username',
    confirmPassword: 'Confirm Password',
    confirmPasswordPlaceholder: 'Confirm your password',
    registerButton: 'Register',
    messages: {
      enterEmail: 'Please enter your email first',
      invalidEmail: 'Please enter a valid email address',
      codeSent: 'Verification code sent to your email',
      enterPassword: 'Please enter your password',
      enterCode: 'Please enter verification code',
      loginSuccess: 'Login successful',
      enterUsername: 'Please enter your username',
      usernameLength: 'Username must be 3-20 characters',
      usernameFormat: 'Username can only contain letters, numbers, underscore and hyphen',
      passwordLength: 'Password must be 6-20 characters',
      confirmPasswordRequired: 'Please confirm your password',
      passwordMismatch: 'Passwords do not match',
      codeFormat: 'Verification code must be 6 digits',
      registerSuccess: 'Registration successful, please login'
    }
  },
  // History page
  history: {
    title: 'History',
    search: 'Search tasks...',
    searchPlaceholder: 'Enter task name or description to search',
    noTasks: 'No tasks',
    table: {
      taskId: 'Task ID',
      taskName: 'Task Name',
      inputType: 'Input Type',
      status: 'Status',
      progress: 'Progress',
      createdAt: 'Created At',
      completedAt: 'Completed At',
      actions: 'Actions'
    },
    taskTypes: {
      sequence: 'Sequence',
      file: 'File',
      mag: 'MAG'
    },
    status: {
      pending: 'Pending',
      running: 'Running',
      completed: 'Completed',
      failed: 'Failed',
      cancelled: 'Cancelled'
    },
    actions: {
      view: 'View Result',
      cancel: 'Cancel',
      delete: 'Delete',
      download: 'Download'
    },
    confirmCancel: 'Are you sure to cancel this task?',
    confirmCancelTitle: 'Confirm Cancel',
    confirmDelete: 'Are you sure to delete this task? This cannot be undone.',
    confirmDeleteTitle: 'Confirm Delete',
    messages: {
      cancelSuccess: 'Task cancelled',
      cancelFailed: 'Failed to cancel task',
      deleteSuccess: 'Task deleted',
      deleteFailed: 'Failed to delete task',
      noResult: 'No analysis result for this task'
    }
  },
  // Visualization page
  visualization: {
    title: 'Result Visualization',
    selectTask: 'Select Task',
    selectTaskPlaceholder: 'Select a task to view results',
    emptyDescription: 'Please select a completed ARG analysis task first',
    noTasks: 'No completed tasks',
    info: {
      taskId: 'Task ID',
      taskName: 'Task Name',
      analysisType: 'Analysis Type',
      argDetection: 'ARG Detection',
      resultSummary: 'Result Summary',
      resultCount: '{total} sequences, {arg} predicted as ARG'
    },
    tab: {
      detail: 'Result Details',
      charts: 'Charts'
    },
    detail: {
      title: 'ARG Prediction Results',
      summaryDesc: '{total} sequences analyzed, {arg} predicted as ARG, {nonArg} as non-ARG',
      downloadArg: 'Download ARG Results',
      legendTitle: 'Table legend:',
      legendArg: 'Green = ARG',
      legendNonArg: 'Gray = Non-ARG',
      noResults: 'No ARG prediction results',
      searchPlaceholder: 'Search sequence ID...',
      filterType: 'Filter',
      showCount: '{filtered} / {total}'
    },
    filterAll: 'All',
    filterArgOnly: 'ARG only',
    filterNonArgOnly: 'Non-ARG only',
    chartsPage: {
      title: 'ARG Result Visualization',
      downloadCharts: 'Download Charts',
      noData: 'No ARG results to visualize',
      pieTitle: 'ARG vs Non-ARG Distribution',
      pieDesc: 'Proportion of ARG vs non-ARG sequences',
      barTitle: 'ARG Category Statistics',
      barDesc: 'Count per ARG category',
      noCategory: 'No category data'
    },
    downloadCard: {
      title: 'Download Results',
      magTip: 'This is a MAG task; you can download Prodigal preprocessing results',
      argFileName: 'ARG Prediction Results',
      argFileDesc: 'All sequences with ARG prediction (TSV)',
      downloadBtn: 'Download',
      mergedFileName: 'Merged Protein Sequences',
      mergedFileDesc: 'All Prodigal-predicted protein sequences (FAA)',
      prodigalFileName: 'Prodigal Results',
      prodigalFileDescSuffix: '(ZIP)',
      prodigalFileDescEn: 'Prodigal prediction results package (ZIP)',
      allFileName: 'Download All',
      allFileDesc: 'All result files (ZIP)',
      downloadAllBtn: 'Download All'
    },
    chartPie: {
      tooltipCount: 'Count',
      tooltipPercent: 'Ratio',
      seriesName: 'ARG Distribution',
      argName: 'ARG',
      nonArgName: 'Non-ARG',
      labelUnit: ''
    },
    chartBar: {
      tooltipCount: 'Count',
      yAxisName: 'Count',
      seriesName: 'ARG Categories'
    },
    summary: {
      title: 'Analysis Summary',
      totalSequences: 'Total Sequences',
      argSequences: 'ARG Sequences',
      nonArgSequences: 'Non-ARG Sequences',
      argRatio: 'ARG Ratio',
      categories: 'ARG Categories'
    },
    charts: {
      distribution: 'ARG Type Distribution',
      confidence: 'Prediction Confidence Distribution',
      sequenceLength: 'Sequence Length Distribution'
    },
    table: {
      title: 'Sequence Details',
      sequenceId: 'Sequence ID',
      isArg: 'ARG',
      argClass: 'ARG Type',
      binaryProb: 'Detection Confidence',
      classProb: 'Classification Confidence',
      actions: 'Actions'
    },
    topClasses: 'Classification Confidence (Top 5)',
    yes: 'Yes',
    no: 'No',
    blast: 'BLAST',
    blastDrawer: {
      title: 'BLAST Results',
      loadingText: 'Running BLAST...',
      querySection: 'Query Sequence',
      queryId: 'Sequence ID',
      queryLength: 'Sequence Length',
      predClass: 'Predicted Class',
      hitsSection: 'Hits',
      hitsCount: '{count} hit(s) found',
      noSignificantMatch: 'No significant matches',
      subjectId: 'Subject ID',
      identity: 'Identity',
      alignLength: 'Align Length',
      evalue: 'E-value',
      bitScore: 'Bit Score',
      hitDetail: 'Hit Detail',
      hitIdLabel: 'Subject ID',
      description: 'Description',
      identityLabel: 'Identity',
      bitScoreLabel: 'Bit Score',
      alignLengthLabel: 'Alignment Length',
      queryRange: 'Query Range',
      subjectRange: 'Subject Range',
      queryLengthLabel: 'Query Length',
      subjectLengthLabel: 'Subject Length',
      close: 'Close',
      reRunBlast: 'Re-run BLAST',
      queryInfo: 'Query Sequence Info',
      noHits: 'No matching sequences found',
      hitId: 'Hit ID'
    },
    download: {
      all: 'Download All Results',
      argOnly: 'Download ARG Only',
      csv: 'Export CSV'
    },
    loading: 'Loading...',
    messages: {
      loadFailed: 'Failed to load task results',
      loadSuccess: 'Data loaded successfully',
      blastFailed: 'BLAST alignment failed',
      downloadFailed: 'Download failed',
      downloadSuccess: 'Download successful',
      preparingDownload: 'Preparing download...',
      chartsGenerating: 'Generating images...',
      chartsDownloadSuccess: 'Charts downloaded',
      pieDownloadSuccess: 'Pie chart downloaded',
      noChartsToDownload: 'No charts to download'
    }
  },
  // Introduction page
  introduction: {
    title: 'About Platform',
    subtitle: 'ARG Analysis Platform',
    hero: {
      title: 'ARG Intelligent Analysis Platform',
      subtitle: 'Deep Learning-based ARG Recognition and Classification System',
      description: 'Using BiLSTM deep learning models to quickly and accurately identify antibiotic resistance genes in genomic sequences and classify them, providing a powerful analytical tool for microbial resistance research.'
    },
    background: {
      title: 'ARG Background Knowledge',
      subtitle: 'Antibiotic Resistance Genes Background',
      definition: {
        title: 'What are ARGs?',
        content: 'Antibiotic Resistance Genes (ARGs) are specific genes found in bacterial genomes that enable bacteria to develop resistance to antibiotics. These genes encode proteins that confer resistance through various mechanisms, posing a serious threat to public health.'
      },
      mechanisms: {
        title: 'Resistance Mechanisms',
        enzyme: {
          title: 'Enzymatic Inactivation',
          desc: 'Produces enzymes like beta-lactamases that degrade antibiotic molecules'
        },
        pump: {
          title: 'Efflux Pumps',
          desc: 'Actively pump antibiotics out of the cell, reducing intracellular drug concentration'
        },
        target: {
          title: 'Target Modification',
          desc: 'Alter the structure of antibiotic targets, reducing drug binding efficiency'
        },
        wall: {
          title: 'Cell Wall Alteration',
          desc: 'Modify cell wall or membrane structure to prevent antibiotic entry'
        }
      },
      importance: {
        title: 'Importance of ARG Research',
        clinical: {
          title: 'Clinical Diagnosis',
          desc: 'Help doctors understand pathogen resistance and select effective treatments'
        },
        epidemiology: {
          title: 'Epidemiological Surveillance',
          desc: 'Track ARG transmission paths and develop precise prevention strategies'
        },
        drug: {
          title: 'Drug Development',
          desc: 'Reveal resistance mechanisms to help develop new antibiotics'
        },
        environment: {
          title: 'Environmental Monitoring',
          desc: 'Assess ARG distribution in environment and prevent ecological risks'
        }
      },
      categories: {
        title: 'ARG Classification System'
      }
    },
    workflow: {
      title: 'Platform Workflow',
      subtitle: 'Analysis Workflow',
      steps: {
        upload: {
          title: 'Data Upload',
          desc: 'Users upload sequence data via selected input method'
        },
        process: {
          title: 'Sequence Processing',
          desc: 'Perform gene prediction and sequence preprocessing for MAG files'
        },
        analyze: {
          title: 'Model Analysis',
          desc: 'Call BiLSTM model for ARG recognition and classification'
        },
        output: {
          title: 'Result Output',
          desc: 'Generate analysis reports and visualization charts'
        }
      },
      inputs: {
        title: 'Three Input Methods'
      }
    },
    model: {
      title: 'Deep Learning Model',
      subtitle: 'BiLSTM Deep Learning Model',
      overview: {
        title: 'BiLSTM Dual-Model Architecture',
        content: 'This platform employs deep learning models based on Bidirectional Long Short-Term Memory networks (BiLSTM), consisting of two cascaded models: a binary classifier for ARG recognition and a multi-class classifier for ARG type classification.'
      },
      binary: {
        title: 'Binary Classification Model',
        desc: 'Determine if input sequence is an ARG',
        specs: {
          task: 'Task Type',
          output: 'Output',
          structure: 'Network Structure',
          accuracy: 'Accuracy'
        }
      },
      multi: {
        title: 'Multi-class Classification Model',
        desc: 'Classify identified ARGs into 14 categories',
        specs: {
          task: 'Task Type',
          output: 'Output',
          structure: 'Network Structure',
          categories: 'Number of Classes'
        }
      },
      encoding: {
        title: 'Sequence Feature Encoding'
      }
    },
    tech: {
      title: 'Technology Stack',
      subtitle: 'Technology Stack',
      frontend: 'Frontend',
      backend: 'Backend',
      ml: 'Deep Learning',
      infra: 'Infrastructure'
    }
  },
  // Admin page
  admin: {
    title: 'Admin Panel',
    systemStats: 'System Statistics',
    tabs: {
      users: 'User Management',
      files: 'File Management',
      logs: 'Login Logs',
      system: 'System Info'
    },
    stats: {
      totalUsers: 'Total Users',
      totalFiles: 'Total Files',
      totalTasks: 'Total Tasks',
      totalLogins: 'Total Logins'
    },
    users: {
      title: 'User List',
      search: 'Search users...',
      searchPlaceholder: 'Search username or user ID',
      table: {
        userId: 'User ID',
        username: 'Username',
        email: 'Email',
        role: 'Role',
        status: 'Status',
        fileCount: 'Files',
        taskCount: 'Tasks',
        createdAt: 'Created At',
        lastLogin: 'Last Login',
        actions: 'Actions'
      },
      roles: {
        admin: 'Admin',
        user: 'User'
      },
      status: {
        active: 'Active',
        banned: 'Banned',
        disabled: 'Disabled'
      },
      notLoggedIn: 'Never',
      actions: {
        setAdmin: 'Set as Admin',
        removeAdmin: 'Remove Admin',
        disable: 'Disable',
        enable: 'Enable',
        delete: 'Delete'
      },
      confirmDelete: 'Are you sure to delete user "{name}"? This will delete all files, tasks and data of this user. This cannot be undone!',
      deleteSuccess: 'User deleted successfully',
      deleting: 'Deleting…',
      noMatch: 'No matching users found'
    },
    files: {
      title: 'File Management',
      searchUser: 'User ID or username',
      searchFile: 'File ID or filename',
      table: {
        fileId: 'File ID',
        userId: 'User ID',
        username: 'Username',
        filename: 'Filename',
        fileSize: 'File Size',
        fileType: 'File Type',
        status: 'Status',
        uploadTime: 'Upload Time',
        actions: 'Actions'
      },
      status: {
        uploaded: 'Uploaded',
        analyzing: 'Analyzing',
        completed: 'Completed',
        deleted: 'Deleted',
        failed: 'Failed'
      },
      confirmDelete: 'Are you sure to delete file "{name}"? This will delete all related tasks and analysis results. This cannot be undone!',
      deleteSuccess: 'File deleted successfully',
      deleting: 'Deleting…',
      noMatch: 'No matching files found'
    },
    logs: {
      title: 'Login Logs',
      table: {
        userId: 'User ID',
        username: 'Username',
        ip: 'IP Address',
        location: 'Location',
        loginTime: 'Login Time',
        status: 'Status'
      },
      status: {
        success: 'Success',
        failed: 'Failed'
      }
    },
    system: {
      title: 'System Information',
      version: 'System Version',
      uptime: 'Uptime',
      memory: 'Memory Usage',
      disk: 'Disk Usage'
    },
    dangerAction: 'Dangerous Action',
    confirmDelete: 'Confirm Delete',
    messages: {
      getUsersFailed: 'Failed to get user list',
      getFilesFailed: 'Failed to get file list',
      searchUsersFailed: 'Failed to search users',
      searchFilesFailed: 'Failed to search files',
      deleteUserFailed: 'Failed to delete user',
      deleteFileFailed: 'Failed to delete file'
    }
  },
  // Common
  common: {
    index: 'Index',
    confirm: 'Confirm',
    cancel: 'Cancel',
    delete: 'Delete',
    save: 'Save',
    edit: 'Edit',
    add: 'Add',
    search: 'Search',
    reset: 'Reset',
    loading: 'Loading...',
    noData: 'No data',
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    tip: 'Tip',
    yes: 'Yes',
    no: 'No',
    all: 'All',
    total: 'Total {count} items',
    aa: 'aa'
  },
  // Confirm dialogs
  confirmDialog: {
    logout: 'Are you sure you want to logout?'
  }
}
