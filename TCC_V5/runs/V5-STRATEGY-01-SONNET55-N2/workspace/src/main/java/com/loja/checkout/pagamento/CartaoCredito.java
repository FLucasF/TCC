package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
class CartaoCredito implements FormaPagamento {

    private static final int MAXIMO_PARCELAS = 12;
    private static final int MAXIMO_SEM_JUROS = 3;
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
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new Cobranca(totalPedido, Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN)));
        }
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
