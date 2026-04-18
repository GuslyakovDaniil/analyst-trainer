package com.example.dao;

import com.example.config.DatabaseConfig;
import com.example.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserDao {
    public void createUser(String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, password); ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getString("role"), rs.getInt("total_score"), rs.getString("avatar_icon"));
        }
        return null;
    }

    public List<User> findAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role, total_score, avatar_icon FROM users WHERE role = 'USER' ORDER BY total_score DESC";
        try (Connection conn = DatabaseConfig.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(new User(rs.getInt("id"), rs.getString("username"), null, rs.getString("role"), rs.getInt("total_score"), rs.getString("avatar_icon")));
        }
        return users;
    }

    public void updateUsername(int userId, String newName) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE users SET username = ? WHERE id = ?")) {
            ps.setString(1, newName); ps.setInt(2, userId); ps.executeUpdate();
        }
    }

    public void updateAvatar(int userId, String icon) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE users SET avatar_icon = ? WHERE id = ?")) {
            ps.setString(1, icon); ps.setInt(2, userId); ps.executeUpdate();
        }
    }

    // --- РЕЙТИНГ И АЧИВКИ ---
    public List<Map<String, Object>> getLeaderboard() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT username, total_score, avatar_icon FROM users WHERE role = 'USER' AND is_public = TRUE AND total_score > 0 ORDER BY total_score DESC LIMIT 10";
        try (Connection conn = DatabaseConfig.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int rank = 1;
            while (rs.next()) list.add(Map.of("rank", rank++, "username", rs.getString("username"), "score", rs.getInt("total_score"), "avatar", rs.getString("avatar_icon")));
        }
        return list;
    }

    public void togglePrivacy(int userId, boolean isPublic) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_public = ? WHERE id = ?")) {
            ps.setBoolean(1, isPublic); ps.setInt(2, userId); ps.executeUpdate();
        }
    }

    public boolean isUserPublic(int userId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT is_public FROM users WHERE id = ?")) {
            ps.setInt(1, userId); ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean(1);
        }
    }

    public void awardAchievement(int userId, String achievementName) {
        String sql = "INSERT INTO user_achievements (user_id, achievement_id) SELECT ?, id FROM achievements_dict WHERE name = ? ON CONFLICT DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, achievementName); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public List<Map<String, Object>> getAllAchievementsWithStatus(int userId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT d.name, d.icon, d.description, CASE WHEN u.id IS NOT NULL THEN true ELSE false END as earned " +
                "FROM achievements_dict d LEFT JOIN user_achievements u ON d.id = u.achievement_id AND u.user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(Map.of("name", rs.getString("name"), "icon", rs.getString("icon"), "description", rs.getString("description"), "earned", rs.getBoolean("earned")));
        }
        return list;
    }
}