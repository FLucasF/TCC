package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 50,00 de desconto nos produtos, a partir de R$ 300,00 em produtos. */
@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(DadosCupom dados) {
        return dados.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(DadosCupom dados) {
        return DESCONTO;
    }
}
