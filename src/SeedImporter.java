import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeedImporter {

    public static void main(String[] args) throws Exception {
        String dataDir = args.length > 0 ? args[0] : "data";
        try (Connection conn = Database.connect()) {
            int students = importStudents(conn, Paths.get(dataDir, "students.csv"));
            System.out.println("Students in roster after import: " + students);
        }
    }

    static int importStudents(Connection conn, Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        Map<String, Integer> sections = new HashMap<>();
        String sql = "INSERT IGNORE INTO Students (reg_no, name, section_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] f = line.split(",", -1);
                Integer sectionId = sections.get(f[2].trim());
                if (sectionId == null) {
                    sectionId = sectionId(conn, f[2].trim());
                    sections.put(f[2].trim(), sectionId);
                }
                ps.setString(1, f[0].trim());
                ps.setString(2, f[1].trim());
                ps.setInt(3, sectionId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Students");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static int sectionId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT section_id FROM Sections WHERE name = ?")) {
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
    }
}
