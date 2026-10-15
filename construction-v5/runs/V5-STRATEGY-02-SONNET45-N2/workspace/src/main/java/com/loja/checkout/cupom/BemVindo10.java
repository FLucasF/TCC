package com.loja.checkout.cupom;

import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("BEMVINDO10")
public class BemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, Resumo resumo) {
        return pedido.subtotalProdutos()
            .multiply(PERCENTUAL)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void validarAplicabilidade(Pedido pedido) {
    }
}
