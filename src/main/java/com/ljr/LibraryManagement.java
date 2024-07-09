package com.ljr;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class LibraryManagement extends JFrame {
    private JTable table;
    private JTextField isbnField, titleField, authorsField, publisherField, publicationDateField, typeField;
    private int currentreaderID;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            try {
                LibraryManagement window = new LibraryManagement();
                window.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LibraryManagement() {
        setBounds(100, 100, 400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        
        showLoginPanel();
    }
    

    private void showLoginPanel() {
        JPanel loginPanel = new JPanel();
        getContentPane().add(loginPanel, BorderLayout.CENTER);
        loginPanel.setLayout(new GridLayout(3, 2));

        JLabel usernameLabel = new JLabel("firstname:");
        loginPanel.add(usernameLabel);

        JTextField usernameField = new JTextField();
        loginPanel.add(usernameField);
        usernameField.setColumns(10);

        JLabel passwordLabel = new JLabel("lastname:");
        loginPanel.add(passwordLabel);

        JPasswordField passwordField = new JPasswordField();
        loginPanel.add(passwordField);
        passwordField.setColumns(10);

        JButton loginButton = new JButton("登录");
        loginPanel.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                login(username, password);
            }
        });
    }

    private void login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空。");
            return;
        }



        String firstName = username;
        String lastName = password;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT readerID FROM  reader WHERE FirstName = ? AND LastName = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentreaderID = rs.getInt("readerID");
                showLibraryPanel();
                setBounds(100, 100, 800, 600);
            } else {
                JOptionPane.showMessageDialog(this, "无效的用户名或密码。");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showLibraryPanel() {
         getContentPane().removeAll();
         setBounds(100, 100, 800, 600);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         getContentPane().setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new GridLayout(2, 6));
         getContentPane().add(searchPanel, BorderLayout.NORTH);

        searchPanel.add(new JLabel("ISBN:"));
        isbnField = new JTextField();
        searchPanel.add(isbnField);

        searchPanel.add(new JLabel("Title:"));
        titleField = new JTextField();
        searchPanel.add(titleField);

        searchPanel.add(new JLabel("Authors:"));
        authorsField = new JTextField();
        searchPanel.add(authorsField);

        searchPanel.add(new JLabel("Publisher:"));
        publisherField = new JTextField();
        searchPanel.add(publisherField);

        searchPanel.add(new JLabel("Publication Date:"));
        publicationDateField = new JTextField();
        searchPanel.add(publicationDateField);

        searchPanel.add(new JLabel("Type:"));
        typeField = new JTextField();
        searchPanel.add(typeField);

        JButton searchButton = new JButton("查询");


        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchBooks();
            }
        });
         getContentPane().add(searchButton, BorderLayout.CENTER);

        table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
         getContentPane().add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(searchButton);
        JButton borrowButton = new JButton("借书");
        borrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                borrowBook();
            }
        });
        buttonPanel.add(borrowButton);

        JButton returnButton = new JButton("还书");
        returnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                returnBook();
            }
        });
        buttonPanel.add(returnButton);
    }

    private void searchBooks() {
        String query = "SELECT * FROM book WHERE 1=1";
        if (!isbnField.getText().trim().isEmpty()) query += " AND ISBN LIKE '%" + isbnField.getText().trim() + "%'";
        if (!titleField.getText().trim().isEmpty()) query += " AND Title LIKE '%" + titleField.getText().trim() + "%'";
        if (!authorsField.getText().trim().isEmpty()) query += " AND Authors LIKE '%" + authorsField.getText().trim() + "%'";
        if (!publisherField.getText().trim().isEmpty()) query += " AND Publisher LIKE '%" + publisherField.getText().trim() + "%'";
        if (!publicationDateField.getText().trim().isEmpty()) query += " AND PublicationDate LIKE '%" + publicationDateField.getText().trim() + "%'";
        if (!typeField.getText().trim().isEmpty()) query += " AND Type LIKE '%" + typeField.getText().trim() + "%'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            table.setModel(new ResultSetTableModel(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void borrowBook() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择一本书来借阅。");
            return;
        }
        String isbn = (String) table.getValueAt(selectedRow, 0);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkBorrowQuery = "SELECT * FROM record WHERE ISBN = ? AND ReturnDate IS NULL";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkBorrowQuery)) {
                checkStmt.setString(1, isbn);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "该书已被借出。");
                    return;
                }
            }

            String checkLimitQuery = "SELECT COUNT(*) FROM record WHERE readerID = ? AND ReturnDate IS NULL";
            try (PreparedStatement checkLimitStmt = conn.prepareStatement(checkLimitQuery)) {
                checkLimitStmt.setInt(1, currentreaderID);
                ResultSet rs = checkLimitStmt.executeQuery();
                if (rs.next()) {
                    int borrowedBooks = rs.getInt(1);
                    String limitQuery = "SELECT Limits FROM  reader WHERE readerID = ?";
                    try (PreparedStatement limitStmt = conn.prepareStatement(limitQuery)) {
                        limitStmt.setInt(1, currentreaderID);
                        ResultSet limitRs = limitStmt.executeQuery();
                        if (limitRs.next() && borrowedBooks >= limitRs.getInt("Limits")) {
                            JOptionPane.showMessageDialog(this, "您已超过最大借阅数目。");
                            return;
                        }
                    }
                }
            }

            String insertQuery = "INSERT INTO record (ISBN, readerID, BorrowingDate) VALUES (?, ?, CURDATE())";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, isbn);
                insertStmt.setInt(2, currentreaderID);
                insertStmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "借书成功。");
                searchBooks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void returnBook() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请选择一本书来归还。");
            return;
        }
        String isbn = (String) table.getValueAt(selectedRow, 0);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkBorrowQuery = "SELECT * FROM record WHERE ISBN = ? AND readerID = ? AND ReturnDate IS NULL";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkBorrowQuery)) {
                checkStmt.setString(1, isbn);
                checkStmt.setInt(2, currentreaderID);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "您没有借阅这本书。");
                    return;
                }
            }

            String updateQuery = "UPDATE record SET ReturnDate = CURDATE() WHERE ISBN = ? AND readerID = ? AND ReturnDate IS NULL";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setString(1, isbn);
                updateStmt.setInt(2, currentreaderID);
                updateStmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "还书成功。");
                searchBooks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
