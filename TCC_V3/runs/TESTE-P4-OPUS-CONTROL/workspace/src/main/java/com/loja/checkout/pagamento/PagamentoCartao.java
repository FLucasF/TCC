package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes pela tabela Price. */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public ResultadoPagamento calcular(ContextoPagamento contexto) {
        BigDecimal total = contexto.totalPedido();
        int parcelas = contexto.parcelas();
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(
                    total.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
            return new ResultadoPagamento(Dinheiro.centavos(total), parcela);
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(total, parcelas));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
        return total.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO);
    }
}
