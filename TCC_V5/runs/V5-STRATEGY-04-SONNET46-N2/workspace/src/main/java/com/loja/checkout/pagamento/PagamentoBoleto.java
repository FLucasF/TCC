package com.loja.checkout.pagamento;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.ErroCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public void verificarParcelamento(int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException(ErroCode.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void verificarDisponibilidade(BigDecimal totalPedido, int parcelas) {
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw new CheckoutException(ErroCode.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
