package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/** Parte comum das opções que cobram uma taxa fixa mais um valor por kg do pedido. */
abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal taxaFixa;
    private final BigDecimal porKg;

    protected FretePorPeso(String taxaFixa, String porKg) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return taxaFixa.add(porKg.multiply(pesoKg));
    }
}
