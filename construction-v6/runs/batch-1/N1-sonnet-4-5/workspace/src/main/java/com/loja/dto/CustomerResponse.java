package com.loja.dto;

import com.loja.domain.Customer;
import java.math.BigDecimal;

public record CustomerResponse(
    String cpf,
    BigDecimal creditBalance
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getCpf(),
            customer.getCreditBalance().value()
        );
    }
}
