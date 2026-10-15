package com.loja.checkout.pagamento;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.ErroCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public void verificarParcelamento(int parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new CheckoutException(ErroCode.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void verificarDisponibilidade(BigDecimal totalPedido, int parcelas) {
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido.setScale(2, RoundingMode.HALF_EVEN), valorParcela);
        }

        BigDecimal taxa = TAXA_MENSAL;
        int n = parcelas;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaNeg = BigDecimal.ONE.divide(umMaisTaxa.pow(n, new MathContext(20)), 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(umMaisTaxaNeg);
        BigDecimal valorParcela = totalPedido.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
