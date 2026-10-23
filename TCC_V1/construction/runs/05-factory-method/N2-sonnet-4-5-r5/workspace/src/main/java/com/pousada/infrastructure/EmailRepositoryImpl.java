package com.pousada.infrastructure;

import com.pousada.domain.model.Email;
import com.pousada.domain.repository.EmailRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class EmailRepositoryImpl implements EmailRepository {
    private final DataSource dataSource;

    public EmailRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Email save(Email email) {
        String sql = "INSERT INTO emails (channel, recipient, subject, body) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, email.channel());
            stmt.setString(2, email.to());
            stmt.setString(3, email.subject());
            stmt.setString(4, email.body());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                email.setId(keys.getLong(1));
            }

            return email;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save email", e);
        }
    }

    @Override
    public List<Email> findAll() {
        List<Email> emails = new ArrayList<>();
        String sql = "SELECT * FROM emails ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                emails.add(new Email(
                    rs.getLong("id"),
                    rs.getString("channel"),
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find emails", e);
        }

        return emails;
    }
}
