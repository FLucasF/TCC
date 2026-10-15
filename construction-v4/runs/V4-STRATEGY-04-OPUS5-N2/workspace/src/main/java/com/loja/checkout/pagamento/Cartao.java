package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Em ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELA_MINIMA = 1;
    private static final int PARCELA_MAXIMA = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELA_MINIMA && parcelas <= PARCELA_MAXIMA;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new ResultadoPagamento(totalPedido, Dinheiro.divide(totalPedido, parcelas));
        }
        BigDecimal parcela = parcelaComJuros(totalPedido, parcelas);
        return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        return Dinheiro.centavos(totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO));
    }
}
