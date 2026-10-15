package com.loja.domain.pagamento;

import com.loja.util.Dinheiro;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("-0.05")));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.add(calcularAjuste(totalPedido, parcelas));
        return Dinheiro.arredondar(valorFinal);
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }
}
