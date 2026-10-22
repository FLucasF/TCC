package br.com.marealta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Numeração sem buracos (1, 2, 3...): o contador fica travado até o fim da transação. */
final class Ids {
    private Ids() {}

    static long next(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE counters SET value = value + 1 WHERE name = ? RETURNING value")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
