package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Os dados da compra como o site manda, ainda sem validar: codigos vem como texto
 * e os campos opcionais podem vir nulos.
 */
public record DadosCompra(
        List<DadosItem> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record DadosItem(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
