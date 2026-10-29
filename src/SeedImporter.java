import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the AcadKit-derived class data (generated seed CSVs) into the PBL-2
 * tables. Every step is idempotent, so a re-run never duplicates. Reviews stay
 * anonymous — nothing here links a student to a review.
 *
 * Courses are reconciled additively: a subject missing from Courses is added,
 * existing rows are never touched (sample Reviews depend on their course_ids).
 * Teacher assignments for added courses are placeholders over Faculty A–F.
 */
public class SeedImporter {

    public static void main(String[] args) throws Exception {
        String dataDir = args.length > 0 ? args[0] : "data";
        try (Connection conn = Database.connect()) {
            int students = importStudents(conn, Paths.get(dataDir, "students.csv"));
            int courses = importCourses(conn, Paths.get(dataDir, "subjects.csv"));
            int tests = importTests(conn, Paths.get(dataDir, "marks.csv"));
            int marks = importMarks(conn, Paths.get(dataDir, "marks.csv"));
            System.out.printf("Students %d | Courses added %d | Tests %d | Marks %d%n",
                    students, courses, tests, marks);
        }
    }

    // --- roster ---------------------------------------------------------

    static int importStudents(Connection conn, Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        Map<String, Integer> sections = new HashMap<>();
        String sql = "INSERT IGNORE INTO Students (reg_no, name, section_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lines.size(); i++) {
                String[] f = split(lines.get(i));
                if (f == null) {
                    continue;
                }
                Integer sectionId = sections.computeIfAbsent(f[2], k -> sectionId(conn, k));
                ps.setString(1, f[0]);
                ps.setString(2, f[1]);
                ps.setInt(3, sectionId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count(conn, "Students");
    }

    // --- courses (additive) ---------------------------------------------

    static int importCourses(Connection conn, Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        Set<String> existing = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT code FROM Courses");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                existing.add(rs.getString(1));
            }
        }
        int[] teachers = teacherIds(conn);
        int added = 0, nth = 0;
        String ins = "INSERT INTO Courses (teacher_id, code, name) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            for (int i = 1; i < lines.size(); i++) {
                String[] f = split(lines.get(i));
                if (f == null || existing.contains(f[0])) {
                    continue;
                }
                ps.setInt(1, teachers[nth++ % teachers.length]);  // placeholder teacher
                ps.setString(2, f[0]);
                ps.setString(3, f[1]);
                ps.executeUpdate();
                existing.add(f[0]);
                added++;
            }
        }
        return added;
    }

    // --- tests ----------------------------------------------------------

    static int importTests(Connection conn, Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        Map<String, Integer> courses = new HashMap<>();
        Set<String> seen = new HashSet<>();
        String ins = "INSERT IGNORE INTO Tests (course_id, label, max_marks) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            for (int i = 1; i < lines.size(); i++) {
                String[] f = split(lines.get(i));
                if (f == null) {
                    continue;
                }
                Integer courseId = courseId(conn, courses, f[1]);
                if (courseId == null) {
                    continue;
                }
                if (!seen.add(courseId + "|" + f[3])) {
                    continue;
                }
                ps.setInt(1, courseId);
                ps.setString(2, f[3]);
                ps.setBigDecimal(3, new BigDecimal(f[5]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count(conn, "Tests");
    }

    // --- marks ----------------------------------------------------------

    static int importMarks(Connection conn, Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        Map<String, Integer> courses = new HashMap<>();
        Map<String, Integer> students = new HashMap<>();
        Map<String, Integer> tests = new HashMap<>();
        int loaded = 0;
        String ins = "INSERT INTO Marks (student_id, test_id, score) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE score = VALUES(score)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            for (int i = 1; i < lines.size(); i++) {
                String[] f = split(lines.get(i));
                if (f == null) {
                    continue;
                }
                Integer courseId = courseId(conn, courses, f[1]);
                if (courseId == null) {
                    continue;
                }
                Integer studentId = lookup(conn, students, f[0],
                        "SELECT student_id FROM Students WHERE reg_no = ?");
                Integer testId = tests.computeIfAbsent(courseId + "|" + f[3],
                        k -> testId(conn, courseId, f[3]));
                if (studentId == null || testId == null) {
                    continue;
                }
                ps.setInt(1, studentId);
                ps.setInt(2, testId);
                ps.setBigDecimal(3, new BigDecimal(f[4]));
                ps.addBatch();
                loaded++;
            }
            ps.executeBatch();
        }
        return loaded;
    }

    // --- helpers --------------------------------------------------------

    /** Trim a CSV line and split on commas; null for a blank line. */
    static String[] split(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] f = trimmed.split(",", -1);
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
        }
        return f;
    }

    static Integer courseId(Connection conn, Map<String, Integer> cache, String code) {
        return cache.computeIfAbsent(code, k ->
                lookup(conn, new HashMap<>(), k, "SELECT course_id FROM Courses WHERE code = ? LIMIT 1"));
    }

    static Integer testId(Connection conn, int courseId, String label) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT test_id FROM Tests WHERE course_id = ? AND label = ?")) {
            ps.setInt(1, courseId);
            ps.setString(2, label);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Integer lookup(Connection conn, Map<String, Integer> cache, String key, String sql) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                Integer v = rs.next() ? rs.getInt(1) : null;
                cache.put(key, v);
                return v;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static int[] teacherIds(Connection conn) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT teacher_id FROM Teachers ORDER BY teacher_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        int[] out = new int[ids.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ids.get(i);
        }
        return out;
    }

    static int count(Connection conn, String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static int sectionId(Connection conn, String name) {
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT section_id FROM Sections WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Sections (name, fa_teacher) VALUES (?, NULL)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            throw new SQLException("could not resolve or create section " + name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
