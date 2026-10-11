package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/** Até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês pela tabela Price. */
@Component
public class Cartao implements FormaPagamento {

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
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    /** O valor final é o próprio total do pedido; a parcela é o total dividido. */
    private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal parcela = Dinheiro.arredonda(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new ResultadoPagamento(Dinheiro.arredonda(totalPedido), parcela);
    }

    /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
    private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal parcela = Dinheiro.arredonda(totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
        return new ResultadoPagamento(Dinheiro.arredonda(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
