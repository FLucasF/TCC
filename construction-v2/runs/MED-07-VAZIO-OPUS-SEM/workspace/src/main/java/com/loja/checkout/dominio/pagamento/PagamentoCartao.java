package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Cartao de credito: de 1 a 12 parcelas. Ate 3x sem juros; de 4x a 12x com
 * juros de 1,99% ao mes pela tabela Price.
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
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
            return new ResultadoPagamento(Dinheiro.arredondar(totalPedido), parcelas, valorParcela);
        }
        BigDecimal valorParcela = Dinheiro.arredondar(parcelaPrice(totalPedido, parcelas));
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fatorNegativo = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, Dinheiro.PRECISAO), Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(fatorNegativo);
        return total.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO);
    }
}
