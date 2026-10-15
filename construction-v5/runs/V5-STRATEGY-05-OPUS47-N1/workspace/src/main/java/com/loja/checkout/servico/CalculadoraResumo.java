package com.loja.checkout.servico;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.entrega.Frete;
import com.loja.checkout.dominio.pagamento.Ajuste;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CalculadoraResumo {

    public ResumoResponse calcular(Pedido pedido) {
        BigDecimal subtotal = Dinheiro.arredonda(pedido.subtotalProdutos());
        Frete freteBase = pedido.modalidade().calcular(pedido.pesoTotal());
        BigDecimal frete = Dinheiro.arredonda(pedido.nivel().fretePago(freteBase.valor()));
        BigDecimal desconto = Dinheiro.arredonda(pedido.cupom().desconto(pedido.itens(), subtotal, frete));
        BigDecimal seguro = Dinheiro.arredonda(subtotal.multiply(pedido.regiao().taxaSeguro()));
        BigDecimal totalPedido = Dinheiro.arredonda(subtotal.subtract(desconto).add(frete).add(seguro));

        if (!pedido.forma().atende(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Ajuste ajuste = pedido.forma().calcular(totalPedido, pedido.parcelas());
        BigDecimal credito = pedido.nivel().credito(subtotal);
        boolean brinde = pedido.nivel().brinde(subtotal);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                freteBase.prazoDias(),
                seguro,
                Dinheiro.arredonda(ajuste.valor()),
                Dinheiro.arredonda(ajuste.totalFinal()),
                pedido.parcelas(),
                Dinheiro.arredonda(ajuste.valorParcela()),
                credito,
                brinde
        );
    }
}
