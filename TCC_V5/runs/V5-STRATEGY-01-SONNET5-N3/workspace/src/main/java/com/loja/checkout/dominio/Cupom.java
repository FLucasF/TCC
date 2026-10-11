package com.loja.checkout.dominio;

import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

/**
 * Cada cupom carrega sua própria condição de aplicabilidade e sua própria
 * fórmula de desconto. A assinatura recebe o contexto mais exigente (itens e
 * frete), mesmo que a maioria dos cupons só use o subtotal.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public void validarAplicavel(ContextoCupom contexto) {
        }

        @Override
        public BigDecimal calcularDescontoBruto(ContextoCupom contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public void validarAplicavel(ContextoCupom contexto) {
            if (contexto.subtotalProdutos().compareTo(new BigDecimal("300.00")) < 0) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL");
            }
        }

        @Override
        public BigDecimal calcularDescontoBruto(ContextoCupom contexto) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public void validarAplicavel(ContextoCupom contexto) {
        }

        @Override
        public BigDecimal calcularDescontoBruto(ContextoCupom contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public void validarAplicavel(ContextoCupom contexto) {
        }

        @Override
        public BigDecimal calcularDescontoBruto(ContextoCupom contexto) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    public abstract void validarAplicavel(ContextoCupom contexto);

    public abstract BigDecimal calcularDescontoBruto(ContextoCupom contexto);

    public final BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.arredondar(calcularDescontoBruto(contexto));
    }
}
