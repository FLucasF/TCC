package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

/** Cobrança de um valor fixo mais um valor por quilo do pedido. */
record FretePorPeso(BigDecimal fixo, BigDecimal porKg) {

    FretePorPeso(String fixo, String porKg) {
        this(Dinheiro.de(fixo), Dinheiro.de(porKg));
    }

    BigDecimal calcular(BigDecimal pesoKg) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pesoKg)));
    }
}
