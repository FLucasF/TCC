package com.loja.checkout.cupom;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Handler implements CupomHandler {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(DadosPedido pedido) {
        return pedido.subtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(DadosPedido pedido) {
        return Dinheiro.arredondar(DESCONTO);
    }
}
