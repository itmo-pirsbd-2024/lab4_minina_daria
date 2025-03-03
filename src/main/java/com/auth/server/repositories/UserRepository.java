package com.auth.server.repositories;

import com.auth.server.models.User;
import com.auth.server.utils.Config;
import com.auth.server.utils.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final int MAX_ATTEMPTS = Config.getInt("login.max.attempts");
    private static final int LOCK_TIME = Config.getInt("login.lock.time.minutes");

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setLocked(rs.getBoolean("locked"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            logger.error("Database error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void save(User user) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error saving user: {}", e.getMessage());
        }
    }

    public void recordLoginAttempt(String username, boolean success) {
        String sql = "INSERT INTO login_attempts (username, success, attempt_time) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setBoolean(2, success);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            if (!success) checkAndLockUser(username);

        } catch (SQLException e) {
            logger.error("Error recording login attempt: {}", e.getMessage());
        }
    }

    private void checkAndLockUser(String username) {
        String sql = "SELECT COUNT(*) FROM login_attempts " +
                "WHERE username = ? AND success = false " +
                "AND attempt_time > NOW() - INTERVAL '? minutes'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setInt(2, LOCK_TIME);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getInt(1) >= MAX_ATTEMPTS) {
                lockUser(username);
            }
        } catch (SQLException e) {
            logger.error("Error checking login attempts: {}", e.getMessage());
        }
    }

    private void lockUser(String username) {
        String sql = "UPDATE users SET locked = true WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.executeUpdate();
            logger.warn("User {} locked due to multiple failed attempts", username);

        } catch (SQLException e) {
            logger.error("Error locking user: {}", e.getMessage());
        }
    }
}