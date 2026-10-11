package com.loja.resumo.pagamento;

import com.loja.resumo.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public int maxParcelas() {
        return MAX_PARCELAS;
    }

    @Override
    public Pagamento aplicar(BigDecimal total, int parcelas) {
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            BigDecimal parcelaSemJuros = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Pagamento(total, parcelas, parcelaSemJuros);
        }
        BigDecimal parcela = Dinheiro.arredondar(total.multiply(fatorPrice(parcelas)));
        return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }

    private BigDecimal fatorPrice(int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal inverso = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), MathContext.DECIMAL128);
        return TAXA_MENSAL.divide(BigDecimal.ONE.subtract(inverso), MathContext.DECIMAL128);
    }
}
