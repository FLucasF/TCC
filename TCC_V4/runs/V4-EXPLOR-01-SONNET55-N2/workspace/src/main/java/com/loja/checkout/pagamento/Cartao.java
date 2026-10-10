package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {
    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(30, RoundingMode.HALF_EVEN);

    public String codigo() { return "CARTAO"; }

    public boolean aceitaParcelas(int parcelas) { return parcelas >= 1 && parcelas <= MAX_PARCELAS; }

    public boolean disponivel(BigDecimal totalPedido) { return true; }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
            return new Cobranca(totalPedido, parcela);
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA).divide(divisor, PRECISAO));
        return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
    }
}
