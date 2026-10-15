package com.loja.pedidos.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    public String codigo() {
        return "PIX";
    }

    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new ErroNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        BigDecimal valor = comCupom ? total : total.subtract(Dinheiro.fracao(total, DESCONTO));
        return new Cobranca(valor, valor);
    }
}
