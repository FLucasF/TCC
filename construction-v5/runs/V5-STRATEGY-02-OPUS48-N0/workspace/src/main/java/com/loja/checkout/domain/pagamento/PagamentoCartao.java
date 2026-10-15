package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/**
 * Cartao de credito: de 1 a 12 vezes. Ate 3x sem juros; de 4x a 12x com juros
 * de 1,99% ao mes pela tabela Price.
 */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            // Sem juros: o valor final e o proprio total; a parcela e o total
            // dividido pelo numero de parcelas, arredondado para centavos.
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(parcelas, parcela, totalPedido, Dinheiro.ZERO);
        }

        // Com juros (tabela Price):
        // parcela = total x taxa / (1 - (1 + taxa)^-n)
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal fator = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO));
        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(parcelas, parcela, totalFinal, ajuste);
    }
}
