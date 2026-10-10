package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Percentual;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagamentoPix extends AVista {

    private static final Percentual DESCONTO = Percentual.de("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    protected BigDecimal totalFinal(BigDecimal totalPedido) {
        return totalPedido.subtract(DESCONTO.sobre(totalPedido));
    }
}
