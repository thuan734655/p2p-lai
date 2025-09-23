package org.example.client.views;

import org.example.client.services.LoginServices;

import javax.swing.*;

public class LoginView extends JFrame {
    public LoginView () {
        super("Login");
        setSize(300, 160);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        SpringLayout layout = new SpringLayout();
        setLayout(layout);

        JLabel usernameLabel = new JLabel("Username:");

        JTextField usernameTextField = new JTextField(15);

        JButton loginButton = new JButton("Login");

        add(usernameLabel);
        add(usernameTextField);
        add(loginButton);

        layout.putConstraint(SpringLayout.WEST, usernameLabel, 20, SpringLayout.WEST, this.getContentPane());
        layout.putConstraint(SpringLayout.NORTH, usernameLabel, 30, SpringLayout.NORTH, this.getContentPane());

        // Username TextField
        layout.putConstraint(SpringLayout.WEST, usernameTextField, 100, SpringLayout.WEST, this.getContentPane());
        layout.putConstraint(SpringLayout.NORTH, usernameTextField, 30, SpringLayout.NORTH, this.getContentPane());

        // Login Button
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, loginButton, 0, SpringLayout.HORIZONTAL_CENTER, this.getContentPane());
        layout.putConstraint(SpringLayout.NORTH, loginButton, 30, SpringLayout.SOUTH, usernameTextField);

        setLocationRelativeTo(null);
        setVisible(true);

        loginButton.addActionListener(e -> {
           String isLogin = LoginServices.Login(usernameTextField.getText());
           if (isLogin != "") {
               setVisible(false);
           }
        });
    }
    public static void main(String[] args) {
        new LoginView();
    }
}
