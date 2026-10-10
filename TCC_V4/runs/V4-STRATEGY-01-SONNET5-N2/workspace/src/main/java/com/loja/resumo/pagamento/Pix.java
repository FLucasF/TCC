package com.loja.resumo.pagamento;

import com.loja.resumo.util.Arredondamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements FormaPagamento {

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
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.paraCentavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new AjustePagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
