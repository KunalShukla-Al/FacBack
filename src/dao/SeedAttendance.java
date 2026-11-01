package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Turn the aggregate attendance (subjects.csv carries each subject's
 * `held_classes`) into the per-session rows the schema wants, so the dashboard
 * and the attendance rule read real sessions, not a single percentage.
 *
 * For each student and subject it lays `held_classes` class days back from a
 * fixed date (one per weekday) and marks present or absent to hit a target
 * attendance. The target is drawn from a seed fixed by reg number and code, so
 * the run is the same every time; about one subject in five is the student's
 * weak one, which is what the attendance gap is meant to surface. All demo data.
 *
 *   javac -cp lib/mysql-connector-j-9.3.0.jar -d bin src/*.java src/dao/*.java
 *   java  -cp bin:lib/mysql-connector-j-9.3.0.jar dao.SeedAttendance
 */
public class SeedAttendance {

    static final LocalDate LAST_CLASS = LocalDate.of(2026, 10, 24);

    public static void main(String[] args) throws IOException, SQLException {
        Map<String, Integer> held = readSubjects(Path.of("data", "subjects.csv"));
        List<String> regs = readRegNos(Path.of("data", "students.csv"));

        try (Connection con = Dao.connect()) {
            Map<String, Integer> courseByCode = lookup(con, "SELECT code, course_id FROM Courses");
            Map<String, Integer> studentByReg = lookup(con, "SELECT reg_no, student_id FROM Students");

            List<Object[]> rows = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            for (String reg : regs) {
                Integer studentId = studentByReg.get(reg);
                if (studentId == null) continue;
                for (Map.Entry<String, Integer> e : held.entrySet()) {
                    Integer courseId = courseByCode.get(e.getKey());
                    if (courseId == null) {
                        if (!skipped.contains(e.getKey())) skipped.add(e.getKey());
                        continue;
                    }
                    rows.addAll(sessions(studentId, courseId, e.getValue(), target(reg, e.getKey())));
                }
            }
            int n = Dao.insertAttendance(con, rows);
            System.out.println("Attendance: " + n + " session rows for " + regs.size() + " students.");
            for (String code : skipped) System.out.println("  no course for " + code + ", skipped");
        }
    }

    /** A student's attendance aim in a subject: ~78–92%, or ~50–64% for a weak one in five. */
    static int target(String reg, String code) {
        Random r = new Random((reg + ":" + code).hashCode());
        boolean weak = r.nextInt(5) == 0;
        return weak ? 50 + r.nextInt(15) : 78 + r.nextInt(15);
    }

    /** `held` class days back from LAST_CLASS, present/absent to meet the target. */
    static List<Object[]> sessions(int studentId, int courseId, int held, int targetPct) {
        int present = Math.round(held * targetPct / 100f);
        boolean[] here = new boolean[held];
        for (int i = 0; i < present && i < held; i++) here[i] = true;
        Random r = new Random((studentId * 31L + courseId));
        for (int i = here.length - 1; i > 0; i--) {   // shuffle which sessions were missed
            int j = r.nextInt(i + 1);
            boolean t = here[i]; here[i] = here[j]; here[j] = t;
        }
        List<Object[]> rows = new ArrayList<>();
        LocalDate d = LAST_CLASS;
        for (int i = 0; i < held; i++) {
            while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.minusDays(1);
            rows.add(new Object[] {studentId, courseId, d, here[i]});
            d = d.minusDays(1);
        }
        return rows;
    }

    static Map<String, Integer> readSubjects(Path p) throws IOException {
        Map<String, Integer> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(p);
        for (String line : lines.subList(1, lines.size())) {
            String[] c = line.split(",");
            if (c.length >= 3 && !c[0].isBlank()) map.put(c[0].trim(), Integer.parseInt(c[2].trim()));
        }
        return map;
    }

    static List<String> readRegNos(Path p) throws IOException {
        List<String> regs = new ArrayList<>();
        List<String> lines = Files.readAllLines(p);
        for (String line : lines.subList(1, lines.size())) {
            String[] c = line.split(",");
            if (c.length >= 1 && !c[0].isBlank()) regs.add(c[0].trim());
        }
        return regs;
    }

    static Map<String, Integer> lookup(Connection con, String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        ResultSet rs = con.createStatement().executeQuery(sql);
        while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        return map;
    }
}
