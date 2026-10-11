package com.loja.resumo.cupom;

import com.loja.resumo.Contexto;
import com.loja.resumo.ErroResumo;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal COMPRA_MINIMA = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public void verificarAplicavel(Contexto contexto) {
        if (contexto.subtotal().compareTo(COMPRA_MINIMA) < 0) {
            throw new ErroResumo("CUPOM_NAO_APLICAVEL");
        }
    }

    @Override
    public BigDecimal desconto(Contexto contexto) {
        return new BigDecimal("50.00");
    }
}
