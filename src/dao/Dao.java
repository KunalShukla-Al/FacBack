package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for the FA dashboard: the roster of a section, each student's
 * marks and attendance against the class, and the outcome the FA logs. Reads
 * only; it never links a student to a review, so the Reviews stay anonymous.
 */
public class Dao {

    static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/facback", "root", "");
    }

    /** The section's id by name, or -1 when there's no such section. */
    public int sectionId(String name) {
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement("SELECT section_id FROM Sections WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't read the section", e);
        }
    }

    public List<Student> students(int sectionId) {
        List<Student> out = new ArrayList<>();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT student_id, reg_no, name FROM Students WHERE section_id = ? ORDER BY name");
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(new Student(rs.getInt(1), rs.getString(2), rs.getString(3)));
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't read the roster", e);
        }
        return out;
    }

    /**
     * Each subject the student has marks or attendance in: the student's own
     * score and attendance, and the class average and size for the Context Score.
     */
    public List<SubjectStat> subjectStats(int studentId, int sectionId) {
        try (Connection con = connect()) {
            Map<Integer, Double> studentScore = scoreByCourse(con, "m.student_id = ?", studentId);
            Map<Integer, double[]> classScore = classScoreByCourse(con, sectionId);  // [pct, size]
            Map<Integer, Double> attendance = attendanceByCourse(con, studentId);
            Map<Integer, String[]> courses = courseNames(con);

            java.util.Set<Integer> courseIds = new java.util.LinkedHashSet<>();
            courseIds.addAll(studentScore.keySet());
            courseIds.addAll(attendance.keySet());
            List<Integer> ids = new ArrayList<>(courseIds);
            ids.sort((a, b) -> courses.getOrDefault(a, new String[] {"", ""})[0]
                .compareTo(courses.getOrDefault(b, new String[] {"", ""})[0]));

            List<SubjectStat> out = new ArrayList<>();
            for (int id : ids) {
                String[] cn = courses.getOrDefault(id, new String[] {"?", "?"});
                double[] cls = classScore.get(id);
                out.add(new SubjectStat(id, cn[0], cn[1],
                    studentScore.get(id),
                    cls == null ? null : cls[0],
                    cls == null ? 0 : (int) cls[1],
                    attendance.get(id)));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't read the student's record", e);
        }
    }

    /** Record what the FA did about a flag, so the same flag isn't chased twice. */
    public void logOutcome(int studentId, String flagKind, String outcome, String note) {
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO FaOutcomes (student_id, flag_kind, outcome, note, logged_on) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, studentId);
            ps.setString(2, flagKind);
            ps.setString(3, outcome);
            ps.setString(4, note == null || note.isBlank() ? null : note);
            ps.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't log the outcome", e);
        }
    }

    // --- the queries behind subjectStats ------------------------------------

    private Map<Integer, Double> scoreByCourse(Connection con, String where, int id) throws SQLException {
        Map<Integer, Double> map = new LinkedHashMap<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT t.course_id, AVG(m.score / t.max_marks * 100) "
          + "FROM Marks m JOIN Tests t ON t.test_id = m.test_id WHERE " + where + " GROUP BY t.course_id");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) map.put(rs.getInt(1), rs.getDouble(2));
        return map;
    }

    private Map<Integer, double[]> classScoreByCourse(Connection con, int sectionId) throws SQLException {
        Map<Integer, double[]> map = new LinkedHashMap<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT t.course_id, AVG(m.score / t.max_marks * 100), COUNT(DISTINCT m.student_id) "
          + "FROM Marks m JOIN Tests t ON t.test_id = m.test_id "
          + "JOIN Students s ON s.student_id = m.student_id WHERE s.section_id = ? GROUP BY t.course_id");
        ps.setInt(1, sectionId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) map.put(rs.getInt(1), new double[] {rs.getDouble(2), rs.getInt(3)});
        return map;
    }

    private Map<Integer, Double> attendanceByCourse(Connection con, int studentId) throws SQLException {
        Map<Integer, Double> map = new LinkedHashMap<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT course_id, AVG(present) * 100 FROM Attendance WHERE student_id = ? GROUP BY course_id");
        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) map.put(rs.getInt(1), rs.getDouble(2));
        return map;
    }

    private Map<Integer, String[]> courseNames(Connection con) throws SQLException {
        Map<Integer, String[]> map = new LinkedHashMap<>();
        ResultSet rs = con.createStatement().executeQuery("SELECT course_id, code, name FROM Courses");
        while (rs.next()) map.put(rs.getInt(1), new String[] {rs.getString(2), rs.getString(3)});
        return map;
    }

    /** Insert per-session rows (student_id, course_id, class_date, present), skipping any already there. */
    static int insertAttendance(Connection con, List<Object[]> rows) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "INSERT IGNORE INTO Attendance (student_id, course_id, class_date, present) VALUES (?, ?, ?, ?)");
        for (Object[] r : rows) {
            ps.setInt(1, (Integer) r[0]);
            ps.setInt(2, (Integer) r[1]);
            ps.setDate(3, java.sql.Date.valueOf((LocalDate) r[2]));
            ps.setBoolean(4, (Boolean) r[3]);
            ps.addBatch();
        }
        int[] done = ps.executeBatch();
        return done.length;
    }
}
