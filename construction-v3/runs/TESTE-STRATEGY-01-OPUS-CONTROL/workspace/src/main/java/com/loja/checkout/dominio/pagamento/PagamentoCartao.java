package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

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
    public ResultadoPagamento calcular(ContextoPagamento contexto, int parcelas) {
        BigDecimal total = contexto.totalPedido();
        if (parcelas <= MAXIMO_SEM_JUROS) {
            return new ResultadoPagamento(total, parcelaSimples(total, parcelas));
        }
        BigDecimal parcela = parcelaComJuros(total, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new ResultadoPagamento(totalFinal, parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-n), arredondada para centavos. */
    private BigDecimal parcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal numerador = total.multiply(TAXA_MENSAL).multiply(fator);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        return Dinheiro.centavos(Dinheiro.dividir(numerador, denominador));
    }
}
