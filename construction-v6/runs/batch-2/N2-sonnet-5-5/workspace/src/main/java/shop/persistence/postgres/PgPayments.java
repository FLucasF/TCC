package shop.persistence.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import shop.persistence.PaymentRepository;

class PgPayments implements PaymentRepository {
    private final Connection c;

    PgPayments(Connection c) {
        this.c = c;
    }

    @Override
    public Optional<PaymentRecord> find(String orderId, String transactionId) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT error_code FROM payments WHERE order_id = ? AND transaction_id = ?")) {
                ps.setString(1, orderId);
                ps.setString(2, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new PaymentRecord(rs.getString(1))) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void record(String orderId, String transactionId, String errorCode) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO payments(order_id, transaction_id, error_code) VALUES (?, ?, ?)")) {
                ps.setString(1, orderId);
                ps.setString(2, transactionId);
                ps.setString(3, errorCode);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
