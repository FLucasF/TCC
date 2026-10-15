package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PixPagamento implements Pagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.PIX;
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
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
