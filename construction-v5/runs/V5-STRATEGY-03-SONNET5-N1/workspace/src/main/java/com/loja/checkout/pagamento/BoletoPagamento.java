package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BoletoPagamento implements Pagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL_PEDIDO = new BigDecimal("1000.00");

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.BOLETO;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL_PEDIDO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
