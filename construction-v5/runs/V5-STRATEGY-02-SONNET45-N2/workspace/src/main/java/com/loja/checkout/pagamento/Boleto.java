package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("BOLETO")
public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("1000.00");

    @Override
    public void validarDisponibilidade(Resumo resumo) {
        if (resumo.totalPedido().compareTo(LIMITE_VALOR) > 0) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    @Override
    public void validarParcelas(Integer parcelas) {
        if (parcelas != null && parcelas != 1) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, Integer parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, Integer parcelas) {
        return totalPedido.add(TARIFA);
    }
}
