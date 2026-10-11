package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

/**
 * Base reutilizável para modalidades que cobram um valor fixo mais um valor
 * por kg do pedido, e que atendem qualquer peso. As modalidades que seguem
 * essa forma só precisam informar os números e o prazo.
 */
public abstract class EntregaLinear implements ModalidadeEntrega {

    private final BigDecimal fixo;
    private final BigDecimal porKg;
    private final int prazoDias;

    protected EntregaLinear(String fixo, String porKg, int prazoDias) {
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
        this.prazoDias = prazoDias;
    }

    @Override
    public int prazoDias() {
        return prazoDias;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pesoKg)));
    }
}
