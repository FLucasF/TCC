package br.com.marealta.persistence;

import br.com.marealta.messaging.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class PostgresMessageLog implements MessageLog {
    private final DataSource ds;

    public PostgresMessageLog(DataSource ds) { this.ds = ds; }

    @Override
    public void save(Message m, MessageStatus status) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO messages (channel, recipient, subject, body, status) VALUES (?,?,?,?,?)")) {
            ps.setString(1, m.channel().name());
            ps.setString(2, m.to());
            ps.setString(3, m.subject());
            ps.setString(4, m.body());
            ps.setString(5, status.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<StoredMessage> all() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<StoredMessage> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new StoredMessage(rs.getLong(1), Channel.valueOf(rs.getString(2)), rs.getString(3),
                        rs.getString(4), rs.getString(5), MessageStatus.valueOf(rs.getString(6))));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
