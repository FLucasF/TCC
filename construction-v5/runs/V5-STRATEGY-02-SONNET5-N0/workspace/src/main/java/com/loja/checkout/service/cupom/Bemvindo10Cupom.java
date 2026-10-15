package com.loja.checkout.service.cupom;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Cupom implements CupomStrategy {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
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
