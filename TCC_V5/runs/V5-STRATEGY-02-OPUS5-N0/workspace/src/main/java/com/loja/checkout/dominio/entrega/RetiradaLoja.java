package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Retirada na loja: gratis, pronta em 1 dia. */
@Component
public class RetiradaLoja implements PoliticaEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BaseCalculo base) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
