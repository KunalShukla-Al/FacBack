package ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dao.Dao;
import dao.Student;
import dao.SubjectStat;
import logic.AttendanceGapRule;
import logic.ContextScore;
import logic.Flag;
import logic.GapRule;
import logic.MarksGapRule;
import logic.NotEnoughDataException;
import logic.StudentSubject;

/**
 * The Faculty Advisor's view of one section. Pick a student and see, per
 * subject, how they score and attend against the class, a 1..5 Context Score,
 * and any gap the rules flag — a subject the student is slipping in against
 * their own baseline. The FA reads this by name (performance and conduct are
 * not anonymous); the Reviews behind the averages never are. A flag the FA has
 * acted on is logged, so it isn't chased twice.
 */
public class FaDashboardForm extends JFrame {

    static final String SECTION = "AS1";
    final transient Dao dao = new Dao();
    final transient List<GapRule> rules = List.of(new MarksGapRule(), new AttendanceGapRule());

    final DefaultTableModel model = new DefaultTableModel(
        new String[] {"Subject", "Student %", "Class %", "Context", "Attendance %", "Flag"}, 0);
    final JComboBox<Student> studentBox = new JComboBox<>();
    final DefaultListModel<Flag> flagModel = new DefaultListModel<>();
    final JList<Flag> flagList = new JList<>(flagModel);
    final JComboBox<String> outcomeBox =
        new JComboBox<>(new String[] {"Spoke to the student", "Nothing unusual", "Escalated to the HoD"});
    final JTextField note = new JTextField();

    public FaDashboardForm() {
        setTitle("FacBack - Section dashboard");
        setSize(960, 560);
        setLayout(null);
        setLocationRelativeTo(null);

        JLabel head = new JLabel("Faculty Advisor - Section " + SECTION
            + ". Shown by name for performance and conduct; the reviews behind the averages stay anonymous.");
        head.setBounds(20, 10, 920, 25);
        add(head);

        studentBox.setBounds(20, 42, 300, 28);
        studentBox.setRenderer(new StudentRenderer());
        studentBox.addActionListener(e -> showStudent());
        add(studentBox);

        JTable table = new JTable(model);
        table.setDefaultEditor(Object.class, null);
        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        table.getColumnModel().getColumn(5).setPreferredWidth(320);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(20, 80, 920, 240);
        add(scroll);

        JLabel flagsHead = new JLabel("Flags to look at:");
        flagsHead.setBounds(20, 330, 200, 22);
        add(flagsHead);
        flagList.setCellRenderer(new FlagRenderer());
        JScrollPane flagScroll = new JScrollPane(flagList);
        flagScroll.setBounds(20, 355, 600, 120);
        add(flagScroll);

        JLabel outLabel = new JLabel("Outcome:");
        outLabel.setBounds(640, 355, 120, 22);
        add(outLabel);
        outcomeBox.setBounds(640, 380, 300, 28);
        add(outcomeBox);
        note.setBounds(640, 415, 300, 28);
        note.setToolTipText("A short note (optional)");
        add(note);
        JButton log = new JButton("Log outcome");
        log.setBounds(640, 450, 160, 30);
        log.addActionListener(e -> logOutcome());
        add(log);

        JButton close = new JButton("Close");
        close.setBounds(820, 450, 120, 30);
        close.addActionListener(e -> dispose());
        add(close);

        loadStudents();
    }

    private void loadStudents() {
        try {
            int sectionId = dao.sectionId(SECTION);
            if (sectionId == -1) {
                JOptionPane.showMessageDialog(this, "No section " + SECTION + " yet. Run the seeders first.");
                return;
            }
            for (Student s : dao.students(sectionId)) studentBox.addItem(s);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void showStudent() {
        Student s = (Student) studentBox.getSelectedItem();
        model.setRowCount(0);
        flagModel.clear();
        if (s == null) return;
        try {
            int sectionId = dao.sectionId(SECTION);
            List<SubjectStat> stats = dao.subjectStats(s.id(), sectionId);
            double overallScore = mean(stats.stream().map(SubjectStat::studentScorePct));
            double overallAtt = mean(stats.stream().map(SubjectStat::attendancePct));

            for (SubjectStat st : stats) {
                StudentSubject view = new StudentSubject(
                    st.name(), st.studentScorePct(), overallScore, st.attendancePct(), overallAtt);
                List<String> flags = new ArrayList<>();
                for (GapRule rule : rules) {
                    rule.check(view).ifPresent(f -> {
                        flags.add(f.message());
                        flagModel.addElement(f);
                    });
                }
                model.addRow(new Object[] {
                    st.name(),
                    pct(st.studentScorePct()),
                    pct(st.classScorePct()),
                    context(st),
                    pct(st.attendancePct()),
                    String.join("; ", flags),
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void logOutcome() {
        Student s = (Student) studentBox.getSelectedItem();
        Flag flag = flagList.getSelectedValue();
        if (s == null) return;
        if (flag == null) {
            JOptionPane.showMessageDialog(this, "Pick a flag to log an outcome against.");
            return;
        }
        try {
            dao.logOutcome(s.id(), flag.kind(), (String) outcomeBox.getSelectedItem(), note.getText());
            note.setText("");
            JOptionPane.showMessageDialog(this, "Logged.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    /** The Context Score cell: n/5, or a dash when the class is too small or there's no score. */
    private String context(SubjectStat st) {
        if (st.studentScorePct() == null || st.classScorePct() == null || !ContextScore.visible(st.classSize())) {
            return "-";
        }
        try {
            return ContextScore.band(st.studentScorePct(), st.classScorePct()) + "/5";
        } catch (NotEnoughDataException e) {
            return "-";
        }
    }

    private static double mean(java.util.stream.Stream<Double> values) {
        return values.filter(v -> v != null).mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static String pct(Double v) {
        return v == null ? "-" : String.format("%.0f%%", v);
    }

    private static class StudentRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Student s) setText(s.name());
            return this;
        }
    }

    private static class FlagRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Flag f) setText(f.kind() + ": " + f.message());
            return this;
        }
    }
}
