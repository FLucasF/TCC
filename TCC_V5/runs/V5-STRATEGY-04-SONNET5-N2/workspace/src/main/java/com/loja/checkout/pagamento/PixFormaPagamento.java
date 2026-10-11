package com.loja.checkout.pagamento;

import com.loja.checkout.dinheiro.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixFormaPagamento implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean indisponivel(BigDecimal totalPedido) {
        return false;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
