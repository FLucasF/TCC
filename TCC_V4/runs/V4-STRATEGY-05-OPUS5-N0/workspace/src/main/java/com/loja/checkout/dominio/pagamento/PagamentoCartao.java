package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Cartao de credito: ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes,
 * pela tabela Price.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal totalFinal = Dinheiro.valor(totalPedido);
            BigDecimal parcela = Dinheiro.valor(
                    totalFinal.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
            return new ResultadoPagamento(totalFinal, parcela);
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.valor(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-n) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, Dinheiro.CALCULO), Dinheiro.CALCULO);
        return Dinheiro.valor(totalPedido.multiply(TAXA_MENSAL, Dinheiro.CALCULO)
                .divide(divisor, Dinheiro.CALCULO));
    }
}
