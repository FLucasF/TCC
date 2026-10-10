package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Pix implements FormaPagamento {

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        return totalPedido.subtract(desconto);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }
}
