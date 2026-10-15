package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ValoresPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public Parcelamento calcular(ValoresPedido valores, int parcelas) {
        BigDecimal total = valores.total();
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new Parcelamento(total, dividir(total, parcelas));
        }
        BigDecimal parcela = parcelaPrice(total, parcelas);
        return new Parcelamento(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }

    private BigDecimal dividir(BigDecimal total, int parcelas) {
        return Dinheiro.centavos(total.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−parcelas). */
    private BigDecimal parcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal parcela = total.multiply(TAXA_MENSAL)
                .multiply(fator, Dinheiro.PRECISAO)
                .divide(fator.subtract(BigDecimal.ONE), Dinheiro.PRECISAO);
        return Dinheiro.centavos(parcela);
    }
}
