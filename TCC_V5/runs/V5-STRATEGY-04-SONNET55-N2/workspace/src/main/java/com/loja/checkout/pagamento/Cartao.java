package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
class Cartao implements FormaPagamento {
    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(30);

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
        if (parcelas <= MAX_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(totalPedido.divide(n, MC)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA).pow(parcelas, MC);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MC), MC);
        BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA).divide(divisor, MC));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(n)), parcela);
    }
}
