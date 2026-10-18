package shop.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import shop.domain.Customer;

public interface CustomerRepository {
    Optional<Customer> find(String cpf);

    void ensure(String cpf);

    void addCredit(String cpf, BigDecimal amount);
}
