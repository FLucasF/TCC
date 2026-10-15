package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoPagamento implements FormaPagamentoCalculo {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
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
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
