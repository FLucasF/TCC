package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 10% de desconto no valor dos produtos. */
@Component
public class CupomBemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(DadosCupom dados) {
        return Moeda.percentual(dados.subtotalProdutos(), PERCENTUAL);
    }
}
