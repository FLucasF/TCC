package com.loja.checkout.pagamento;

import com.loja.checkout.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Moeda.arredondar(
                    totalPedido.divide(new BigDecimal(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128));
        BigDecimal valorParcela = Moeda.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(denominador, MathContext.DECIMAL128));
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
