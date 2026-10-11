package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

@Component
public class PagamentoCartao implements FormaPagamento {
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;
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
    public Resultado calcular(BigDecimal totalPedido, int n) {
        BigDecimal qtd = BigDecimal.valueOf(n);
        if (n <= MAX_SEM_JUROS) {
            return new Resultado(totalPedido, Dinheiro.arredondar(totalPedido.divide(qtd, MathContext.DECIMAL128)));
        }
        // Tabela Price: total * i / (1 - (1 + i)^-n)
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(n);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
        BigDecimal parcela = Dinheiro.arredondar(
                totalPedido.multiply(TAXA_MENSAL).divide(denominador, MathContext.DECIMAL128));
        return new Resultado(Dinheiro.arredondar(parcela.multiply(qtd)), parcela);
    }
}
