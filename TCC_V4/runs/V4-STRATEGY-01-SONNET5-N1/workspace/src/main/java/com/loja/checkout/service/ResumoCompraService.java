package com.loja.checkout.service;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.ContextoDesconto;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.OpcaoEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.pedido.ItemPedido;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.exception.PedidoRecusadoException;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest requisicao) {
        List<ItemPedido> itens = converterItens(requisicao.itens());
        validarItens(itens);

        NivelClube nivelClube = parseEnum(NivelClube.class, requisicao.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, requisicao.regiao(), "REGIAO_INVALIDA");
        OpcaoEntrega opcaoEntrega = parseEnum(OpcaoEntrega.class, requisicao.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarValorItens(itens));
        BigDecimal pesoTotal = somarPesoItens(itens);

        if (!opcaoEntrega.disponivel(pesoTotal)) {
            throw new PedidoRecusadoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivelClube.isentaFrete()
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : Dinheiro.arredondar(opcaoEntrega.calcularFrete(pesoTotal));

        BigDecimal descontoCupom = calcularDescontoCupom(requisicao.cupom(), subtotalProdutos, frete, itens);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, requisicao.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();

        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcularResultado(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                opcaoEntrega.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        Cupom cupom = parseEnum(Cupom.class, codigoCupom, "CUPOM_INVALIDO");
        ContextoDesconto contexto = new ContextoDesconto(subtotalProdutos, frete, itens);

        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException("CUPOM_NAO_APLICAVEL");
        }

        return Dinheiro.arredondar(cupom.calcularDesconto(contexto));
    }

    private List<ItemPedido> converterItens(List<ItemRequest> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
                .map(item -> new ItemPedido(
                        item.nome(),
                        item.precoUnitario(),
                        item.quantidade() == null ? 0 : item.quantidade(),
                        item.pesoKg()))
                .toList();
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal somarValorItens(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPesoItens(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new PedidoRecusadoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusadoException(codigoErro);
        }
    }
}
