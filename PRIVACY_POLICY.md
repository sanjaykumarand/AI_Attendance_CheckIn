# PRIVACY POLICY

**Project:** AI Attendance Management
**Created by:** Sanjay Kumaran D
**Scope:** Personal project — data used strictly for local, personal, academic purposes only

---

## 1. What Data Is Collected

This system collects the following personal data, entered manually by the
project owner with each individual's consent:

- Full name
- Roll number
- Phone number
- Email address
- Attendance timestamps (date and time of check-in)

**No biometric data is collected.** This system does not use face
recognition, voice recognition, fingerprints, or any other biometric
identifier.

## 2. Where Data Is Stored

All personal data is stored **locally**, in a single SQLite database file
(`attendance.db`) on the project owner's own machine. This file is:

- Not uploaded to any cloud storage or public server
- Not shared with any third party for storage purposes
- Excluded from version control (via `.gitignore`) to prevent accidental
  public exposure

## 3. What Data Is Sent to Third Parties (and Why)

| Data sent | Sent to | Purpose |
|---|---|---|
| Roster (name, roll number) + attendance timestamps | Google Gemini API | To generate natural-language attendance summaries and pattern reports |
| OTP code + recipient email | Gmail SMTP | To deliver the one-time verification code for check-in |

No phone numbers, and no data beyond what's listed above, are sent to any
external service. Data sent to Gemini is used only to generate that specific
report/answer and is subject to Google's own data handling policies for API
usage (see: https://ai.google.dev/gemini-api/terms).

## 4. Consent

Before any individual is registered in this system, they must be informed
that:
- Their name, roll number, phone number, and email will be stored
- Their attendance timestamps will be logged
- Summarized (non-biometric) data may be sent to Google's Gemini API for
  report generation
- They may request their data be deleted at any time

Only individuals who explicitly agree should be registered.

## 5. Right to Access and Deletion

Any registered individual may, at any time, request:
- To see what data is stored about them
- To have their data permanently deleted

Deletion can be performed immediately via the Register app's "Delete
Student" feature, which permanently removes both their registration record
and all associated attendance history from `attendance.db`.

## 6. Data Retention

Data is retained only as long as needed for the project's personal/academic
purpose. There is no automatic expiration — the project owner is responsible
for deleting data when it's no longer needed (e.g., end of semester/project).

## 7. Security Measures

- OTP codes expire after 5 minutes and are single-use
- API keys and email credentials are stored as environment variables, not
  hardcoded in version-controlled files
- The database file and credential-containing scripts are excluded from any
  public repository via `.gitignore`

Note: as a personal academic project, this system has **not** undergone
formal security auditing and should not be treated as enterprise-grade
secure.

## 8. Children's Data

If any registered individual is a minor, additional parental/guardian consent
should be obtained by the project owner before registration, in line with
applicable local regulations.

## 9. Changes to This Policy

As this is a personal project, this policy may be updated informally as the
project evolves. Registered individuals will be informed of significant
changes where practical.

## 10. Contact

For questions about this policy or to request data deletion, contact the
project owner directly: Sanjay Kumaran D.

---

*Last updated: 2026*
