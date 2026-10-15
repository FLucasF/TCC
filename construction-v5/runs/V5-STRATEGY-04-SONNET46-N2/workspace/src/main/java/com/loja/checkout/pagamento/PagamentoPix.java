package com.loja.checkout.pagamento;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.ErroCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements FormaPagamento {

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public void verificarParcelamento(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException(ErroCode.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void verificarDisponibilidade(BigDecimal totalPedido, int parcelas) {
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
