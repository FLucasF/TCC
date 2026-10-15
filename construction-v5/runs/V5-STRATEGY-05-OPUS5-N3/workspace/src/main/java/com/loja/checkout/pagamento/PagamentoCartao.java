package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
@Component
class PagamentoCartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL64;

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
        if (parcelas <= MAXIMO_PARCELAS_SEM_JUROS) {
            return new ResultadoPagamento(totalPedido,
                    Dinheiro.dividir(totalPedido, parcelas));
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new ResultadoPagamento(
                Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO);
    }
}
