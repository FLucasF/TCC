package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Pagamento(totalPedido, parcelas, Dinheiro.centavos(totalPedido.divide(n, MathContext.DECIMAL128)));
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        return new Pagamento(Dinheiro.centavos(parcela.multiply(n)), parcelas, parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
    private static BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(-parcelas, MathContext.DECIMAL128);
        return total.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(fator), MathContext.DECIMAL128);
    }
}
