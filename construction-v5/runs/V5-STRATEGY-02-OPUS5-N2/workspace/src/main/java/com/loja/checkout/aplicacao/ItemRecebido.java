package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.PedidoRecusado;

import java.math.BigDecimal;

public record ItemRecebido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public Item paraItem() {
        if (!positivo(precoUnitario) || !positivo(quantidade) || !positivo(pesoKg)) {
            throw new PedidoRecusado(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(nome, precoUnitario, quantidade, pesoKg);
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static boolean positivo(Integer valor) {
        return valor != null && valor > 0;
    }
}
