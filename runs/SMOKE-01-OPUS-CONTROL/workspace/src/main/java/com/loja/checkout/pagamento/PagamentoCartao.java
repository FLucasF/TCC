package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.valor(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
            return new ResultadoPagamento(Dinheiro.valor(totalPedido), parcela);
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.valor(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-n) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
        return Dinheiro.valor(totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO));
    }
}
