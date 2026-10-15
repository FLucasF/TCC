package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados da compra que o site envia. Os codigos (entrega, cupom, pagamento,
 * clube, regiao) chegam como texto para que o servico resolva cada um na
 * ordem certa e devolva o codigo de erro correto quando o valor nao existe.
 */
public record ResumoCompraRequest(
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
