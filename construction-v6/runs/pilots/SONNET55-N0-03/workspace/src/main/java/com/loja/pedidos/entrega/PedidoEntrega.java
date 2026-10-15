package com.loja.pedidos.entrega;

import java.math.BigDecimal;

import com.loja.pedidos.dominio.Regiao;

public record PedidoEntrega(String cep, Regiao regiao, BigDecimal pesoKg) {
}
