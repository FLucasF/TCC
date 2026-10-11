package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cartão de crédito em 1 a 12 vezes. Até 3x é sem juros; de 4x a 12x há juros
 * de 1,99% ao mês pela tabela Price.
 */
@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** Valor final é o próprio total; a parcela é o total dividido pelas vezes. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, Dinheiro.MODO);
        return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
    }

    /** Tabela Price: parcela = total × i ÷ (1 − (1 + i)^−n); final = parcela × n. */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.centavos(
                totalPedido.multiply(TAXA_MENSAL).divide(denominador, MathContext.DECIMAL128));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }
}
