package com.loja.checkout.pagamento;

import com.loja.checkout.Arredondamento;
import com.loja.checkout.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {
    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() { return "PIX"; }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {}

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.arredondar(totalPedido.multiply(TAXA_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
