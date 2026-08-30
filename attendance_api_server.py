"""


Smart Attendance API Server
Handles: registration (with phone number), email-OTP verified check-in,
and Gemini-powered reporting. Both JavaFX apps talk to this server.

Setup:
    pip install fastapi uvicorn google-genai

Set environment variables before running:
    set GEMINI_API_KEY=your_gemini_key
    set OTP_EMAIL_ADDRESS=youraccount@gmail.com
    set OTP_EMAIL_APP_PASSWORD=your_16_char_gmail_app_password

Run:
    python attendance_api_server.py
Server: http://127.0.0.1:8001
Docs:   http://127.0.0.1:8001/docs
"""

import sqlite3
import os
import json
import random
import smtplib
import time
from email.mime.text import MIMEText
from datetime import datetime, timedelta

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from google import genai
from google.genai import types

DB_PATH = "attendance.db"
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_API_KEY_HERE")
GEMINI_MODEL = "gemini-3.6-flash"

EMAIL_ADDRESS = os.environ.get("OTP_EMAIL_ADDRESS", "")
EMAIL_APP_PASSWORD = os.environ.get("OTP_EMAIL_APP_PASSWORD", "")

OTP_EXPIRY_SECONDS = 300  # 5 minutes

app = FastAPI(title="Smart Attendance API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# In-memory OTP store: { roll_number: {"otp": "123456", "expires": timestamp} }
otp_store = {}



def init_db():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS students (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roll_number TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL,
            phone_number TEXT NOT NULL,
            email TEXT,
            registered_on TEXT NOT NULL
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS attendance (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            roll_number TEXT NOT NULL,
            date TEXT NOT NULL,
            time TEXT NOT NULL,
            status TEXT NOT NULL
        )
    """)
    conn.commit()
    conn.close()


init_db()




class RegisterRequest(BaseModel):
    roll_number: str
    name: str
    phone_number: str
    email: str  # OTP is sent here (free path); phone_number stored for records/anti-proxy identity


class OtpRequest(BaseModel):
    roll_number: str


class OtpVerifyRequest(BaseModel):
    roll_number: str
    otp: str




def send_otp_email(to_email: str, otp: str, student_name: str):
    if not EMAIL_ADDRESS or not EMAIL_APP_PASSWORD:
        raise RuntimeError("Email credentials not configured. Set OTP_EMAIL_ADDRESS and OTP_EMAIL_APP_PASSWORD.")

    subject = "Your Attendance Check-in OTP"
    body = f"Hi {student_name},\n\nYour attendance check-in OTP is: {otp}\n\nThis code expires in 5 minutes.\n\n- Smart Attendance System"

    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = EMAIL_ADDRESS
    msg["To"] = to_email

    with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
        server.login(EMAIL_ADDRESS, EMAIL_APP_PASSWORD)
        server.sendmail(EMAIL_ADDRESS, to_email, msg.as_string())




@app.post("/register")
def register_student(req: RegisterRequest):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO students (roll_number, name, phone_number, email, registered_on) VALUES (?, ?, ?, ?, ?)",
            (req.roll_number, req.name, req.phone_number, req.email, datetime.now().strftime("%Y-%m-%d")),
        )
        conn.commit()
        return {"status": "registered", "roll_number": req.roll_number}
    except sqlite3.IntegrityError:
        raise HTTPException(status_code=400, detail="Roll number already registered.")
    finally:
        conn.close()


@app.get("/students")
def list_students():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT roll_number, name, phone_number, email, registered_on FROM students ORDER BY roll_number")
    rows = cur.fetchall()
    conn.close()
    return [
        {"roll_number": r, "name": n, "phone_number": p, "email": e, "registered_on": d}
        for r, n, p, e, d in rows
    ]


@app.delete("/students/{roll_number}")
def remove_student(roll_number: str):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("DELETE FROM students WHERE roll_number = ?", (roll_number,))
    cur.execute("DELETE FROM attendance WHERE roll_number = ?", (roll_number,))
    conn.commit()
    deleted = cur.rowcount
    conn.close()
    if not deleted:
        raise HTTPException(status_code=404, detail="Student not found.")
    return {"status": "removed", "roll_number": roll_number}




def get_student(roll_number: str):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT roll_number, name, email FROM students WHERE roll_number = ?", (roll_number,))
    row = cur.fetchone()
    conn.close()
    if not row:
        return None
    return {"roll_number": row[0], "name": row[1], "email": row[2]}


@app.post("/checkin/request-otp")
def request_otp(req: OtpRequest):
    student = get_student(req.roll_number)
    if not student:
        raise HTTPException(status_code=404, detail="Roll number not registered.")

    otp = str(random.randint(100000, 999999))
    otp_store[req.roll_number] = {"otp": otp, "expires": time.time() + OTP_EXPIRY_SECONDS}

    try:
        send_otp_email(student["email"], otp, student["name"])
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send OTP: {e}")

    return {"status": "otp_sent", "name": student["name"]}


@app.post("/checkin/verify-otp")
def verify_otp(req: OtpVerifyRequest):
    entry = otp_store.get(req.roll_number)
    if not entry:
        raise HTTPException(status_code=400, detail="No OTP requested for this roll number.")

    if time.time() > entry["expires"]:
        del otp_store[req.roll_number]
        raise HTTPException(status_code=400, detail="OTP expired. Request a new one.")

    if req.otp.strip() != entry["otp"]:
        raise HTTPException(status_code=400, detail="Incorrect OTP.")

    # OTP correct -> mark attendance
    student = get_student(req.roll_number)
    today = datetime.now().strftime("%Y-%m-%d")
    now_time = datetime.now().strftime("%H:%M:%S")

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT 1 FROM attendance WHERE roll_number = ? AND date = ?", (req.roll_number, today))
    if cur.fetchone():
        conn.close()
        del otp_store[req.roll_number]
        return {"status": "already_checked_in", "name": student["name"]}

    cur.execute(
        "INSERT INTO attendance (roll_number, date, time, status) VALUES (?, ?, ?, ?)",
        (req.roll_number, today, now_time, "present"),
    )
    conn.commit()
    conn.close()

    del otp_store[req.roll_number]
    return {"status": "checked_in", "name": student["name"], "time": now_time}


# ---------- AI Report ----------

class ReportRequest(BaseModel):
    date_from: str
    date_to: str


SYSTEM_INSTRUCTION = """You are an attendance analytics assistant for a college class.
Given a roster and attendance records, identify present/absent students, detect multi-day
absence patterns if present, and write a short, professional, factual summary. Do not speculate
about reasons for absence."""

CREATOR_ANSWER = (
    "AI Attendance Management, created by Sanjay Kumaran D, only for personal project use only. "
    "All data is kept personally within the local machine only."
)

IDENTITY_PATTERNS = [
    "who created", "who made", "who built", "who developed", "creator",
    "who is the developer", "who owns this", "your creator", "made this app",
    "built this app", "developed this", "who designed",
]


def is_identity_question(message: str) -> bool:
    lowered = message.lower().strip()
    return any(p in lowered for p in IDENTITY_PATTERNS)


@app.post("/report")
def generate_report(req: ReportRequest):
    if GEMINI_API_KEY == "YOUR_API_KEY_HERE":
        raise HTTPException(status_code=500, detail="Gemini API key not configured.")

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT roll_number, name FROM students ORDER BY roll_number")
    roster = [{"roll_number": r, "name": n} for r, n in cur.fetchall()]

    cur.execute(
        "SELECT roll_number, date, time, status FROM attendance WHERE date BETWEEN ? AND ? ORDER BY date, time",
        (req.date_from, req.date_to),
    )
    records = [{"roll_number": r, "date": d, "time": t, "status": s} for r, d, t, s in cur.fetchall()]
    conn.close()

    client = genai.Client(api_key=GEMINI_API_KEY)
    payload = {"date_range": f"{req.date_from} to {req.date_to}", "roster": roster, "attendance_records": records}

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=f"Analyze this attendance data:\n\n{json.dumps(payload, indent=2)}",
        config=types.GenerateContentConfig(system_instruction=SYSTEM_INSTRUCTION, temperature=0.2),
    )

    return {"report": response.text, "roster_size": len(roster), "record_count": len(records)}


class AskRequest(BaseModel):
    question: str
    date_from: str = None
    date_to: str = None


ASK_SYSTEM_INSTRUCTION = """You are an assistant answering questions about a college
attendance system's data. You will be given the class roster and attendance records
(a date range, if relevant to the question). Answer the user's question factually and
concisely based only on this data. If the data doesn't contain the answer, say so
plainly instead of guessing."""


@app.post("/ask")
def ask_question(req: AskRequest):
    if is_identity_question(req.question):
        return {"answer": CREATOR_ANSWER}

    if GEMINI_API_KEY == "YOUR_API_KEY_HERE":
        raise HTTPException(status_code=500, detail="Gemini API key not configured.")

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT roll_number, name FROM students ORDER BY roll_number")
    roster = [{"roll_number": r, "name": n} for r, n in cur.fetchall()]

    date_from = req.date_from or "2000-01-01"
    date_to = req.date_to or datetime.now().strftime("%Y-%m-%d")
    cur.execute(
        "SELECT roll_number, date, time, status FROM attendance WHERE date BETWEEN ? AND ? ORDER BY date, time",
        (date_from, date_to),
    )
    records = [{"roll_number": r, "date": d, "time": t, "status": s} for r, d, t, s in cur.fetchall()]
    conn.close()

    client = genai.Client(api_key=GEMINI_API_KEY)
    payload = {"roster": roster, "attendance_records": records, "date_range_considered": f"{date_from} to {date_to}"}

    prompt = f"Data:\n{json.dumps(payload, indent=2)}\n\nQuestion: {req.question}"

    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
        config=types.GenerateContentConfig(system_instruction=ASK_SYSTEM_INSTRUCTION, temperature=0.2),
    )

    return {"answer": response.text}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8001)

    """
    Copyright (c) 2026 Sanjay Kumaran D All Rights Reserved.
    Only for Personal Educational Purpose only
    """
