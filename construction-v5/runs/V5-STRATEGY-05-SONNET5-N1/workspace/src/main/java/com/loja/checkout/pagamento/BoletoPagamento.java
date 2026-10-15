package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoPagamento implements FormaPagamentoCalculadora {

    private static final BigDecimal TARIFA_FIXA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public FormaPagamento getTipo() {
        return FormaPagamento.BOLETO;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = TARIFA_FIXA;
        BigDecimal totalFinal = Arredondamento.monetario(totalPedido.add(ajuste));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
