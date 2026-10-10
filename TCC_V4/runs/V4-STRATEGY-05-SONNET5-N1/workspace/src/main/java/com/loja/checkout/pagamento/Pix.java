package com.loja.checkout.pagamento;

import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.centavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
    }
}
