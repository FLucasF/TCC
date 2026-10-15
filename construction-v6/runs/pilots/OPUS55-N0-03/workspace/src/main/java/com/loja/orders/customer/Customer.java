package com.loja.orders.customer;

import java.math.BigDecimal;

public record Customer(String cpf, BigDecimal creditBalance) {
}
