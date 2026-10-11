package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("PIX")
public class Pix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public void validarDisponibilidade(Resumo resumo) {
    }

    @Override
    public void validarParcelas(Integer parcelas) {
        if (parcelas != null && parcelas != 1) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal desconto = totalPedido
            .multiply(PERCENTUAL_DESCONTO)
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, Integer parcelas) {
        return totalPedido.add(calcularAjuste(totalPedido, parcelas));
    }
}
