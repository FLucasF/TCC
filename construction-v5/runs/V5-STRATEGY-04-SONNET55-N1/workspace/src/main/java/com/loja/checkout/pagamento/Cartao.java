package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements Pagamento {
    private static final int MAX_PARCELAS = 12;
    private static final int MAX_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal n = BigDecimal.valueOf(parcelas);
        if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
            return new Cobranca(totalPedido, totalPedido.divide(n, 2, RoundingMode.HALF_EVEN));
        }
        // Tabela Price: total × i ÷ (1 − (1 + i)^−n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MathContext.DECIMAL128);
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).multiply(fator)
                        .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
