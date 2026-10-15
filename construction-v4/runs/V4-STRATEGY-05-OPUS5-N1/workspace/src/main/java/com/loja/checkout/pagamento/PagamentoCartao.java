package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return semJuros(totalPedido, parcelas);
        }
        return comJuros(totalPedido, parcelas);
    }

    private Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Dinheiro.emCentavos(
                totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
        return new Cobranca(totalPedido, parcelas, valorParcela);
    }

    private Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = TabelaPrice.parcela(totalPedido, TAXA_MENSAL, parcelas);
        BigDecimal totalFinal = Dinheiro.emCentavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, parcelas, valorParcela);
    }
}
