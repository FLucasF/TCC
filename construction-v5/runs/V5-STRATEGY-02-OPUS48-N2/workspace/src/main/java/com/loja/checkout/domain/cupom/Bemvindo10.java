package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public void validarAplicabilidade(DadosCupom dados) {
        // Sem condicao.
    }

    @Override
    public BigDecimal desconto(DadosCupom dados) {
        return Dinheiro.percentual(dados.subtotalProdutos(), PERCENTUAL);
    }
}
