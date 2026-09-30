package com.loja.checkout.dominio.cupom;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% de desconto no valor dos produtos. */
@Component
public class CupomBemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotalProdutos(), new BigDecimal("10"));
    }
}
