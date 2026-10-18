package shop.customer;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerRepository {
    /** Creates the customer with zero credit if it does not exist yet. */
    void createIfAbsent(String cpf);

    Optional<Customer> find(String cpf);

    void addCredit(String cpf, BigDecimal amount);
}
