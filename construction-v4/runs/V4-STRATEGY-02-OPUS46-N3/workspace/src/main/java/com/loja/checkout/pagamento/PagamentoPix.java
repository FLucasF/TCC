package com.loja.checkout.pagamento;

import com.loja.checkout.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Moeda.arredondar(
                totalPedido.subtract(Moeda.arredondar(totalPedido.multiply(DESCONTO))));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
