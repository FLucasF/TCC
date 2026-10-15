package com.loja.pedidos.dominio;

import java.math.BigDecimal;

public record Cliente(String cpf, BigDecimal saldoCredito) {
}
