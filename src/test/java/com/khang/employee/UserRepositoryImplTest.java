package com.khang.employee;

import com.khang.employee.entity.User;
import com.khang.employee.repository.UserRepository;
import com.khang.employee.repository.UserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryImplTest {
    private Connection connection;
    private UserRepository userRepository;
    private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS users");
        }

        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE users (user_id BIGINT PRIMARY KEY, user_name VARCHAR(50) NOT NULL)";
            stmt.executeUpdate(sql);
        }

        userRepository = new UserRepositoryImpl(DB_URL, DB_USER, DB_PASSWORD);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testSave() throws SQLException {
        User user = new User(1L, "John Doe", true);

        User savedUser = userRepository.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isEqualTo(1L);
        assertThat(savedUser.getName()).isEqualTo("John Doe");

        try (Statement stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM users WHERE user_id = 1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("user_id")).isEqualTo(1L);
            assertThat(rs.getString("user_name")).isEqualTo("John Doe");
        }
    }

    @Test
    void testSaveUpdate() throws SQLException {
        User user = new User(1L, "John Doe", true);
        userRepository.save(user);

        User updatedUser = new User(1L, "Jane Doe", false);
        userRepository.save(updatedUser);

        try (Statement stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM users WHERE user_id = 1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("user_name")).isEqualTo("Jane Doe");
        }
    }

    @Test
    void testFindById() {
        User user = new User(1L, "John Doe", true);
        userRepository.save(user);

        Optional<User> foundUser = userRepository.findById(1L);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(1L);
        assertThat(foundUser.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void testFindByIdNotFound() {
        Optional<User> foundUser = userRepository.findById(999L);

        assertThat(foundUser).isEmpty();
    }

    @Test
    void testFindAll() {
        userRepository.save(new User(1L, "John Doe", true));
        userRepository.save(new User(2L, "Jane Doe", false));

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getName).containsExactlyInAnyOrder("John Doe", "Jane Doe");
    }

    @Test
    void testDeleteById() {
        userRepository.save(new User(1L, "John Doe", true));

        userRepository.deleteById(1L);

        Optional<User> foundUser = userRepository.findById(1L);
        assertThat(foundUser).isEmpty();
    }

    @Test
    void testCount() {
        userRepository.save(new User(1L, "John Doe", true));
        userRepository.save(new User(2L, "Jane Doe", false));

        long count = userRepository.count();

        assertThat(count).isEqualTo(2);
    }
}