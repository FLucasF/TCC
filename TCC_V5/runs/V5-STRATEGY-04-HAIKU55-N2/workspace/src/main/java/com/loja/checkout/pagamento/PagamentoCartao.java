package com.loja.checkout.pagamento;

import static com.loja.checkout.calculo.Dinheiro.centavos;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public int parcelasMaximas() {
        return 12;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Pagamento(totalPedido, valorParcela, parcelas);
        }
        BigDecimal parcela = centavos(parcelaTabelaPrice(totalPedido, parcelas));
        BigDecimal valorFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        return new Pagamento(valorFinal, parcela, parcelas);
    }

    private static BigDecimal parcelaTabelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fatorCrescimento = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal numerador = total.multiply(TAXA_MENSAL).multiply(fatorCrescimento);
        BigDecimal denominador = fatorCrescimento.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, MathContext.DECIMAL128);
    }
}
