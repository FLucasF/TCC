package com.loja.resumo.cupom;

import com.loja.resumo.Contexto;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomBemVindo implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Contexto contexto) {
        return contexto.subtotal().multiply(new BigDecimal("0.10"));
    }
}
