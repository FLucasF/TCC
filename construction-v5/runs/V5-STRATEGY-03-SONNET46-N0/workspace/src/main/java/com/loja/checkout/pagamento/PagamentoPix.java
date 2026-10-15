package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarLimite(BigDecimal totalPedido) {
    }

    @Override
    public ResultadoPagamento calcular(int parcelas, BigDecimal totalPedido) {
        BigDecimal ajuste = totalPedido.multiply(DESCONTO).setScale(2, RoundingMode.HALF_EVEN).negate();
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new ResultadoPagamento(totalFinal, 1, totalFinal, ajuste);
    }
}
