package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: sempre a vista, com 5% de desconto no total do pedido. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Moeda.percentual(totalPedido, PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Moeda.emCentavos(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
