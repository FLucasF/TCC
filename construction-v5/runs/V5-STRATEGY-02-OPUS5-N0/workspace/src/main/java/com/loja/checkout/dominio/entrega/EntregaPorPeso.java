package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Entrega cobrada como taxa fixa + valor por kg do pedido. */
public abstract class EntregaPorPeso implements PoliticaEntrega {

    private final BigDecimal taxaFixa;
    private final BigDecimal porKg;

    protected EntregaPorPeso(String taxaFixa, String porKg) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(BaseCalculo base) {
        return Dinheiro.arredondar(taxaFixa.add(porKg.multiply(base.pesoTotalKg())));
    }
}
