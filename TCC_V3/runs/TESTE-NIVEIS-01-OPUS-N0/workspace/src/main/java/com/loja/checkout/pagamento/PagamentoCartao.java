package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new ResultadoPagamento(Dinheiro.arredondar(totalPedido), parcela);
        }
        BigDecimal parcela = parcelaPrice(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        return Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
    }
}
