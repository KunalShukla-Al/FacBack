# FacBack — Misconduct report (blueprint)

One page, written before the code. This is additive: it doesn't touch the anonymous reviews or the
Faculty Advisor dashboard, and nothing here joins a report to a review.

## The problem in one sentence
A student who sees someone cheating in an exam has nowhere safe to report it — telling the
invigilator in the room exposes them, and a raw photo mailed to a teacher proves nothing and could
as easily be used to frame a classmate.

## What it does
A student reports an exam malpractice from inside FacBack: picks the subject, uploads a photo or
short video they already took on their phone, adds an optional note, and submits. The teacher of
that subject sees the class, the subject, the student the report is about, and the media — never who
reported it. The app records nothing itself; it stores what the reporter uploads plus the facts the
server can vouch for (who is logged in, the moment of upload).

## The one idea that holds it together
**Anonymous to the teacher, accountable to the system.** The reporter is hidden from the teacher —
the same promise a review gets — but the system still knows who they are (they're logged in), so an
admin can trace a malicious or repeated false report. That asymmetry is the whole design: it shields
the reporter without handing anyone a frictionless way to frame a classmate. A report is an input to
a human's investigation, never a verdict.

## Who sees what
- **Reporter** — hidden from the teacher; known to the system, so an admin can trace abuse.
- **Accused** — named to the teacher, who identifies them from the image; the roster stays private to students.
- **Teacher** — class, subject, accused, media, time. Not the reporter.
- **Admin** — everything, including who reported, and only to investigate abuse.

## Parts (each one step)
1. **report** — a form: subject (from the student's own courses), a photo/video upload (JFileChooser), an optional note.
2. **store** — a `MisconductReport` row: reporter (hidden from the teacher later), course, server timestamp, the media as a file path (not in the database).
3. **queue** — the teacher's review screen: the reports for their subjects, the media shown, and an outcome they log (reusing the `FaOutcomes` pattern).
4. **triage** *(future)* — a local vision model reads each image for a phone, smartwatch or paper slip and **ranks** the queue by what it saw. It never drops a report, so a miss can't bury a real one.

## Constraints that shaped choices
- Java Swing has no camera and no GPS, so the app captures nothing; it takes a file the reporter already has. A geotag inside that file is shown **labelled "unverified"** — it's editable, so it's a hint, never proof.
- The vision model can't run in Swing and shouldn't: it's a separate **local service the app calls** (the same shape as hearsay's model route), so the model swaps without touching the UI.
- Idempotent schema (`CREATE TABLE IF NOT EXISTS`), like the rest of the PBL-2 tables; no column links a report to a review.

## Out of scope / later
- The vision triage is future scope — a **stub** stands in so the queue works today, exactly as hearsay's model stub does.
- No in-app recording, no live proctoring, no automatic punishment. A human decides.

## Micro-challenges (classify each before asking for help)
storing media safely (path vs blob, size cap) · showing a report only to the right teacher · hiding
the reporter from the teacher while keeping them traceable by an admin · the triage service interface
and its stub · ranking a queue without ever dropping a report.
