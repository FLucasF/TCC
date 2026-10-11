package com.loja.checkout.service.cupom;

import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL)
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
