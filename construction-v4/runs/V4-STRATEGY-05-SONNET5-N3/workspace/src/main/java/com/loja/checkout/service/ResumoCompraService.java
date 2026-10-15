package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.PedidoRecusadoException;
import com.loja.checkout.model.ContextoCupom;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.Dinheiro;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.model.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());
        NivelClube nivelClube = parseNivelClube(pedido.nivelClube());
        Regiao regiao = parseRegiao(pedido.regiao());
        ModalidadeEntrega modalidade = parseModalidade(pedido.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotalKg = calcularPesoTotal(pedido.itens());

        if (!modalidade.disponivel(pesoTotalKg)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotalKg);
        if (nivelClube.freteGratis()) {
            frete = Dinheiro.arredondar(BigDecimal.ZERO);
        }

        Cupom cupom = parseCupom(pedido.cupom());
        BigDecimal descontoCupom = Dinheiro.arredondar(BigDecimal.ZERO);
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(subtotalProdutos, frete, pedido.itens());
            if (!cupom.aplicavel(contextoCupom)) {
                throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(contextoCupom);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private NivelClube parseNivelClube(String valor) {
        return parseEnum(valor, NivelClube.class, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    private Regiao parseRegiao(String valor) {
        return parseEnum(valor, Regiao.class, CodigoErro.REGIAO_INVALIDA);
    }

    private ModalidadeEntrega parseModalidade(String valor) {
        return parseEnum(valor, ModalidadeEntrega.class, CodigoErro.MODALIDADE_INVALIDA);
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        return parseEnum(valor, FormaPagamento.class, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    private Cupom parseCupom(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_INVALIDO);
        }
    }

    private <E extends Enum<E>> E parseEnum(String valor, Class<E> tipo, CodigoErro codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new PedidoRecusadoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusadoException(codigoErro);
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }
}
