package com.loja.resumo.calculo;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    public ResumoCompra calcular(Pedido pedido) {
        Carrinho carrinho = new Carrinho(pedido.itens());
        if (!carrinho.valido()) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }

        NivelClube nivel = Codigos.buscar(NivelClube.class, pedido.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Codigos.buscar(Regiao.class, pedido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException("REGIAO_INVALIDA"));
        ModalidadeEntrega entrega = Codigos.buscar(ModalidadeEntrega.class, pedido.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException("MODALIDADE_INVALIDA"));
        if (!entrega.atende(carrinho)) {
            throw new PedidoRecusadoException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (pedido.cupom() != null) {
            cupom = Codigos.buscar(Cupom.class, pedido.cupom())
                    .orElseThrow(() -> new PedidoRecusadoException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(carrinho)) {
                throw new PedidoRecusadoException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento forma = Codigos.buscar(FormaPagamento.class, pedido.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteCobrado(entrega.frete(carrinho));
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.atende(total)) {
            throw new PedidoRecusadoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = forma.cobrar(total, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }
}
