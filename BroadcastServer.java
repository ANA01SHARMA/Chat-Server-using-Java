import javax.swing.*; // For JFrame, JPanel, JTextArea, JList, JScrollPane, DefaultListModel, SwingUtilities
import java.awt.*; // For BorderLayout, Dimension, Font
import java.io.*; // For DataInputStream, DataOutputStream, IOException
import java.net.*; // For ServerSocket and Socket
import java.util.*; // For Map and ConcurrentHashMap
import java.util.concurrent.*; // For ConcurrentHashMap

public class BroadcastServer extends JFrame {
    ServerSocket serverSocket;//    
    static final int PORT = 7500;
    final Map<String, ClientHandler> clients = new ConcurrentHashMap<>(); // Thread-safe map to store client handlers
    // This map helps the server track who is connected and manage their communication safely.
    DefaultListModel<String> clientListModel; // Model for the JList of connected clients
    JTextArea logArea; // Text area to display server logs

    public BroadcastServer() {
        setupGUI();
        new Thread(()->startServer()).start(); // Start the server in a separate thread to avoid blocking the GUI
    }

    private void setupGUI() {
        setTitle("Broadcast Server");
        setSize(600, 500);
        setLayout(new BorderLayout());
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        clientListModel = new DefaultListModel<>(); // Model to hold the list of connected clients
        JList<String> clientList = new JList<>(clientListModel); // JList to display connected clients
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        clientScroll.setPreferredSize(new Dimension(200, 0));
        add(clientScroll, BorderLayout.WEST);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Server Log"));
        add(logScroll, BorderLayout.CENTER);

        setVisible(true);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            appendLog("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                DataInputStream tempIn = new DataInputStream(clientSocket.getInputStream());
                String clientName = tempIn.readUTF(); // Read the client's name from the input stream
                // It writes a String to the output stream in modified UTF-8 format.
                appendLog("Client connected: " + clientName);
                ClientHandler handler = new ClientHandler(clientSocket, clientName); // Create a new ClientHandler for the connected client
                clients.put(clientName, handler); // Add the client handler to the map

                SwingUtilities.invokeLater(() -> { // Update the GUI on the Event Dispatch Thread
                    clientListModel.addElement(clientName); // Add the client's name to the list model
                    broadcastClientList(); // Broadcast the updated client list to all connected clients
                });

                new Thread(handler).start();
            }
        } catch (IOException e) {
            appendLog("Error starting server: " + e.getMessage());
        }
    }

    private void broadcastClientList() { // Working of broadcastClientList method
        StringBuilder sb = new StringBuilder("USER_LIST");//USER_LIST/Mukesh/Arun/Anamika
        for (String name : clients.keySet()) {
            sb.append("/").append(name);
        }

        for (ClientHandler c : clients.values()) {
            try {
                c.sendText(sb.toString()); // Send the updated user list to each client
            } catch (IOException e) {
                appendLog("Failed to send user list to " + c.clientName);
            }
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n"); // Append the message to the log area
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Scroll to the bottom of the log area
        });
    }

    class ClientHandler implements Runnable { // Handles communication with a connected client
        private final Socket socket;
        private final String clientName;
        private DataInputStream in;
        private DataOutputStream out;

        public ClientHandler(Socket socket, String clientName) {
            this.socket = socket;
            this.clientName = clientName;
            try {
                in = new DataInputStream(socket.getInputStream()); // Initialize input stream to read data from the client
                out = new DataOutputStream(socket.getOutputStream()); // Initialize output stream to send data to the client
            } catch (IOException e) {
                appendLog("Error setting up streams for " + clientName);
            }
        }

        public void run() {
            try {
                while (true) {
                    String type = in.readUTF();

                    if (type.equals("File")) { // If the message type is "File", it indicates a file transfer
                        String fileName = in.readUTF();
                        int size = in.readInt();
                        byte[] fileData = new byte[size];
                        in.readFully(fileData);

                        appendLog(clientName + " sent file: " + fileName); // Log the file transfer
                        broadcastFile(fileName, fileData, this);
                    } else if (type.equals("PRIVATE_FILE")) { // If the message type is "PRIVATE_FILE", it indicates a private file transfer
                        String receiver = in.readUTF();
                        String fileName = in.readUTF();
                        int size = in.readInt();
                        byte[] fileData = new byte[size];
                        in.readFully(fileData);

                        ClientHandler target = clients.get(receiver);
                        if (target != null) {
                            appendLog("[Private File] " + clientName + " -> " + receiver + ": " + fileName);
                            target.sendFile(fileName, fileData);
                        } else {
                            sendText("User '" + receiver + "' not found.");
                        }
                    } else if (type.equals("PRIVATE")) {
                        String receiver = in.readUTF();
                        String message = in.readUTF();
                        ClientHandler target = clients.get(receiver);
                        if (target != null) {
                            target.sendText("[Private] " + clientName + ": " + message);
                        }
                    } else {
                        appendLog(clientName + ": " + type);
                        broadcastMessage(clientName + ": " + type, this);
                    }
                }
            } catch (IOException e) {
                appendLog("Client disconnected: " + clientName);
            } finally {
                clients.remove(clientName); // Remove the client from the map when they disconnect
                SwingUtilities.invokeLater(() -> {
                    clientListModel.removeElement(clientName);
                    broadcastClientList();
                });
                try {
                    socket.close();
                } catch (IOException e) {}
            }
        }

        public void sendText(String msg) throws IOException {
            out.writeUTF(msg);
            out.flush();
        }

        public void sendFile(String filename, byte[] data) throws IOException {
            out.writeUTF("File");
            out.writeUTF(filename); // Send the file name
            out.writeInt(data.length); // Send the file size
            out.write(data);
            out.flush();
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) { // Don't send the message back to the sender
                try {
                    client.sendText(message);
                } catch (IOException e) {
                    appendLog("Failed to send message to " + client.clientName);
                }
            }
        }
    }

    private void broadcastFile(String fileName, byte[] data, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) {
                try {
                    client.sendFile(fileName, data);
                } catch (IOException e) {
                    appendLog("Failed to send file to " + client.clientName);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->new BroadcastServer()); // Launch the server GUI on the Event Dispatch Thread
    }
}
