package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;

/** Base para transportadoras que cobram uma taxa fixa mais um valor por kg. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal taxaFixa;
    private final BigDecimal porKg;

    protected EntregaPorPeso(String taxaFixa, String porKg) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return Moeda.centavos(taxaFixa.add(porKg.multiply(pesoKg)));
    }
}
