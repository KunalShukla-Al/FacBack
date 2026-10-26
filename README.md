# FacBack

Anonymous teacher feedback for SRM classes. Students rate the teacher of each subject on four
criteria and can add a comment. Teachers see results for their own subjects only, and the admin
sees every department.

A Java Swing desktop app with a MySQL database, built for the Advanced Object Oriented
Programming course (21CSC206P) at SRM Institute of Science and Technology.

## How reviews stay anonymous

- The `Reviews` table has no student column. A review stores its scores, the comment and the
  month (`YYYY-MM`), nothing else.
- To stop repeat reviews, `Submissions` keeps a SHA-256 hash of the RA number and course, with the
  date. The same student can review the same subject again after 14 days.

## Roles

| Role | What they can do |
|---|---|
| Student | Register with an RA number, pick department, teacher and subject, submit a review |
| Teacher | See averages and comments for their own subjects, and how many reviews are new since their last login |
| Admin | See every teacher's results, add a teacher with their subject and login |

## Running it

1. Start MySQL 8 locally. `Database.java` connects as `root` with no password; change it there if
   yours differs.
2. Create the database and sample data:

   ```
   mysql -u root < schema.sql
   ```

3. In Eclipse, use File → Import → Existing Projects into Workspace and pick this folder. It needs
   JDK 21; the MySQL connector is already in `lib/`. Run `LoginForm`.

Without Eclipse:

```
javac -cp lib/mysql-connector-j-9.3.0.jar -d bin src/*.java
java -cp bin:lib/mysql-connector-j-9.3.0.jar LoginForm
```

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

## Reviews

- **PBL-1:** title, purpose and design. Done.
- **PBL-2, 9 Nov 2026:** a view for the faculty advisor of one section. In progress.

## Team

Kunal Shukla, Kavish Bohra, Aniruddha Bose.
