// === SimpleChatClientComboBox.java ===

import javax.swing.*;
import java.awt.*;
import java.io.*; // For reading and writing files
import java.net.*; // For socket communication
import java.util.logging.*; // To log events like messages sent, received, errors, etc. into a file.

public class SimpleChatClient extends JFrame {
    Socket socket; // Socket for communication with the server
    DataOutputStream dataOut; // Output stream to send data to the server
    DataInputStream dataIn; // Input stream to receive data from the server

    ImageBackgroundPanel messagePanel; // Custom panel with a background image for displaying messages
    JScrollPane scrollPane;
    JTextField inputField;
    JButton sendButton, attachButton;
    JComboBox<String> userComboBox;
    boolean isBroadcast = true; // Flag to determine if the message is broadcasted or sent to a specific user
    String name;

    private static final Logger logger = Logger.getLogger(SimpleChatClient.class.getName()); // Logger to log events like messages sent, received, errors, etc. into a file.

    public SimpleChatClient(String name) {
        this.name = name;
        setupLogger(); // Initialize the logger to log events into a file named after the client.
        setupGUI(); // Set up the GUI components for the chat client
        connectToServer(); // Connect to the chat server
        startReading(); // Start a thread to read incoming messages from the server
    }

    void setupLogger() { //Create a text file like client_Anamika.txt
        try {
            FileHandler fh = new FileHandler("client_" + name + ".txt", true);
            fh.setFormatter(new SimpleFormatter()); // Simple formatter to log messages in a readable format
            logger.addHandler(fh); // Add the file handler to the logger
            logger.setUseParentHandlers(false); // Disable console logging to avoid duplicate logs in the console
        } catch (IOException e) {
            System.out.println("Logger failed: " + e.getMessage());
        }
    }

