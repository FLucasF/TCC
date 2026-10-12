package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Cartao implements FormaPagamento {

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

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
        return new Cobranca(Dinheiro.centavos(totalPedido), parcelas, valorParcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-n). */
    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        BigDecimal valorParcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO));
        BigDecimal valorFinal = Dinheiro.centavos(
                valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(valorFinal, parcelas, valorParcela);
    }
}
