package com.loja.pedidos.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal LIMITE = Dinheiro.valor("1000.00");
    private static final BigDecimal TARIFA = Dinheiro.valor("3.49");

    public String codigo() {
        return "BOLETO";
    }

    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new ErroNegocio(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw new ErroNegocio(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    public Cobranca cobrar(BigDecimal total, int parcelas, boolean comCupom) {
        BigDecimal valor = total.add(TARIFA);
        return new Cobranca(valor, valor);
    }
}
