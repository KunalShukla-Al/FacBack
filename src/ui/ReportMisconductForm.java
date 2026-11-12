package ui;

import dao.DataAccessException;
import dao.MediaStore;
import dao.ReportDao;
import dao.ReportException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;

/** A student's report: a subject, one photo or video, and an optional note. */
public class ReportMisconductForm extends JFrame {

    private final String reporter;
    private final ReportDao dao = new ReportDao();
    private final List<Object[]> courses;
    private final JComboBox<String> cmbCourse = new JComboBox<>();
    private final JLabel lblFile = new JLabel("No file chosen");
    private final JTextArea txtNote = new JTextArea();
    private File chosen;

    public ReportMisconductForm(String reporter) {
        this.reporter = reporter;
        setTitle("FacBack - Report misconduct");
        setSize(520, 380);
        setLayout(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel hidden = new JLabel("The teacher never sees your name.");
        hidden.setBounds(20, 10, 470, 25);
        add(hidden);
        JLabel traceable = new JLabel("The system keeps it, so an admin can trace false reports.");
        traceable.setBounds(20, 32, 470, 25);
        add(traceable);

        JLabel lblSubject = new JLabel("Subject");
        lblSubject.setBounds(20, 70, 120, 25);
        add(lblSubject);
        cmbCourse.setBounds(150, 70, 330, 25);
        add(cmbCourse);

        JButton btnChoose = new JButton("Choose photo or video");
        btnChoose.setBounds(150, 110, 200, 28);
        add(btnChoose);
        btnChoose.addActionListener(e -> choose());
        lblFile.setBounds(150, 145, 330, 25);
        add(lblFile);

        JLabel lblNote = new JLabel("Note (optional)");
        lblNote.setBounds(20, 185, 120, 25);
        add(lblNote);
        txtNote.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(txtNote);
        scroll.setBounds(150, 185, 330, 80);
        add(scroll);

        JButton btnSubmit = new JButton("Submit");
        btnSubmit.setBounds(150, 290, 100, 30);
        add(btnSubmit);
        btnSubmit.addActionListener(e -> submit());

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBounds(380, 290, 100, 30);
        add(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        List<Object[]> loaded;
        try {
            loaded = dao.courseChoices();
        } catch (DataAccessException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            loaded = List.of();
        }
        courses = loaded;
        for (Object[] c : courses) cmbCourse.addItem((String) c[1]);
    }

    private void choose() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
            "Photo or video", "jpg", "jpeg", "png", "mp4", "mov", "webm"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chosen = fc.getSelectedFile();
            lblFile.setText(chosen.getName());
        }
    }

    private void submit() {
        int i = cmbCourse.getSelectedIndex();
        if (i < 0 || chosen == null) {
            JOptionPane.showMessageDialog(this, "Choose a subject and a photo or video.");
            return;
        }
        String stored = null;
        try {
            stored = MediaStore.store(chosen.toPath());
            dao.insert(reporter, (Integer) courses.get(i)[0], stored, txtNote.getText());
            JOptionPane.showMessageDialog(this, "Report filed. Thank you.");
            dispose();
        } catch (ReportException | DataAccessException ex) {
            if (stored != null) MediaStore.discard(stored);
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }
}
