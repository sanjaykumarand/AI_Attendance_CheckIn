<center><h1>----AI Attendance CheckIN----</h1></center> <br>
<h2>----Created by <a href="https://www.linkedin.com/in/sanjaykumarand"> D Sanjay Kumaran </a>---- <br>
----Only for Personal Educational Project Purpose----
<br></h2>

<h3>•Click Blue Text to read Disclaimer - <a href="https://github.com/sanjaykumarand/AI_Attendance_CheckIn/blob/main/DISCLAIMER.md">DISCLAIMER</a> <br>
•Click Blue Text to read Privacy Policy - <a href="https://github.com/sanjaykumarand/AI_Attendance_CheckIn/blob/main/PRIVACY_POLICY.md">Privacy Policy</a> <br>
•Click Blue Text to read about License - <a href="https://github.com/sanjaykumarand/AI_Attendance_CheckIn/blob/main/LICENSE.md">License</a> <br> <br>

◊For Installation Click Blue Text to see - <a href="https://github.com/sanjaykumarand/AI_Attendance_CheckIn/blob/main/INSTALLATION.md">Installation Guide</a></h3> <br>

<h3>◉ Features:
<br><br>
AI Attendance Management — Features<br><br>

Core Attendance System<br><br>

Personal roster registration (name, roll number, phone number, email) — no biometric data collected<br>
Email-based OTP verification for check-in (anti-proxy protection, prevents one student checking in for another)<br>
OTP auto-expires after 5 minutes, single-use only<br>
Duplicate check-in prevention (can't mark attendance twice in the same day)<br>
Local SQLite database — all personal data stored on-device, never uploaded to any cloud database<br><br>

AI-Powered Reporting (Gemini API)<br><br>

Natural-language attendance summaries generated from raw records<br>
Multi-day pattern detection (e.g., chronic absenteeism, repeated lateness)<br>
Custom date-range reports (today, last 7 days, last 30 days, or any custom range)<br>
Interactive Q&A prompt box — ask free-form questions about attendance data and get AI-generated answers grounded in your actual records<br>
Hardcoded identity/creator responses (bypasses AI for 100% consistent branding answers, e.g., "who created this app")<br><br>

Three Dedicated Desktop Applications (JavaFX)<br><br>

Register App — add students, view full roster in a table, delete a student's data (with confirmation dialog) — supports "right to be forgotten"<br>
Check-in App — simple roll number + OTP flow for daily attendance marking<br>
Report App — date-range picker, quick-select buttons (Today/Week/Month), AI report generation, save report to file, plus the Q&A prompt box<br><br>

Backend Architecture<br><br>

FastAPI REST API server bridging all three JavaFX apps to a single shared database<br>
Clean separation of concerns: registration, OTP/check-in, and reporting each have dedicated endpoints<br>
CORS-enabled for flexible client connections<br><br>

Branding & UI<br><br>

Custom app icons per application (distinct emoji-based icons generated for Register, Check-in, and Report apps)<br>
Consistent app identity across all three apps<br><br>

Privacy & Governance<br><br>

No face, voice, or biometric recognition used anywhere in the system<br>
Explicit consent-based registration workflow<br>
Full data deletion capability built into the Register app<br>
Documented Privacy Policy, Disclaimer, and License included in the project<br>
.gitignore configured to prevent accidental exposure of the database file and API credentials<br><br>

Tech Stack<br><br>

Backend: Python, FastAPI, SQLite, Google Gemini API, smtplib (email OTP)<br>
Frontend: Java, JavaFX, Maven<br>
AI: Google Gemini (gemini-3.6-flash)<br>
</h3>