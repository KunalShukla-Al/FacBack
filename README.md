# FacBack

Anonymous teacher feedback for SRM classes. Students rate the teacher of each subject on four
criteria and can add a comment. Teachers see results for their own subjects only, and the admin
sees every department.

A Java Swing desktop app with a MySQL database, built for the Advanced Object Oriented
Programming course (21CSC206P) at SRM Institute of Science and Technology.

## How reviews stay anonymous

- The `Reviews` table has no student column. A review stores its scores, the comment and the
  month (`YYYY-MM`), nothing else.
- To stop repeat reviews, `Submissions` keeps an HMAC-SHA256 tag of the RA number and course, with the
  date. The same student can review the same subject again after 14 days.

## Roles

| Role | What they can do |
|---|---|
| Student | Register with an RA number, pick department, teacher and subject, submit a review |
| Teacher | See averages and comments for their own subjects, and how many reviews are new since their last login |
| Admin / Faculty Advisor | See every teacher's results, add a teacher, and open the section dashboard |

## Faculty Advisor view (PBL-2)

The review from PBL-1 was that one teacher's marks say little on their own: the same student can do
well in one subject and drift in another. So the Faculty Advisor (the admin, who knows the class by
name) gets a **section dashboard** that reads marks and attendance against each student's *own*
baseline, not the class's, and surfaces where they're slipping — while the reviews behind the
averages stay anonymous.

It's laid out in three packages:

- **`logic`** — the rules and scoring, no database or UI. `ContextScore` turns a student's average
  over the class average into a 1..5 band (hidden for a class under 10, so a thin average can't
  mislead). `GapRule` is the interface each check implements; `MarksGapRule` and `AttendanceGapRule`
  flag a subject that sits 20 points below the student's own average. `NotEnoughDataException` is
  raised when there's nothing fair to compare against.
- **`dao`** — `Dao` reads the roster, each student's marks and attendance against the class, and
  logs the FA's outcome; `SeedImporter` loads the roster, courses, tests and marks; `SeedAttendance`
  lays the aggregate `held_classes` out as per-session rows. `DataAccessException` keeps `java.sql`
  out of the rest.
- **`ui`** — `FaDashboardForm`, reached from the admin screen's **Section dashboard** button.

## Running it

1. Start MySQL 8 locally. Set `FACBACK_DB_URL`, `FACBACK_DB_USER` and
   `FACBACK_DB_PASSWORD` for your database. Local demo defaults are root with an empty password.
2. Create the database and sample data:

   ```
   mysql -u root < schema.sql
   ```

3. In Eclipse, use File → Import → Existing Projects into Workspace and pick this folder. It needs
   JDK 21; the MySQL connector is already in `lib/`. Run `LoginForm`.

Without Eclipse:

```
javac -cp lib/mysql-connector-j-9.3.0.jar -d bin src/*.java src/dao/*.java src/logic/*.java src/ui/*.java
java -cp bin:lib/mysql-connector-j-9.3.0.jar LoginForm
```

For the Faculty Advisor dashboard, load the demo roster, marks and attendance once:

```
java -cp bin:lib/mysql-connector-j-9.3.0.jar dao.SeedImporter
java -cp bin:lib/mysql-connector-j-9.3.0.jar dao.SeedAttendance
```

Then log in as `admin` and open **Section dashboard**. All the marks and attendance are demo data.

Sample logins: `admin` / `admin123`, and `facultyA` to `facultyF` / `pass`. Students register
with any RA number.

## Files

| File | Job |
|---|---|
| `Database.java` | All SQL, through JDBC, and the `TooSoonException` for the 14-day rule |
| `FeedbackCalc.java` | Averages a review's four scores and names the band |
| `LoginForm.java` | Login, and the starting point |
| `RegisterForm.java` | Student registration |
| `FeedbackForm.java` | The review form |
| `ResultsForm.java` | Results for teachers and the admin |
| `AddTeacherForm.java` | Adds a teacher, their subject and login in one transaction |
| `schema.sql` | Tables and sample data |

## Tests

The `logic` package is pure (no database, no UI), so it's unit-tested on its own.
There's no JUnit on the classpath, so the tests are a small self-contained harness:

```bash
javac -d bin src/logic/*.java src/test/LogicTests.java
java  -cp bin test.LogicTests
```

They cover the Context Score bands at each boundary, the zero-class-average guard,
the thin-class visibility cut-off, and both gap rules (a wide shortfall flags; a
small dip or missing data does not).

## Reviews

- **PBL-1:** title, purpose and design. Done.
- **PBL-2, 10 Nov 2026:** a view for the faculty advisor of one section. In progress.

## Team

Kunal Shukla, Kavish Bohra, Aniruddha Bose.

## Setup

- **Database:** the app reads `FACBACK_DB_URL`, `FACBACK_DB_USER` and `FACBACK_DB_PASSWORD`. Without them it uses a local MySQL at `localhost:3306` with user `root` and an empty password.
- **Review key:** set `FACBACK_SECRET`, or let the app create `data/review.key` on first use. Keep this key: changing it restarts every 14-day review cooldown.
- **Existing databases:** run `migrations/001_hashing_and_outcome_history.sql` once. It widens the password column, clears the old unsalted review hashes, and adds the outcome history table. Accounts with plain-text passwords are upgraded to salted hashes on their next login.
- **Tests:** `javac -d bin $(find src -name '*.java') && java -cp bin test.LogicTests`

## PBL-II threading

CSV reads and marks imports use `ui.BackgroundTask`, a `SwingWorker` that runs blocking work off the Swing event-dispatch thread. The form snapshots input, disables controls during work, shows an activity indicator, and handles completion or failure on the EDT. The login screen is created with `SwingUtilities.invokeLater`.

Run the additional thread tests after compiling all sources:

```sh
java -Djava.awt.headless=true -cp bin test.BackgroundTaskTests
```

See [PBL coverage and demo](docs/pbl/PBL-COVERAGE.md) and [report corrections](docs/pbl/REPORT-REVIEW.md).
