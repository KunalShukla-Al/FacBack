import javax.swing.*;

public class AddTeacherForm extends JDialog {

    AddTeacherForm(ResultsForm admin) {
        super(admin, "Add Teacher", true);
        setSize(420, 330);
        setLayout(null);
        setLocationRelativeTo(admin);

        JComboBox<String> cmbDept = new JComboBox<>();
        cmbDept.setEditable(true);
        try {
            for (String d : Database.getDepartments()) cmbDept.addItem(d);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }

        JTextField txtName = new JTextField();
        JTextField txtCode = new JTextField();
        JTextField txtSubject = new JTextField();
        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();

        String[] labels = {"Teacher name", "Department", "Subject code", "Subject name", "Username", "Password"};
        JComponent[] fields = {txtName, cmbDept, txtCode, txtSubject, txtUser, txtPass};
        for (int i = 0; i < 6; i++) {
            JLabel l = new JLabel(labels[i]);
            l.setBounds(30, 20 + i * 35, 110, 25);
            add(l);
            fields[i].setBounds(140, 20 + i * 35, 240, 25);
            add(fields[i]);
        }

        JButton btnSave = new JButton("Save");
        btnSave.setBounds(140, 235, 90, 30);
        add(btnSave);
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String dept = cmbDept.getEditor().getItem().toString().trim();
            String code = txtCode.getText().trim().toUpperCase();
            String subject = txtSubject.getText().trim();
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            if (name.isEmpty() || dept.isEmpty() || code.isEmpty() || subject.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill in every field");
                return;
            }
            try {
                Database.addTeacher(name, dept, code, subject, user, pass);
                JOptionPane.showMessageDialog(this, name + " added with " + code + ". They can log in as " + user + ".");
                admin.refresh();
                dispose();
            } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this, "That username is already taken");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
    }
}
