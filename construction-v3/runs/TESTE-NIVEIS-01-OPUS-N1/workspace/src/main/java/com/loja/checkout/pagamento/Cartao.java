package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** De 1 a 12 vezes: ate 3x sem juros, de 4x a 12x com 1,99% ao mes (tabela Price). */
@Component
class Cartao implements FormaPagamento {

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
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CONTAS));
            return new Pagamento(Dinheiro.centavos(totalPedido), parcelas, parcela);
        }
        BigDecimal parcela = parcelaComJuros(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Pagamento(totalFinal, parcelas, parcela);
    }

    /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
    private BigDecimal parcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CONTAS);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CONTAS));
        return Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CONTAS));
    }
}
