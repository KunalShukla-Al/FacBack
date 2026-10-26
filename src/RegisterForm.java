import javax.swing.*;

public class RegisterForm extends JDialog {

    JTextField txtRegNo = new JTextField();
    JPasswordField txtPass = new JPasswordField();
    JPasswordField txtConfirm = new JPasswordField();

    RegisterForm(JFrame owner) {
        super(owner, "New Student", true);
        setSize(360, 230);
        setLayout(null);
        setLocationRelativeTo(owner);

        String[] labels = {"Register No", "Password", "Confirm"};
        JTextField[] fields = {txtRegNo, txtPass, txtConfirm};
        for (int i = 0; i < 3; i++) {
            JLabel l = new JLabel(labels[i]);
            l.setBounds(30, 20 + i * 35, 90, 25);
            add(l);
            fields[i].setBounds(120, 20 + i * 35, 190, 25);
            add(fields[i]);
        }

        JButton btnCreate = new JButton("Create");
        btnCreate.setBounds(120, 130, 90, 30);
        add(btnCreate);
        btnCreate.addActionListener(e -> {
            String regNo = txtRegNo.getText().trim().toUpperCase();
            String pass = new String(txtPass.getPassword());
            if (!regNo.startsWith("RA") || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter your RA register number and a password");
            } else if (!pass.equals(new String(txtConfirm.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Passwords do not match");
            } else {
                try {
                    Database.registerStudent(regNo, pass);
                    JOptionPane.showMessageDialog(this, "Account created. Log in with " + regNo);
                    dispose();
                } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
                    JOptionPane.showMessageDialog(this, "This register number already has an account");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
    }
}
