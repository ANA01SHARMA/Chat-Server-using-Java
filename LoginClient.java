import javax.swing.*; // GUI Components (JFrame, JButton, JTextField, etc.)
import java.awt.*;  // Layouts and Fonts
import java.sql.*;  // JDBC for database connectivity

public class LoginClient extends JFrame {
    JTextField usernameField;
    JPasswordField passwordField;
    JButton loginButton, registerButton;
    JLabel statusLabel;
    String url="jdbc:mysql://localhost:3306/data";
    String pass="Anamika@1";
    String user_sql="root";

    public LoginClient() {
        setTitle("Login Page");
        setSize(400, 300); 
        setLocationRelativeTo(null); //Centers the window on the screen.
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new GridLayout(6, 1, 10, 10)); //Arranges components in 6 rows and 1 column, with 10px gaps. 

        usernameField = new JTextField(); // Text field for username input
        passwordField = new JPasswordField(); // Password field for password input
        loginButton = new JButton("Login");    // Button to trigger login action
        registerButton = new JButton("Register"); // Button to trigger registration action
        statusLabel = new JLabel("", SwingConstants.CENTER); // Label to display status messages

        add(new JLabel("Username:", SwingConstants.CENTER)); // Adds a label for username
        add(usernameField);
        add(new JLabel("Password:", SwingConstants.CENTER)); // Adds a label for password
        add(passwordField);
        add(loginButton);
        add(registerButton);
        add(statusLabel);

        // Login button action
        loginButton.addActionListener(e -> { //Using lambda to shorten 'public void actionPerformed(ActionEvent e)'
            String user = usernameField.getText(); 
            String pass = new String(passwordField.getPassword());

            if (authenticate(user, pass)) {
                statusLabel.setText("Login Successful!");
                dispose(); // Close login window
                new SimpleChatClient(user); // Open chat client
            } else {
                statusLabel.setText("Invalid credentials.");
            }
        });

        // Register button action
        registerButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Please fill both fields.");
                return; // stops the rest of the code
            }

            if (userExists(user)) {
                statusLabel.setText("Username already exists.");
            } else {
                if (registerUser(user, pass)) {
                    statusLabel.setText("User registered. You can login.");
                } else {
                    statusLabel.setText("Registration failed.");
                }
            }
        });

        setVisible(true);
    }

    //User existence check
    private boolean userExists(String username) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // JDBC driver for MySQL
            // Establish connection to the database
            Connection conn = DriverManager.getConnection(url,user_sql,pass);

            String query = "SELECT * FROM users WHERE username = ?"; //The ? is a placeholder → later we will replace it with the actual username.
            PreparedStatement pst = conn.prepareStatement(query); //Used to execute parameterized SQL queries
            pst.setString(1, username); // Set the first parameter (username) in the query
            ResultSet rs = pst.executeQuery();

            boolean exists = rs.next(); // This checks if there is a record with the given username and returns true if it exists.
            //true → if username exists in DB
            //false → if username is new

            rs.close(); // Close ResultSet
            pst.close(); // Close PreparedStatement
            conn.close(); // Disconnect from the database

            return exists; // Return true if user exists, false otherwise

        } catch (Exception e) {
            e.printStackTrace();
            return false; 
        }
    }

    // Authenticate user credentials
    private boolean authenticate(String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url,user_sql,pass);

            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            boolean success = rs.next(); // This checks if there is a record with the given username and password and moves to next record.

            rs.close();
            pst.close();
            conn.close();

            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Register new user
    private boolean registerUser(String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url,user_sql,pass);

            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, username);
            pst.setString(2, password);

            int rows = pst.executeUpdate(); // This executes the insert query and returns the number of rows affected.

            pst.close();
            conn.close();

            return rows > 0; // Returns true if at least one row was inserted, false otherwise

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Main method to launch login window
    public static void main(String[] args) {
         SwingUtilities.invokeLater(()->new LoginClient()); // Ensures that the GUI is created on the Event Dispatch Thread (EDT)
    }
}
