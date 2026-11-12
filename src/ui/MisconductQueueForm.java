package ui;

import dao.DataAccessException;
import dao.Report;
import dao.ReportDao;
import dao.ReportException;
import logic.ReportTriage;
import logic.StubTriage;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** The teacher's queue: reports for their subjects, the media to open, and an outcome to log. */
public class MisconductQueueForm extends JFrame {

    static final String[] OUTCOMES = {"Confirmed", "Not confirmed", "Needs more evidence"};
    static final List<String> IMAGE_TYPES = List.of("jpg", "jpeg", "png");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final int teacherId;
    private final ReportDao dao = new ReportDao();
    private final ReportTriage triage = new StubTriage();
    private List<Report> queue = List.of();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JLabel lblDetail = new JLabel(" ");
    private final JComboBox<String> cmbOutcome = new JComboBox<>(OUTCOMES);
    private final JTextArea txtNote = new JTextArea();

    public MisconductQueueForm(int teacherId) {
        this.teacherId = teacherId;
        setTitle("FacBack - Misconduct queue");
        setSize(520, 470);
        setLayout(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBounds(20, 20, 470, 170);
        add(listScroll);
        list.addListSelectionListener(e -> showSelected());

        lblDetail.setBounds(20, 195, 470, 60);
        lblDetail.setVerticalAlignment(SwingConstants.TOP);
        add(lblDetail);

        JButton btnOpen = new JButton("Open media");
        btnOpen.setBounds(20, 260, 130, 28);
        add(btnOpen);
        btnOpen.addActionListener(e -> openMedia());

        JLabel lblOutcome = new JLabel("Outcome");
        lblOutcome.setBounds(20, 300, 80, 25);
        add(lblOutcome);
        cmbOutcome.setBounds(110, 300, 200, 25);
        add(cmbOutcome);

        txtNote.setLineWrap(true);
        JScrollPane noteScroll = new JScrollPane(txtNote);
        noteScroll.setBounds(20, 335, 470, 50);
        add(noteScroll);

        JButton btnLog = new JButton("Log outcome");
        btnLog.setBounds(20, 395, 130, 30);
        add(btnLog);
        btnLog.addActionListener(e -> logOutcome());

        reload();
    }

    /** Loads the teacher's reports. Likely cases come first; equal scores keep newest-first order. */
    private void reload() {
        try {
            queue = dao.reportsForTeacher(teacherId).stream()
                .sorted(Comparator.comparingDouble((Report r) -> triage.score(r.mediaPath())).reversed())
                .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }
        model.clear();
        for (Report r : queue) {
            String status = r.outcome() == null ? "" : "  [" + r.outcome() + "]";
            model.addElement(r.courseCode() + "  " + r.reportedAt().format(WHEN) + status);
        }
        lblDetail.setText(" ");
        txtNote.setText("");
    }

    private void showSelected() {
        int i = list.getSelectedIndex();
        if (i < 0) return;
        Report r = queue.get(i);
        String note = r.note() == null ? "no note" : r.note();
        StringBuilder html = new StringBuilder("<html>" + r.courseName() + " · " + note);
        for (String line : dao.outcomeHistory(r.id())) html.append("<br>").append(line);
        lblDetail.setText(html.append("</html>").toString());
    }

    private void openMedia() {
        int i = list.getSelectedIndex();
        if (i < 0) {
            JOptionPane.showMessageDialog(this, "Pick a report first.");
            return;
        }
        File file = new File(queue.get(i).mediaPath());
        try {
            if (IMAGE_TYPES.contains(extension(file))) {
                JOptionPane.showMessageDialog(this, new JLabel(fit(file)), file.getName(), JOptionPane.PLAIN_MESSAGE);
            } else {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Couldn't open the file: " + ex.getMessage());
        }
    }

    /** The image scaled to fit a 640 × 480 box, so a large photo still fits the dialog. */
    private static ImageIcon fit(File file) {
        ImageIcon icon = new ImageIcon(file.getPath());
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w <= 640 && h <= 480) return icon;
        double scale = Math.min(640.0 / w, 480.0 / h);
        return new ImageIcon(icon.getImage().getScaledInstance(
            (int) (w * scale), (int) (h * scale), Image.SCALE_SMOOTH));
    }

    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void logOutcome() {
        int i = list.getSelectedIndex();
        if (i < 0) {
            JOptionPane.showMessageDialog(this, "Pick a report first.");
            return;
        }
        try {
            dao.logOutcome(teacherId, queue.get(i).id(), (String) cmbOutcome.getSelectedItem(), txtNote.getText());
        } catch (ReportException | DataAccessException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }
        reload();
    }
}
