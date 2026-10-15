package com.loja.checkout.cupom;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CupomBemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto contexto) {
        return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(PERCENTUAL));
    }
}
