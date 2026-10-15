package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;

@Component
public class CartaoPagamento implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Cobranca(totalPedido, parcela);
        }
        BigDecimal parcela = parcelaComJuros(totalPedido, parcelas);
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }

    private BigDecimal parcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = total.multiply(TAXA_MENSAL).divide(denominador, MathContext.DECIMAL128);
        return Dinheiro.arredondar(parcela);
    }
}
