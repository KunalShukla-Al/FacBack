import javax.swing.*;

public class LoginForm extends JFrame {

    JTextField txtUser = new JTextField();
    JPasswordField txtPass = new JPasswordField();

    LoginForm() {
        setTitle("FacBack - Login");
        setSize(360, 210);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel l1 = new JLabel("Username");
        l1.setBounds(30, 25, 80, 25);
        add(l1);
        txtUser.setBounds(110, 25, 200, 25);
        add(txtUser);

        JLabel l2 = new JLabel("Password");
        l2.setBounds(30, 60, 80, 25);
        add(l2);
        txtPass.setBounds(110, 60, 200, 25);
        add(txtPass);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(110, 105, 95, 30);
        add(btnLogin);
        btnLogin.addActionListener(e -> login());

        JButton btnRegister = new JButton("New Student");
        btnRegister.setBounds(215, 105, 115, 30);
        add(btnRegister);
        btnRegister.addActionListener(e -> new RegisterForm(this).setVisible(true));
    }

    void login() {
        try {
            String username = txtUser.getText().trim();
            if (username.toUpperCase().startsWith("RA")) username = username.toUpperCase();
            Object[] user = Database.login(username, new String(txtPass.getPassword()));
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Wrong username or password");
                return;
            }
            dispose();
            String role = (String) user[0];
            if (role.equals("STUDENT")) {
                JOptionPane.showMessageDialog(null,
                    "Your login is used only to make sure you give one response per subject\n"
                  + "(and wait " + Database.COOLDOWN_DAYS + " days before reviewing the same subject again).\n\n"
                  + "It is never saved with your feedback. Teachers and the admin cannot see who wrote what.",
                    "Your feedback is anonymous", JOptionPane.INFORMATION_MESSAGE);
                new FeedbackForm(username).setVisible(true);
            }
            else if (role.equals("TEACHER")) {
                int n = Database.newReviewsSinceLastLogin(username, (Integer) user[1]);
                String msg = n == 0 ? "No new reviews since your last login."
                    : n + (n == 1 ? " new review" : " new reviews") + " since your last login.";
                JOptionPane.showMessageDialog(null, msg, "Welcome", JOptionPane.INFORMATION_MESSAGE);
                new ResultsForm((Integer) user[1]).setVisible(true);
            }
            else new ResultsForm(0).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        new LoginForm().setVisible(true);
    }
}
