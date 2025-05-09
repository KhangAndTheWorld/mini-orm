package com.khang.employee;

import com.khang.employee.entity.User;
import com.khang.employee.repository.UserRepository;
import com.khang.employee.repository.UserRepositoryImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        String dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String dbUser = "sa";
        String dbPassword = "";

        try {
            createTable(dbUrl, dbUser, dbPassword);

            UserRepository userRepo = new UserRepositoryImpl(dbUrl, dbUser, dbPassword);

            User user = new User(1L, "John Doe", true);
            userRepo.save(user);
            System.out.println("Saved user: " + user.getName());

            Optional<User> foundUser = userRepo.findById(1L);
            foundUser.ifPresent(u -> System.out.println("Found user: " + u.getName()));

            List<User> users = userRepo.findAll();
            System.out.println("All users: " + users.size());

            long count = userRepo.count();
            System.out.println("Number of users: " + count);

            userRepo.deleteById(1L);
            System.out.println("Deleted user: " + user.getName());

            count = userRepo.count();
            System.out.println("Number of users after delete: " + count);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTable(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE users (user_id BIGINT PRIMARY KEY, user_name VARCHAR(255) NOT NULL)";
            stmt.executeUpdate(sql);
        }
    }
}
