# Misconduct report — division

The [blueprint](BLUEPRINT.md) broken into one small commit a day, uploaded from 4 Nov 2026 — after
the hearsay hackathon (due 3 Nov), so its final push stays clear. Owner follows the own-the-code
split: plumbing is Claude's, the project code is mine. Any day an exam eats slides to the next, same
as the AcadKit plan.

| Day | Commit | Owner | Obstacle to classify |
|---|---|---|---|
| Tue 4 Nov | Blueprint (`docs/misconduct/BLUEPRINT.md`) | Claude, I review | — |
| Wed 5 Nov | *skip — TBVP FT-IV* | — | — |
| Thu 6 Nov | `MisconductReport` table in `schema.sql` (idempotent; media as a path) | Claude (plumbing) | design — column shape |
| Fri 7 Nov | `dao.ReportDao` — insert, reports-for-teacher, log outcome + a report exception | Claude (plumbing) | knowledge — JDBC I've seen |
| Sat 8 Nov | `ui.ReportMisconductForm` — subject dropdown, `JFileChooser` for pic/video, note, submit | **me** (core UI) | design — chooser + validation |
| Sun 9 Nov | Teacher queue screen — reports for their subjects, open media, log outcome | mixed (Claude wires data, I lay it out) | design — screen layout |
| Mon 10 Nov | PBL-2 review | — | — |
| after 10 Nov | `TriageService` interface + `StubTriage`; queue ranks by its output | **me** (design, hearsay pattern) | design — interface + stub |

By the 10 Nov review: a working report → teacher-queue flow, with the triage seam (stub now, local
vision model later) as a future-scope slide. The graded FA dashboard stays untouched throughout.
