# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an **Antibiotic Resistance Gene (ARG) identification system** using BiLSTM deep learning models. It provides:
- Binary classification (ARG vs non-ARG)
- Multi-class classification (14 ARG types: MLS, aminoglycoside, beta-lactam, tetracycline, etc.)
- Web visualization interface (Vue 3 frontend + Spring Boot backend)
- BLAST sequence similarity search
- Batch processing for MAG (Metagenome-Assembled Genome) analysis

## Tech Stack

- **Backend**: Spring Boot 3.2.3 (Java 17), MySQL, Redis, Spring Security + JWT
- **Frontend**: Vue 3 (Composition API), Vite 6, Element Plus, Pinia, ECharts
- **ML**: PyTorch BiLSTM model (Dockerized)
- **Tools**: Prodigal (gene prediction), NCBI BLAST

## Build Commands

### Backend
```bash
cd arg_visualization_web/backend
mvn clean package -DskipTests
java -jar target/web-0.0.1-SNAPSHOT.jar
```

### Frontend
```bash
cd arg_visualization_web/frontend
npm run dev          # Development server
npm run build        # Production build
```

### Docker Images
```bash
# ARG-BiLSTM (CPU)
cd docker_image_v1.0 && docker build -t arg-bilstm:latest .

# GPU version
docker build -f Dockerfile.gpu -t arg-bilstm:gpu .

# Prodigal
cd docker_image_prodigal && docker build -t prodigal:latest .
```

### Workflow Script
```bash
cd workflow && pip install -r requirements.txt
python run_arg_analysis.py --input_dir /path/to/mags --output results.tsv
```

## Architecture

### Data Flow
```
User Upload → Backend Storage → Docker Container → Results → Visualization
```

### API Structure
- `/api/auth` - Authentication (login, register, JWT tokens)
- `/api/analysis` - Task management
- `/api/file` - File operations
- `/api/mag` - MAG analysis
- `/api/visualization` - Visualization data
- `/api/blast` - BLAST search

### Key Files
| Path | Purpose |
|------|---------|
| `application.yml` | Backend config (DB, Redis, Docker, file paths) |
| `messages_*.properties` | i18n (Chinese/English) |
| `router/index.js` | Vue Router with auth guards |
| `utils/request.js` | Axios with JWT interceptors |
| `stores/user.js` | Pinia auth state |

## External Dependencies (Not in Repo)

These must be provided separately:
- `docker_image_v1.0/models/binary_model.pth`
- `docker_image_v1.0/models/multi_model.pth`
- `blast_db/db/ARGNet_DB*` - BLAST database files

## Development Patterns

- **Backend**: Service layer pattern, `Result<T>` wrapper for all responses, `I18nUtil` for localization
- **Frontend**: Composition API (`<script setup>`), lazy-loaded routes, Axios interceptors for auth tokens
- **Response format**: `{"code": 0, "message": "...", "data": {...}}`

## Conventions

- Large files (models, DBs, MAG data) should not be modified unnecessarily
- Frontend has `node_modules/` - avoid unnecessary reinstalls
- When adding demo materials, create new directories instead of modifying existing structure
