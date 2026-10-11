package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que o site envia. Os campos de código (modalidade, cupom, pagamento, nível,
 * região) chegam como String para o serviço validá-los e devolver o código de
 * erro certo quando faltam ou não existem, em vez de um erro genérico de parsing.
 */
public record ResumoRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
