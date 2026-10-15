package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(
                totalPedido.subtract(Dinheiro.arredondar(totalPedido.multiply(DESCONTO))));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
