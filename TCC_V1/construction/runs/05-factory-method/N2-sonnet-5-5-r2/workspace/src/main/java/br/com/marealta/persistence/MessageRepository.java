package br.com.marealta.persistence;

import br.com.marealta.domain.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MessageRepository {

    public void insert(Connection c, Message m) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "insert into messages (channel, recipient, subject, body) values (?, ?, ?, ?)")) {
            st.setString(1, m.channel());
            st.setString(2, m.to());
            st.setString(3, m.subject());
            st.setString(4, m.body());
            st.executeUpdate();
        }
    }

    public List<Message> all(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("select * from messages order by id");
             ResultSet rs = st.executeQuery()) {
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                        rs.getString("subject"), rs.getString("body")));
            }
            return result;
        }
    }
}
