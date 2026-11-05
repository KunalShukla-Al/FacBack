import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Database {

    public static final int COOLDOWN_DAYS = 14;

    public static class TooSoonException extends Exception {
        TooSoonException(String message) {
            super(message);
        }
    }

    static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/facback", "root", "");
    }

    static Object[] login(String username, String password) throws SQLException {
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT role, teacher_id FROM Users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new Object[] {rs.getString("role"), rs.getInt("teacher_id")};
        }
    }

    static int newReviewsSinceLastLogin(String username, int teacherId) throws SQLException {
        try (Connection con = connect()) {
            PreparedStatement count = con.prepareStatement(
                "SELECT COUNT(*) FROM Reviews r JOIN Courses c ON c.course_id = r.course_id "
              + "WHERE c.teacher_id = ? AND r.review_id > (SELECT last_seen_review FROM Users WHERE username = ?)");
            count.setInt(1, teacherId);
            count.setString(2, username);
            ResultSet rs = count.executeQuery();
            rs.next();
            int newReviews = rs.getInt(1);

            PreparedStatement seen = con.prepareStatement(
                "UPDATE Users SET last_seen_review = (SELECT COALESCE(MAX(review_id), 0) FROM Reviews) WHERE username = ?");
            seen.setString(1, username);
            seen.executeUpdate();
            return newReviews;
        }
    }

    static void registerStudent(String regNo, String password) throws SQLException {
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO Users (username, password, role) VALUES (?, ?, 'STUDENT')");
            ps.setString(1, regNo);
            ps.setString(2, password);
            ps.executeUpdate();
        }
    }

    static List<String> getDepartments() throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection con = connect()) {
            ResultSet rs = con.createStatement().executeQuery("SELECT DISTINCT dept FROM Teachers ORDER BY dept");
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    static List<Object[]> getTeachers(String dept) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT teacher_id, name FROM Teachers WHERE dept = ? ORDER BY name");
            ps.setString(1, dept);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Object[] {rs.getInt(1), rs.getString(2)});
        }
        return list;
    }

    static List<Object[]> getCourses(int teacherId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT course_id, code, name FROM Courses WHERE teacher_id = ? ORDER BY code");
            ps.setInt(1, teacherId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Object[] {rs.getInt(1), rs.getString(2) + " - " + rs.getString(3)});
        }
        return list;
    }

    static String hash(String username, int courseId) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest((username + ":" + courseId).getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    static void submitReview(String username, int courseId, int[] ratings, String comment) throws Exception {
        String key = hash(username, courseId);
        LocalDate today = LocalDate.now();
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try {
                PreparedStatement check = con.prepareStatement(
                    "SELECT last_submitted FROM Submissions WHERE hash = ? FOR UPDATE");
                check.setString(1, key);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    LocalDate nextAllowed = rs.getDate(1).toLocalDate().plusDays(COOLDOWN_DAYS);
                    if (today.isBefore(nextAllowed)) {
                        long days = ChronoUnit.DAYS.between(today, nextAllowed);
                        throw new TooSoonException("You reviewed this subject recently. You can review it again in "
                            + days + " day(s), from " + nextAllowed + ".");
                    }
                }

                PreparedStatement sub = con.prepareStatement(
                    "INSERT INTO Submissions VALUES (?, ?) ON DUPLICATE KEY UPDATE last_submitted = VALUES(last_submitted)");
                sub.setString(1, key);
                sub.setDate(2, Date.valueOf(today));
                sub.executeUpdate();

                PreparedStatement rev = con.prepareStatement(
                    "INSERT INTO Reviews (course_id, clarity, punctuality, doubt_solving, overall, comment, review_month) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?)");
                rev.setInt(1, courseId);
                for (int i = 0; i < 4; i++) rev.setInt(i + 2, ratings[i]);
                rev.setString(6, comment);
                rev.setString(7, today.toString().substring(0, 7));
                rev.executeUpdate();

                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    static List<Object[]> getResults(int teacherId) throws SQLException {
        String sql = "SELECT c.course_id, t.dept, t.name, c.code, c.name, COUNT(r.review_id), "
                   + "AVG(r.clarity), AVG(r.punctuality), AVG(r.doubt_solving), AVG(r.overall) "
                   + "FROM Courses c JOIN Teachers t ON t.teacher_id = c.teacher_id "
                   + "LEFT JOIN Reviews r ON r.course_id = c.course_id "
                   + "WHERE ? = 0 OR t.teacher_id = ? "
                   + "GROUP BY c.course_id ORDER BY t.dept, t.name, c.code";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, teacherId);
            ps.setInt(2, teacherId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[] {rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getInt(6), rs.getDouble(7), rs.getDouble(8), rs.getDouble(9), rs.getDouble(10)});
            }
        }
        return list;
    }

    static List<Object[]> getComments(int courseId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT review_month, overall, comment FROM Reviews WHERE course_id = ? ORDER BY review_month DESC");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Object[] {rs.getString(1), rs.getInt(2), rs.getString(3)});
        }
        return list;
    }

    static void addTeacher(String name, String dept, String code, String subject,
                           String username, String password) throws SQLException {
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try {
                PreparedStatement t = con.prepareStatement(
                    "INSERT INTO Teachers (name, dept) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                t.setString(1, name);
                t.setString(2, dept);
                t.executeUpdate();
                ResultSet keys = t.getGeneratedKeys();
                keys.next();
                int teacherId = keys.getInt(1);

                PreparedStatement c = con.prepareStatement("INSERT INTO Courses (teacher_id, code, name) VALUES (?, ?, ?)");
                c.setInt(1, teacherId);
                c.setString(2, code);
                c.setString(3, subject);
                c.executeUpdate();

                PreparedStatement u = con.prepareStatement(
                    "INSERT INTO Users (username, password, role, teacher_id) VALUES (?, ?, 'TEACHER', ?)");
                u.setString(1, username);
                u.setString(2, password);
                u.setInt(3, teacherId);
                u.executeUpdate();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** Outcome of a marks import: how many rows landed, and reg numbers not on the roster. */
    static class ImportResult {
        final int imported;
        final List<String> unknownRegNos;

        ImportResult(int imported, List<String> unknownRegNos) {
            this.imported = imported;
            this.unknownRegNos = unknownRegNos;
        }
    }

    /**
     * Upsert marks for one test of a course from parsed reg_no -> score rows.
     * Finds the test by (course, label), creating it (or refreshing its max) so a
     * freshly announced test imports in one step. Reg numbers absent from the
     * roster are skipped and returned, so the caller can show which ones missed.
     * All or nothing: a database error rolls the whole import back.
     */
    static ImportResult importMarks(int courseId, String label, java.math.BigDecimal maxMarks,
                                    java.util.Map<String, java.math.BigDecimal> scores) throws SQLException {
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try {
                int testId;
                PreparedStatement find = con.prepareStatement(
                    "SELECT test_id FROM Tests WHERE course_id = ? AND label = ?");
                find.setInt(1, courseId);
                find.setString(2, label);
                ResultSet rs = find.executeQuery();
                if (rs.next()) {
                    testId = rs.getInt(1);
                    PreparedStatement upd = con.prepareStatement(
                        "UPDATE Tests SET max_marks = ? WHERE test_id = ?");
                    upd.setBigDecimal(1, maxMarks);
                    upd.setInt(2, testId);
                    upd.executeUpdate();
                } else {
                    PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO Tests (course_id, label, max_marks) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                    ins.setInt(1, courseId);
                    ins.setString(2, label);
                    ins.setBigDecimal(3, maxMarks);
                    ins.executeUpdate();
                    ResultSet keys = ins.getGeneratedKeys();
                    keys.next();
                    testId = keys.getInt(1);
                }

                PreparedStatement findStudent = con.prepareStatement(
                    "SELECT student_id FROM Students WHERE reg_no = ?");
                PreparedStatement mark = con.prepareStatement(
                    "INSERT INTO Marks (student_id, test_id, score) VALUES (?, ?, ?) "
                  + "ON DUPLICATE KEY UPDATE score = VALUES(score)");
                int imported = 0;
                List<String> unknown = new ArrayList<>();
                for (java.util.Map.Entry<String, java.math.BigDecimal> e : scores.entrySet()) {
                    findStudent.setString(1, e.getKey());
                    ResultSet sr = findStudent.executeQuery();
                    if (!sr.next()) {
                        unknown.add(e.getKey());
                        continue;
                    }
                    mark.setInt(1, sr.getInt(1));
                    mark.setInt(2, testId);
                    mark.setBigDecimal(3, e.getValue());
                    mark.addBatch();
                    imported++;
                }
                mark.executeBatch();
                con.commit();
                return new ImportResult(imported, unknown);
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }
}
