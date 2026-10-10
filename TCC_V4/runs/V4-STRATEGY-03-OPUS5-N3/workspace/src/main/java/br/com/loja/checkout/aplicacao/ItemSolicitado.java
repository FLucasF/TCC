package br.com.loja.checkout.aplicacao;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Item;

/** Um item do carrinho como o site envia: qualquer campo pode faltar. */
public record ItemSolicitado(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    boolean valido() {
        return positivo(precoUnitario) && quantidade != null && quantidade > 0 && positivo(pesoKg);
    }

    Item paraItem() {
        return new Item(nome, precoUnitario, quantidade, pesoKg);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
