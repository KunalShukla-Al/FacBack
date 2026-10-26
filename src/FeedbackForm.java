import javax.swing.*;
import java.util.List;

public class FeedbackForm extends JFrame {

    static final String[] CRITERIA = {"Teaching clarity", "Punctuality", "Doubt solving", "Overall"};

    String username;
    JComboBox<String> cmbDept = new JComboBox<>();
    JComboBox<String> cmbTeacher = new JComboBox<>();
    JComboBox<String> cmbCourse = new JComboBox<>();
    List<Object[]> teachers, courses;
    ButtonGroup[] groups = new ButtonGroup[4];
    JRadioButton[][] radios = new JRadioButton[4][5];
    JTextArea txtComment = new JTextArea();

    FeedbackForm(String username) {
        this.username = username;
        setTitle("FacBack - Give Feedback");
        setSize(520, 500);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel note = new JLabel("Anonymous: your name is never saved with your feedback.");
        note.setBounds(20, 10, 470, 25);
        add(note);

        String[] labels = {"Department", "Teacher", "Subject"};
        JComboBox<?>[] boxes = {cmbDept, cmbTeacher, cmbCourse};
        for (int i = 0; i < 3; i++) {
            JLabel l = new JLabel(labels[i]);
            l.setBounds(20, 45 + i * 35, 120, 25);
            add(l);
            boxes[i].setBounds(150, 45 + i * 35, 330, 25);
            add(boxes[i]);
        }
        cmbDept.addActionListener(e -> loadTeachers());
        cmbTeacher.addActionListener(e -> loadCourses());

        for (int c = 0; c < 4; c++) {
            JLabel l = new JLabel(CRITERIA[c]);
            l.setBounds(20, 160 + c * 35, 130, 25);
            add(l);
            groups[c] = new ButtonGroup();
            for (int r = 0; r < 5; r++) {
                radios[c][r] = new JRadioButton(String.valueOf(r + 1));
                radios[c][r].setBounds(150 + r * 60, 160 + c * 35, 55, 25);
                groups[c].add(radios[c][r]);
                add(radios[c][r]);
            }
        }

        JLabel lc = new JLabel("Comment");
        lc.setBounds(20, 305, 100, 25);
        add(lc);
        txtComment.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(txtComment);
        scroll.setBounds(150, 305, 330, 80);
        add(scroll);

        JButton btnSubmit = new JButton("Submit");
        btnSubmit.setBounds(150, 400, 100, 30);
        add(btnSubmit);
        btnSubmit.addActionListener(e -> submit());

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBounds(380, 400, 100, 30);
        add(btnLogout);
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginForm().setVisible(true);
        });

        try {
            for (String d : Database.getDepartments()) cmbDept.addItem(d);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void loadTeachers() {
        cmbTeacher.removeAllItems();
        try {
            teachers = Database.getTeachers((String) cmbDept.getSelectedItem());
            for (Object[] t : teachers) cmbTeacher.addItem((String) t[1]);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void loadCourses() {
        cmbCourse.removeAllItems();
        int t = cmbTeacher.getSelectedIndex();
        if (t < 0) return;
        try {
            courses = Database.getCourses((Integer) teachers.get(t)[0]);
            for (Object[] c : courses) cmbCourse.addItem((String) c[1]);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void submit() {
        int c = cmbCourse.getSelectedIndex();
        if (c < 0) {
            JOptionPane.showMessageDialog(this, "Choose a subject");
            return;
        }
        int[] ratings = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int r = 0; r < 5; r++) {
                if (radios[i][r].isSelected()) ratings[i] = r + 1;
            }
            if (ratings[i] == 0) {
                JOptionPane.showMessageDialog(this, "Please rate " + CRITERIA[i]);
                return;
            }
        }
        try {
            Database.submitReview(username, (Integer) courses.get(c)[0], ratings, txtComment.getText().trim());
            JOptionPane.showMessageDialog(this, "Thank you. Your feedback was saved anonymously.");
            for (ButtonGroup g : groups) g.clearSelection();
            txtComment.setText("");
        } catch (Database.TooSoonException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
