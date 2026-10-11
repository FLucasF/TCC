package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto, so para compras a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal VALOR = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public void validarAplicabilidade(DadosCupom dados) {
        if (dados.subtotalProdutos().compareTo(MINIMO) < 0) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
    }

    @Override
    public BigDecimal desconto(DadosCupom dados) {
        return Dinheiro.centavos(VALOR);
    }
}
