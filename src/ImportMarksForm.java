import javax.swing.*;
import java.awt.Font;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a teacher gets a test's marks into FacBack without re-typing them: paste
 * the two columns straight from the marks sheet in Excel or Google Sheets, or
 * load a CSV. Each line is reg_no then score, split on a tab or a comma, so a
 * copied spreadsheet selection drops in as-is. Parsing is lenient and reports
 * what it skipped rather than failing the whole paste.
 */
public class ImportMarksForm extends JFrame {

    private final JComboBox<String> courseBox = new JComboBox<>();
    private final List<Integer> courseIds = new ArrayList<>();
    private final JTextField labelField = new JTextField("FT-II");
    private final JTextField maxField = new JTextField("50");
    private final JTextArea area = new JTextArea();

    ImportMarksForm(int teacherId) {
        setTitle("FacBack - Import Marks");
        setSize(640, 560);
        setLayout(null);
        setLocationRelativeTo(null);

        JLabel courseLabel = new JLabel("Course:");
        courseLabel.setBounds(20, 20, 60, 25);
        add(courseLabel);
        courseBox.setBounds(85, 20, 515, 25);
        add(courseBox);

        JLabel labelLabel = new JLabel("Test:");
        labelLabel.setBounds(20, 55, 60, 25);
        add(labelLabel);
        labelField.setBounds(85, 55, 180, 25);
        add(labelField);

        JLabel maxLabel = new JLabel("Max marks:");
        maxLabel.setBounds(300, 55, 90, 25);
        add(maxLabel);
        maxField.setBounds(395, 55, 100, 25);
        add(maxField);

        JLabel hint = new JLabel("Paste rows as  reg_no, score  (one per line; tab or comma). A header row is ignored.");
        hint.setBounds(20, 90, 580, 25);
        add(hint);

        area.setFont(new Font("Menlo", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBounds(20, 120, 580, 330);
        add(scroll);

        JButton loadCsv = new JButton("Load CSV…");
        loadCsv.setBounds(20, 462, 120, 30);
        add(loadCsv);
        loadCsv.addActionListener(e -> loadCsv());

        JButton importBtn = new JButton("Import");
        importBtn.setBounds(390, 462, 100, 30);
        add(importBtn);
        importBtn.addActionListener(e -> doImport());

        JButton close = new JButton("Close");
        close.setBounds(500, 462, 100, 30);
        add(close);
        close.addActionListener(e -> dispose());

        try {
            for (Object[] c : Database.getCourses(teacherId)) {
                courseBox.addItem(c[1].toString());
                courseIds.add((Integer) c[0]);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load your courses: " + ex.getMessage());
        }
        if (courseIds.isEmpty()) {
            courseBox.addItem("No courses linked to you");
            importBtn.setEnabled(false);
        }
    }

    private void loadCsv() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            area.setText(String.join("\n", Files.readAllLines(chooser.getSelectedFile().toPath())));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read the file: " + ex.getMessage());
        }
    }

    private void doImport() {
        if (courseIds.isEmpty()) {
            return;
        }
        String label = labelField.getText().trim();
        if (label.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter the test label, e.g. FT-II.");
            return;
        }
        BigDecimal max;
        try {
            max = new BigDecimal(maxField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Max marks must be a number.");
            return;
        }

        List<String> errors = new ArrayList<>();
        Map<String, BigDecimal> scores = parse(area.getText(), errors);
        if (scores.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid rows to import.\n" + String.join("\n", errors));
            return;
        }

        try {
            Database.ImportResult result = Database.importMarks(
                courseIds.get(courseBox.getSelectedIndex()), label, max, scores);
            StringBuilder msg = new StringBuilder("Imported " + result.imported + " mark(s) for " + label + ".");
            if (!result.unknownRegNos.isEmpty()) {
                msg.append("\n\nNot on the roster, skipped (").append(result.unknownRegNos.size()).append("):\n")
                   .append(String.join(", ", result.unknownRegNos));
            }
            if (!errors.isEmpty()) {
                msg.append("\n\nSkipped lines:\n").append(String.join("\n", errors));
            }
            JOptionPane.showMessageDialog(this, msg.toString());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    /** Split pasted text into reg_no -> score; unparseable lines go into errors. */
    private Map<String, BigDecimal> parse(String text, List<String> errors) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split("[\\t,]+");
            if (f.length < 2) {
                errors.add("line " + (i + 1) + ": need reg_no and score");
                continue;
            }
            String reg = f[0].trim().toUpperCase();
            String scoreText = f[f.length - 1].trim();
            try {
                out.put(reg, new BigDecimal(scoreText));
            } catch (NumberFormatException ex) {
                if (i == 0) {
                    continue;                        // the header row (e.g. "reg_no,score")
                }
                errors.add("line " + (i + 1) + ": '" + scoreText + "' is not a number");
            }
        }
        return out;
    }
}
