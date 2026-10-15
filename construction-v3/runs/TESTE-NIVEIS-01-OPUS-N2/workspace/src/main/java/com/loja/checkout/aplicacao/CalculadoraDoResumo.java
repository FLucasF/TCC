package com.loja.checkout.aplicacao;

import com.loja.checkout.api.PedidoRecusadoException;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.aplicacao.regras.RegrasDoPedido;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.pagamento.Pago;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra que o site mostra antes de confirmar o pedido. */
@Service
public class CalculadoraDoResumo {

    private final MontadorDeRascunho montador;
    private final RegrasDoPedido regras;

    public CalculadoraDoResumo(MontadorDeRascunho montador, RegrasDoPedido regras) {
        this.montador = montador;
        this.regras = regras;
    }

    public ResumoResponse calcular(ResumoRequest pedidoBruto) {
        Rascunho rascunho = montador.montar(pedidoBruto);
        regras.primeiroProblema(rascunho).ifPresent(codigo -> {
            throw new PedidoRecusadoException(codigo);
        });
        return montarResumo(rascunho);
    }

    private ResumoResponse montarResumo(Rascunho rascunho) {
        BigDecimal subtotalProdutos = rascunho.pedido().subtotalProdutos();
        BigDecimal totalPedido = rascunho.totalPedido();
        Pago pago = rascunho.formaPagamento().orElseThrow().calcular(totalPedido, rascunho.parcelas());
        NivelClube nivel = rascunho.nivelClube().orElseThrow();

        return new ResumoResponse(
                subtotalProdutos,
                rascunho.descontoCupom(),
                rascunho.frete(),
                rascunho.entrega().prazoDias(),
                rascunho.seguro(),
                Dinheiro.centavos(pago.totalFinal().subtract(totalPedido)),
                pago.totalFinal(),
                rascunho.parcelas(),
                pago.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }
}
