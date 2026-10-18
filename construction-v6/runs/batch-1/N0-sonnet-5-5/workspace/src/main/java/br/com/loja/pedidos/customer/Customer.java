package br.com.loja.pedidos.customer;

import java.math.BigDecimal;

public record Customer(String cpf, BigDecimal creditBalance) {
}
