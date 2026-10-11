package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarLimite(BigDecimal totalPedido) {
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public ResultadoPagamento calcular(int parcelas, BigDecimal totalPedido) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(totalFinal, 1, totalFinal, TARIFA);
    }
}
