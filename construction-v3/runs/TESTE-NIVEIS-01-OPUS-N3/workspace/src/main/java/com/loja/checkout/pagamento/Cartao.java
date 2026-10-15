package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido);
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(totalFinal, parcela);
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }

    /** parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, mc));
        return totalPedido.multiply(TAXA_MENSAL, mc).divide(divisor, mc);
    }
}
