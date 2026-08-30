# 🚀 Quick Start Guide - AI Resume Analyzer

## Current Status: ✅ ALL BUILD ARTIFACTS READY

Build artifacts are located at:
- **Backend JAR**: `backend/target/resume-analyzer-api-1.0.0.jar` (112.86 MB)
- **Frontend**: `frontend/dist/resume-pulse/` (built and optimized)

---

## 🎯 FASTEST WAY TO RUN

### Terminal 1: Start Backend Service

```powershell
# Set up environment
$env:JAVA_HOME = 'C:\Program Files\Amazon Corretto\jdk21.0.12_9'
$env:M2_HOME = 'C:\tools\apache-maven-3.8.1'
$env:Path = "$env:M2_HOME\bin;$env:Path"

# Navigate to project
cd c:\Arun-Projects\AI-Resume-main\AI-Resume-main\backend

# Run the application
java -jar target/resume-analyzer-api-1.0.0.jar
```

**Expected Output:**
```
2026-08-28 02:30:15 INFO - Starting ResumeAnalyzerApplication v1.0.0
2026-08-28 02:30:18 INFO - Started ResumeAnalyzerApplication in 3.2 seconds
2026-08-28 02:30:20 INFO - Tomcat started on port 10000
```

### Terminal 2: Serve Frontend

```powershell
# Navigate to frontend
cd c:\Arun-Projects\AI-Resume-main\AI-Resume-main\frontend

# Install simple HTTP server (one time)
npm install -g http-server

# Serve the built application
http-server dist/resume-pulse -p 8080
```

**Expected Output:**
```
Starting up http-server...
Listening on http://0.0.0.0:8080
Hit CTRL-C to stop the server
```

### Terminal 3 (Optional): Alternative Frontend Server

```powershell
cd c:\Arun-Projects\AI-Resume-main\AI-Resume-main\frontend
npm start
```

---

## 🌐 Access the Application

Open your browser and navigate to:

- **Frontend**: http://localhost:8080 (or 4200 with `npm start`)
- **Backend API**: http://localhost:10000/api
- **Health Check**: http://localhost:10000/actuator/health

---

## ⚠️ IMPORTANT: Database Setup Required

Before running, set up MySQL:

```bash
# Start MySQL Service
net start MySQL80

# Or open MySQL Shell and create database
mysql -u root -p
CREATE DATABASE resume_analyzer;
```

Then set environment variables for backend:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/resume_analyzer'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your_mysql_password'
$env:GROQ_API_KEY = 'your_groq_api_key_here'
$env:SERVER_PORT = '10000'
```

---

## 📋 What Was Fixed

✅ Removed duplicate `frontendd/` folder
✅ Installed Maven 3.8.1
✅ Built backend JAR successfully (no errors)
✅ Fixed npm execution policy
✅ Built frontend successfully (no errors)
✅ Created comprehensive documentation

---

## 🎓 Project Structure

```
AI-Resume-main/
├── backend/                           # Spring Boot API
│   ├── pom.xml                       # Maven build config
│   ├── src/main/java/...            # Java source code
│   └── target/
│       └── resume-analyzer-api-1.0.0.jar ✅
│
├── frontend/                          # Angular Application
│   ├── package.json                  # npm dependencies
│   ├── src/app/...                  # Angular components
│   └── dist/resume-pulse/           ✅ (production build)
│
├── BUILD_SUMMARY.md                 # Detailed build info
└── ERROR_SUMMARY.md                 # Issues & solutions
```

---

## 🔍 Verification

Confirm everything is ready:

```powershell
# Check Java
java -version

# Check Maven
mvn -v

# Check npm
npm -v

# Check backend JAR exists
Test-Path "c:\Arun-Projects\AI-Resume-main\AI-Resume-main\backend\target\resume-analyzer-api-1.0.0.jar"

# Check frontend build exists
Test-Path "c:\Arun-Projects\AI-Resume-main\AI-Resume-main\frontend\dist\resume-pulse\index.html"
```

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| "Port already in use" | Change port: `$env:SERVER_PORT = '8081'` |
| "Database connection failed" | Start MySQL: `net start MySQL80` |
| "GROQ_API_KEY not found" | Set env: `$env:GROQ_API_KEY = 'your_key'` |
| "npm: command not found" | Check Node.js is installed: `node -v` |
| "Maven not found" | Set JAVA_HOME: `$env:JAVA_HOME = 'C:\Program Files\Amazon Corretto\jdk21.0.12_9'` |

---

## 📊 Application Features

Once running, you can:

1. **Upload Resume** - PDF or DOCX format
2. **AI Analysis** - Uses Groq AI for intelligent resume analysis
3. **Chat Interface** - Ask questions about your resume
4. **Dashboard** - View resume analysis results
5. **User Auth** - Login/Register with email verification

---

## 🎉 SUCCESS!

Your AI Resume Analyzer is ready to run! 

Follow the steps above to start both services and access the application. 

For more information, see:
- `BUILD_SUMMARY.md` - Complete build details
- `ERROR_SUMMARY.md` - Resolved issues and configuration

**Happy analyzing!** 🚀
