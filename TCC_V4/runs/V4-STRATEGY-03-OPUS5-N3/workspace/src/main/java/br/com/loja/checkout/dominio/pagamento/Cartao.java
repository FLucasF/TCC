package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** Ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes pela tabela Price. */
@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final MathContext PRECISAO = MathContext.DECIMAL64;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean atende(Dinheiro totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(Dinheiro totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new Cobranca(totalPedido, totalPedido.divididoPor(parcelas));
        }
        Dinheiro parcela = parcelaPrice(totalPedido, parcelas);
        return new Cobranca(parcela.vezes(parcelas), parcela);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    private Dinheiro parcelaPrice(Dinheiro totalPedido, int parcelas) {
        BigDecimal umMaisTaxaElevado = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(umMaisTaxaElevado, PRECISAO));
        return Dinheiro.de(totalPedido.valor().multiply(TAXA_MENSAL).divide(divisor, PRECISAO));
    }
}
