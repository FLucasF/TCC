package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cartao de credito: de 1 a 12 parcelas. Ate 3x sem juros; de 4x a 12x com
 * juros de 1,99% ao mes calculados pela tabela Price.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(Dinheiro.arredondar(totalPedido), parcelas, parcela);
        }

        BigDecimal parcela = Dinheiro.arredondar(parcelaPrice(totalPedido, parcelas));
        BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcelas, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return total.multiply(TAXA_MENSAL).divide(divisor, PRECISAO);
    }
}
