package com.loja.checkout.cupom;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Handler implements CupomHandler {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(DadosPedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(DadosPedido pedido) {
        return Dinheiro.arredondar(pedido.subtotalProdutos().multiply(PERCENTUAL));
    }
}
