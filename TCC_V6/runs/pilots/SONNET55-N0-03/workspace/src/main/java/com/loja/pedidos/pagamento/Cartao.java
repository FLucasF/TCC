package com.loja.pedidos.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Cartao implements FormaPagamento {

    private static final int MAX_PARCELAS = 12;
    private static final int MAX_SEM_JUROS = 3;
    private static final BigDecimal TAXA = new BigDecimal("0.0199");

    public String codigo() {
        return "CARTAO";
    }

    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new ErroNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new Cobranca(total, parcela);
        }
        // Tabela Price: total × i ÷ (1 − (1+i)^−n) = total × i × f ÷ (f − 1), com f = (1+i)^n
        BigDecimal f = BigDecimal.ONE.add(TAXA).pow(parcelas);
        BigDecimal parcela = Dinheiro.arredondar(
                total.multiply(TAXA).multiply(f).divide(f.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
        return new Cobranca(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
    }
}
