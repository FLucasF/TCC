package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
@Component
public class CupomBemvindo10 implements PoliticaCupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(BaseCalculo base, BigDecimal frete) {
        return Dinheiro.percentual(base.subtotalProdutos(), new BigDecimal("0.10"));
    }
}
