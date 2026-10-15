package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixPagamento implements FormaPagamentoCalculo {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
