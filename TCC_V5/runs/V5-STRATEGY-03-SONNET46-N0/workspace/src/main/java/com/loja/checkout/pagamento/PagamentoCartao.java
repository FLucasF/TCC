package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAX = 12;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > PARCELAS_MAX) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarLimite(BigDecimal totalPedido) {
    }

    @Override
    public ResultadoPagamento calcular(int parcelas, BigDecimal totalPedido) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, parcelas, valorParcela, BigDecimal.ZERO.setScale(2));
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN));
        BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, parcelas, valorParcela, ajuste);
    }
}
