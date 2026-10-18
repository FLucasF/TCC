package shop.persistence.postgres;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import shop.domain.Customer;
import shop.persistence.CustomerRepository;

class PgCustomers implements CustomerRepository {
    private final Connection c;

    PgCustomers(Connection c) {
        this.c = c;
    }

    @Override
    public Optional<Customer> find(String cpf) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT credit_balance FROM customers WHERE cpf = ?")) {
                ps.setString(1, cpf);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new Customer(cpf, rs.getBigDecimal(1))) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void ensure(String cpf) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO customers(cpf) VALUES (?) ON CONFLICT (cpf) DO NOTHING")) {
                ps.setString(1, cpf);
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void addCredit(String cpf, BigDecimal amount) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?")) {
                ps.setBigDecimal(1, amount);
                ps.setString(2, cpf);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