    void setupGUI() {
        setTitle("Chat - " + name);
        setSize(500, 600);
        setResizable(false);
        setLayout(new BorderLayout()); // Use BorderLayout for better component arrangement

        messagePanel = new ImageBackgroundPanel("D:\\Java Programs\\SUMMER TRAINING PROGRAMS\\PROJECT MATERIAL\\Chatbox Final\\97c00759d90d786d9b6096d274ad3e07.jpg");
        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // Show scrollbar only when needed
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 16));

        sendButton = new JButton("Send");
        attachButton = new JButton("Attach");

        userComboBox = new JComboBox<>();
        userComboBox.addItem("Broadcast to All");
        userComboBox.setSelectedIndex(0);
        userComboBox.addActionListener(e -> isBroadcast = userComboBox.getSelectedIndex() == 0); // Update isBroadcast flag based on selected user

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(userComboBox);
        buttonPanel.add(attachButton);
        buttonPanel.add(sendButton);

        bottomPanel.add(inputField, BorderLayout.CENTER); // Add input field to the center of the bottom panel
        bottomPanel.add(buttonPanel, BorderLayout.EAST); // Add button panel to the east of the bottom panel
        add(bottomPanel, BorderLayout.SOUTH); // Add bottom panel to the south of the main frame

        sendButton.addActionListener(e -> sendTypedMessage());
        attachButton.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendButton.doClick()); // Send message when Enter is pressed in the input field

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    void sendTypedMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String selectedUser = (String) userComboBox.getSelectedItem(); // Get the selected user from the combo box
        if (!isBroadcast && selectedUser != null && !selectedUser.equals("Broadcast to All")) {
            sendPrivateMessage(selectedUser, text);
        } else {
            sendMessage(text);
        }
        addMessageBubble(text, true); // Add the message bubble to the chat panel
        inputField.setText(""); // Clear the input field after sending the message
    }

    void addMessageBubble(String message, boolean isSent) {
        JPanel bubbleWrapper = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT)); // Create a wrapper panel for the message bubble
        bubbleWrapper.setOpaque(false);

        JTextArea bubbleText = new JTextArea(message);
        bubbleText.setLineWrap(true); // Text wraps to the next line if it’s too long.
        bubbleText.setWrapStyleWord(true); // Wraps at word boundaries.
        bubbleText.setEditable(false); // Make the text area non-editable
        bubbleText.setFont(new Font("Arial", Font.PLAIN, 14));
        bubbleText.setBackground(isSent ? new Color(220, 248, 198, 230) : new Color(240, 240, 240, 200)); // Different background color for sent and received messages
        bubbleText.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10)); // Add padding around the text
        bubbleText.setOpaque(true); // Make the text area opaque to show the background color

        bubbleWrapper.add(bubbleText); // Add the text area to the wrapper panel
        messagePanel.add(bubbleWrapper); // Add the wrapper panel to the message panel
        messagePanel.revalidate(); // Revalidate the message panel to update the layout
        messagePanel.repaint();    // Repaint the message panel to reflect changes

        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum())); // Scroll to the bottom of the chat panel after adding a new message
    }

    void connectToServer() {
        try {
            socket = new Socket("localhost", 7500); // Connect to the server on localhost at port 7500
            dataOut = new DataOutputStream(socket.getOutputStream()); // Output stream to send data to the server(from client - server)
            dataIn = new DataInputStream(socket.getInputStream()); // Input stream to receive data from the server (from server - client)
            dataOut.writeUTF(name); // Send the client's name to the server
            dataOut.flush(); // Ensure the data is sent immediately
            addMessageBubble("Connected", false);
        } catch (IOException e) {
            addMessageBubble("Connection failed: " + e.getMessage(), false);
        }
    }

    void startReading() {
        new Thread(() -> {
            try {
                while (true) { // Continuously read messages from the server
                    String type = dataIn.readUTF(); // Read the type of message (text, file, user list, etc.)
                    if (type.equals("File")) {
                        String filename = dataIn.readUTF(); // Read the filename of the received file
                        int size = dataIn.readInt(); // Read the size of the file
                        byte[] fileBytes = new byte[size];
                        dataIn.readFully(fileBytes);
                        FileOutputStream fos = new FileOutputStream("received_" + filename);
                        fos.write(fileBytes); // Write the received file bytes to a new file
                        fos.close();
                        addMessageBubble("Received file: " + filename, false);
                        logger.info("Received file: " + filename);
                    } else if (type.startsWith("USER_LIST")) { // If the message is a user list
                        updateUserList(type); // Update the user list in the combo box
                    } else {
                        addMessageBubble(type, false); // Add the received message to the chat panel
                        logger.info("Message received: " + type); // Log the received message
                    }
                }
            } catch (IOException e) {
                addMessageBubble("Disconnected.", false);
                logger.warning("Disconnected: " + e.getMessage());
            }
        }).start(); // Start a new thread to read messages from the server
    }

    void updateUserList(String message) { // Update the user list in the combo box
        SwingUtilities.invokeLater(() -> {
            userComboBox.removeAllItems(); // Clear the existing items in the combo box
            userComboBox.addItem("Broadcast to All");
            String[] parts = message.split("/"); 
            for (int i = 1; i < parts.length; i++) { // Start from index 1 to skip the "USER_LIST" part
                if (!parts[i].equals(name)) {     // Exclude the current user's name from the list
                    userComboBox.addItem(parts[i]);
                }
            }
        });
    }

    void sendMessage(String msg) {
        try {
            dataOut.writeUTF(msg); // Send the message to the server
            dataOut.flush(); 
            logger.info("Sent message: " + msg);
        } catch (IOException e) {
            addMessageBubble("Error sending message.", false);
            logger.warning("Error sending message: " + e.getMessage());
        }
    }

    void sendPrivateMessage(String receiver, String msg) {
        try {
            dataOut.writeUTF("PRIVATE");
            dataOut.writeUTF(receiver);
            dataOut.writeUTF(msg);
            dataOut.flush();
            logger.info("Sent private message to " + receiver + ": " + msg);
        } catch (IOException e) {
            addMessageBubble("Error sending private message.", false);
            logger.warning("Error sending private message: " + e.getMessage());
        }
    }

    void sendFile() {
        JFileChooser fileChooser = new JFileChooser(); // Create a file chooser to select files to send
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()]; // Create a byte array to hold the file data
                fis.read(buffer); // Read the file into a byte array
                fis.close();

                String selectedUser = (String) userComboBox.getSelectedItem();
                if (!isBroadcast && selectedUser != null && !selectedUser.equals("Broadcast to All")) {
                    dataOut.writeUTF("PRIVATE_FILE");
                    dataOut.writeUTF(selectedUser);
                } else {
                    dataOut.writeUTF("File");
                }

                dataOut.writeUTF(file.getName());
                dataOut.writeInt(buffer.length);
                dataOut.write(buffer);
                dataOut.flush();

                addMessageBubble("File sent: " + file.getName(), true);
                logger.info("File sent: " + file.getName());
            } catch (IOException e) {
                addMessageBubble("Error sending file.", false);
                logger.warning("Error sending file: " + e.getMessage());
            }
        }
    }
}

class ImageBackgroundPanel extends JPanel {
    private final Image background;

    public ImageBackgroundPanel(String imagePath) { // Constructor to set the background image
        this.background = new ImageIcon(imagePath).getImage(); // Load the image from the specified path
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));// Use BoxLayout to stack components vertically
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call the superclass method to ensure proper painting
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this); // Draw the background image to fill the panel
    }
}
