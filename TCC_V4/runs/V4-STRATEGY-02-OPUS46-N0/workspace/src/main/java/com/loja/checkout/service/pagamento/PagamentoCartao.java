package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class PagamentoCartao implements ProcessadorPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = Moeda.arredondar(totalPedido.divide(new BigDecimal(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal umMaisTaxaN = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal umMaisTaxaNegN = BigDecimal.ONE.divide(umMaisTaxaN, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(umMaisTaxaNegN);
        BigDecimal valorParcela = Moeda.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(denominador, MathContext.DECIMAL128));
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));

        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
