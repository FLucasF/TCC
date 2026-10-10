package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
public final class Cartao implements FormaPagamento {

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
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** O valor final e o proprio total do pedido; a parcela e o total dividido. */
    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.arredonda(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CONTAS));
        return new Cobranca(totalPedido, valorParcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas); final = parcela x parcelas. */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CONTAS);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CONTAS));
        BigDecimal valorParcela = Dinheiro.arredonda(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CONTAS));
        BigDecimal totalFinal = Dinheiro.arredonda(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, valorParcela);
    }
}
