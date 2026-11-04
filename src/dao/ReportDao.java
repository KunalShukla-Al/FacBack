package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for misconduct reports. The reporter column is always written, so
 * an admin can trace abuse, but {@link #reportsForTeacher} never selects it — the
 * teacher stays blind to who reported. Media lives on disk; only its path is stored.
 */
public class ReportDao {

    /**
     * File a report. The reporter is the logged-in username, mediaPath points at
     * the photo or video the student uploaded, and note is optional. The timestamp
     * is the server's own, so it can be vouched for.
     *
     * @throws ReportException when the report carries no media.
     */
    public void insert(String reporter, int courseId, String mediaPath, String note)
            throws ReportException {
        if (mediaPath == null || mediaPath.isBlank())
            throw new ReportException("A report needs a photo or video.");
        try (Connection con = Dao.connect()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO MisconductReports (reporter, course_id, media_path, note, reported_at) "
              + "VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, reporter);
            ps.setInt(2, courseId);
            ps.setString(3, mediaPath);
            ps.setString(4, note == null || note.isBlank() ? null : note);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't file the report", e);
        }
    }

    /** The reports for the teacher's own subjects, newest first. The reporter is never read. */
    public List<Report> reportsForTeacher(int teacherId) {
        List<Report> out = new ArrayList<>();
        try (Connection con = Dao.connect()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT r.report_id, c.code, c.name, r.media_path, r.note, r.reported_at, "
              + "r.outcome, r.outcome_note, r.outcome_on "
              + "FROM MisconductReports r JOIN Courses c ON c.course_id = r.course_id "
              + "WHERE c.teacher_id = ? ORDER BY r.reported_at DESC");
            ps.setInt(1, teacherId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalDate outcomeOn = rs.getDate("outcome_on") == null
                    ? null : rs.getDate("outcome_on").toLocalDate();
                out.add(new Report(
                    rs.getInt("report_id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("media_path"),
                    rs.getString("note"),
                    rs.getTimestamp("reported_at").toLocalDateTime(),
                    rs.getString("outcome"),
                    rs.getString("outcome_note"),
                    outcomeOn));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't read the reports", e);
        }
        return out;
    }

    /** Record the teacher's outcome on a report, with the server's date. */
    public void logOutcome(int reportId, String outcome, String note) {
        try (Connection con = Dao.connect()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE MisconductReports SET outcome = ?, outcome_note = ?, outcome_on = ? "
              + "WHERE report_id = ?");
            ps.setString(1, outcome);
            ps.setString(2, note == null || note.isBlank() ? null : note);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            ps.setInt(4, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Couldn't log the outcome", e);
        }
    }
}
