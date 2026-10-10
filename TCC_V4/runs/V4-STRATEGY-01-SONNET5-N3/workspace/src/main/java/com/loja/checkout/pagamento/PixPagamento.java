package com.loja.checkout.pagamento;

import com.loja.checkout.service.Arredondamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("PIX")
public class PixPagamento implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.centavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(totalFinal, totalFinal, desconto.negate());
    }
}
