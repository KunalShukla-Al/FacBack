import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class ResultsForm extends JFrame {

    int teacherId;
    DefaultTableModel model = new DefaultTableModel(new String[] {
        "Dept", "Teacher", "Code", "Subject", "Reviews", "Clarity", "Punctuality", "Doubts", "Overall", "Result"}, 0);
    DefaultTableModel comments = new DefaultTableModel(new String[] {"Month", "Overall", "Comment"}, 0);
    JTable table = new JTable(model);
    List<Object[]> results;

    ResultsForm(int teacherId) {
        this.teacherId = teacherId;
        setTitle(teacherId == 0 ? "FacBack - Admin" : "FacBack - My Feedback");
        setSize(1000, 540);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel note = new JLabel("Reviews are anonymous: no student names are ever shown. Select a row to read its comments.");
        note.setBounds(20, 10, 950, 25);
        add(note);

        table.setDefaultEditor(Object.class, null);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(230);
        table.getColumnModel().getColumn(9).setPreferredWidth(140);
        table.getSelectionModel().addListSelectionListener(e -> showComments());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(20, 40, 945, 190);
        add(scroll);

        JTable commentTable = new JTable(comments);
        commentTable.setDefaultEditor(Object.class, null);
        commentTable.getColumnModel().getColumn(2).setPreferredWidth(600);
        JScrollPane scroll2 = new JScrollPane(commentTable);
        scroll2.setBounds(20, 240, 945, 190);
        add(scroll2);

        if (teacherId == 0) {
            JButton btnTeacher = new JButton("Add Teacher");
            btnTeacher.setBounds(20, 445, 140, 30);
            add(btnTeacher);
            btnTeacher.addActionListener(e -> new AddTeacherForm(this).setVisible(true));
        } else {
            JButton btnImport = new JButton("Import Marks");
            btnImport.setBounds(20, 445, 140, 30);
            add(btnImport);
            btnImport.addActionListener(e -> new ImportMarksForm(teacherId).setVisible(true));
        }

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBounds(845, 445, 120, 30);
        add(btnLogout);
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginForm().setVisible(true);
        });

        refresh();
    }

    void refresh() {
        model.setRowCount(0);
        comments.setRowCount(0);
        try {
            results = Database.getResults(teacherId);
            for (Object[] r : results) {
                int n = (Integer) r[5];
                if (n == 0) {
                    model.addRow(new Object[] {r[1], r[2], r[3], r[4], n, "-", "-", "-", "-", "No reviews yet"});
                } else {
                    double score = FeedbackCalc.score((Double) r[6], (Double) r[7], (Double) r[8], (Double) r[9]);
                    model.addRow(new Object[] {r[1], r[2], r[3], r[4], n, f(r[6]), f(r[7]), f(r[8]), f(r[9]),
                        FeedbackCalc.band(score)});
                }
            }
            if (results.isEmpty()) {
                comments.addRow(new Object[] {"", "", "No subjects are linked to you yet. Ask the admin to add your subjects."});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    static String f(Object avg) {
        return String.format("%.1f", (Double) avg);
    }

    void showComments() {
        comments.setRowCount(0);
        int row = table.getSelectedRow();
        if (row < 0) return;
        if ((Integer) results.get(row)[5] == 0) {
            comments.addRow(new Object[] {"", "", "No reviews yet for this subject."});
            return;
        }
        try {
            for (Object[] c : Database.getComments((Integer) results.get(row)[0])) comments.addRow(c);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
