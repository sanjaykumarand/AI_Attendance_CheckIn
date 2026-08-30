# INSTALLATION GUIDE

**Project:** AI Attendance Management
**Created by:** Sanjay Kumaran D

This guide walks you through setting up the full project from scratch:
backend server, all three JavaFX apps, and your API credentials.

---

## 1. Prerequisites

Install these first if you don't already have them:

| Requirement | Check if installed | Install if missing |
|---|---|---|
| Python 3.10+ | `python --version` | https://www.python.org/downloads/ |
| Java JDK 17+ | `java -version` | `winget install Microsoft.OpenJDK.17` |
| Apache Maven | `mvn -version` | See [Maven setup](#4-maven-setup-if-mvn-is-not-recognized) below |

---

## 2. Clone the Repository

```
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
```

---

## 3. Backend Setup (Python)

### 3.1 Install dependencies

```
pip install fastapi uvicorn google-genai
```

### 3.2 Get a free Gemini API key

1. Go to https://aistudio.google.com/apikey
2. Sign in and click **Create API Key**
3. Copy the key — you'll need it in step 5

### 3.3 Get a Gmail App Password (for OTP emails)

1. Go to https://myaccount.google.com/security
2. Enable **2-Step Verification** if not already on
3. Go to https://myaccount.google.com/apppasswords
4. Create a new app password named "Attendance System"
5. Copy the 16-character password — you'll need it in step 5

> Use a regular Gmail account. College/work Google Workspace accounts often
> have App Passwords disabled by admin policy.

---

## 4. Maven Setup (if `mvn` is not recognized)

1. Download the **Binary zip archive** from https://maven.apache.org/download.cgi
2. Extract it to a permanent folder, e.g. `C:\Program Files\Apache\maven`
3. Note the full path to its `bin` folder (e.g. `C:\Program Files\Apache\maven\bin`)
   — you'll need this in step 5's launch scripts

(You do **not** need to add this to your system PATH — the launch scripts
below add it automatically per-session.)

---

## 5. Create the Launch Scripts

This repo does not include pre-made launch scripts (since they'd contain
your personal file paths and credentials). Create these four `.bat` files
yourself in your project folders, filling in your own values.

### 5.1 `start_backend.bat`
Place in your backend/API project folder:

```bat
@echo off
title AI Attendance - Backend Server
color 0E

set BACKEND_DIR=<path to the attendance-project folder>
set GEMINI_API_KEY=<your Gemini API key from step 3.2>
set OTP_EMAIL_ADDRESS=<your Gmail address>
set OTP_EMAIL_APP_PASSWORD=<your 16-character app password from step 3.3>

cd /d %BACKEND_DIR%
call python attendance_api_server.py

pause
```

### 5.2 `start_register.bat`
Place in your register-app folder:

```bat
@echo off
title AI Attendance - Register Launcher
color 0B

set REGISTER_DIR=<path to register-app folder>
set MAVEN_BIN=<path to your Maven bin folder from step 4>
set PATH=%PATH%;%MAVEN_BIN%

cd /d %REGISTER_DIR%
call mvn clean compile javafx:run

pause
```

### 5.3 `start_report.bat`
Place in your report-app folder (same pattern, swap `REPORT_DIR`):

```bat
@echo off
title AI Attendance - Report Launcher
color 0D

set REPORT_DIR=<path to report-app folder>
set MAVEN_BIN=<path to your Maven bin folder from step 4>
set PATH=%PATH%;%MAVEN_BIN%

cd /d %REPORT_DIR%
call mvn clean compile javafx:run

pause
```

### 5.4 `start_checkin.bat`
Place in your checkin-app folder (same pattern, swap `CHECKIN_DIR`):

```bat
@echo off
title AI Attendance CheckIn
color 0A

set CHECKIN_DIR=<path to checkin-app folder>
set MAVEN_BIN=<path to your Maven bin folder from step 4>
set PATH=%PATH%;%MAVEN_BIN%

cd /d %CHECKIN_DIR%
call mvn clean compile javafx:run

pause
```

> ⚠️ These scripts contain real credentials once filled in — keep them out
> of version control. They're already excluded via `.gitignore`.

---

## 6. First Run

Run each script in this order (each opens its own window):

1. **`start_backend.bat`** — starts the API server at `http://127.0.0.1:8001`.
   Wait until you see "Model loaded" / "Uvicorn running" before continuing.
2. **`start_register.bat`** — opens the Register app. Add at least one
   student here first (with their consent — see `PRIVACY_POLICY.md`).
3. **`start_checkin.bat`** — opens the Check-in app. Enter the roll number
   you just registered, request an OTP, check your email, and verify.
4. **`start_report.bat`** — opens the Report app. Generate a report for
   today to confirm the full pipeline works end-to-end.

First-time Maven runs will take longer (downloading dependencies) — this is
normal and only happens once per app.

---

## 7. Verifying Everything Works

- Backend health check: open `http://127.0.0.1:8001/docs` in a browser —
  you should see the FastAPI interactive docs page.
- `attendance.db` should appear in the `attendance-project` folder after the
  backend's first run.
- A successful check-in should show `✓ Attendance marked successfully!` in
  the Check-in app.

---

## 8. Optional: Convert Scripts to .exe

You can wrap any `.bat` script into a standalone `.exe` using Windows'
built-in **IExpress** tool (Win+R → `iexpress`):

1. Choose **Create new Self Extraction Directive file**
2. Choose **Extract files and run an installation command**
3. Add your `.bat` file when prompted
4. On the **Install Program** screen, type manually:
   `cmd.exe /c start_backend.bat` (swap the filename per script)
   — this avoids a known Windows bug where `.bat` files fail to launch
   through IExpress's default `command.com` association
5. Choose a save location outside Desktop/OneDrive (e.g. `C:\Temp\`) to
   avoid permission issues
6. Finish the wizard — your `.exe` is ready

---

## Troubleshooting

| Problem | Likely fix |
|---|---|
| `mvn is not recognized` | Re-check `MAVEN_BIN` path in the script matches your actual Maven folder |
| `BUILD FAILURE... Output directory is empty` | Your `.java` file isn't in the correct `src/main/java/...` nested folder path |
| OTP email never arrives | Double-check `OTP_EMAIL_APP_PASSWORD` — must be an App Password, not your normal Gmail password |
| `Gemini API key not configured` | `GEMINI_API_KEY` not set correctly in `start_backend.bat` |
| Report/Check-in app can't connect | Make sure `start_backend.bat` is running first, and stays open |

---

For data handling details, see `PRIVACY_POLICY.md`. For usage terms, see
`LICENSE.md` and `DISCLAIMER.md`.
