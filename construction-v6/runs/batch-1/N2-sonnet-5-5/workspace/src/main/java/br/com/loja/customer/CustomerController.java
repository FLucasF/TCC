package br.com.loja.customer;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;

@RestController
class CustomerController {

    record CustomerResponse(String cpf, BigDecimal creditBalance) {
    }

    private final CustomerRepository customers;

    CustomerController(CustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping("/customers/{cpf}")
    CustomerResponse get(@PathVariable String cpf) {
        BigDecimal balance = customers.findCreditBalance(cpf)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));
        return new CustomerResponse(cpf, balance);
    }
}
