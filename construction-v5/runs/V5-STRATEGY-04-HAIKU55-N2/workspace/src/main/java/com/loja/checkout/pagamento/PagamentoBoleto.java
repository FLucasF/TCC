package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public int parcelasMaximas() {
        return 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.add(TARIFA);
        return new Pagamento(valorFinal, valorFinal, 1);
    }
}
