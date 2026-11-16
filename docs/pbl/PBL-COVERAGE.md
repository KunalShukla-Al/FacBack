# PBL coverage and demonstration

The supplied assessment table explicitly requires packages, interfaces, strings and threads for Project Review 2. All four are implemented. The table does not supply detailed criteria for Reviews 1 and 3 or the separate unit-based scenario assessments; this mapping does not certify those unspecified criteria.

| Concept | Evidence | Demonstration |
|---|---|---|
| Packages | logic, dao, ui, test | Explain rule, database and UI responsibilities. |
| Interfaces | GapRule with MarksGapRule and AttendanceGapRule; ReportTriage with StubTriage | Show calls through the interface and interchangeable implementations. |
| Strings | ImportMarksForm.parse: split, trim, toUpperCase; StringBuilder results | Paste registration numbers and scores from a spreadsheet. |
| Threads | ui.BackgroundTask extends SwingWorker; ImportMarksForm uses it for CSV reads and imports | Explain doInBackground versus done, input snapshots, busy controls and exception delivery. |

## Threading behavior

LoginForm creates the initial screen with SwingUtilities.invokeLater. ImportMarksForm captures field values on the event-dispatch thread (EDT). BackgroundTask executes CSV reading or parsing and JDBC import on a worker thread. Its done method runs on the EDT, obtains the result with get, delivers success or the original failure, and restores controls in finally. An indeterminate progress bar shows activity; it does not claim a numeric completion percentage. The form disables input and closing during an operation to avoid duplicate imports or misleading cancellation of a database write.

Other screens still perform synchronous database work. This change demonstrates threading in a real workflow; it is not a claim that the entire application is asynchronous.

## Reproducible tests

From the repository root:

```sh
javac -d bin src/*.java src/dao/*.java src/logic/*.java src/ui/*.java src/test/*.java
java -cp bin test.LogicTests
java -Djava.awt.headless=true -cp bin test.BackgroundTaskTests
```

The existing harness contains 11 checks. The background-task harness tests success and failure, verifies worker/EDT separation, proves the EDT processes another event while work is blocked, checks original exception delivery, and verifies EDT cleanup. These are not database integration tests.

## Presentation sequence

1. Explain the feedback problem, roles and database design.
2. Register/login, submit a review and demonstrate the cooldown.
3. Show teacher results and the faculty-advisor dashboard.
4. Open Import Marks, paste demo rows, and explain the worker code and progress indicator.
5. On one demo machine, submit misconduct media and review it through the owning teacher's queue.
6. Show the test output and acknowledge that vision triage is a stub.

Use a demo database. Run the existing-database migration only once: its DELETE FROM Submissions resets cooldowns whenever run. Media uses local paths, and account/course eligibility is not verified by an enrollment model.
