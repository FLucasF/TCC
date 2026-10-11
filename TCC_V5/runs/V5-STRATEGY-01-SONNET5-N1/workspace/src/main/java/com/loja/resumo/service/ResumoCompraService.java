package com.loja.resumo.service;

import com.loja.resumo.domain.BeneficioClube;
import com.loja.resumo.domain.ContextoCupom;
import com.loja.resumo.domain.Cupom;
import com.loja.resumo.domain.Dinheiro;
import com.loja.resumo.domain.FormaPagamento;
import com.loja.resumo.domain.ItemPedido;
import com.loja.resumo.domain.ModalidadeEntrega;
import com.loja.resumo.domain.NivelClube;
import com.loja.resumo.domain.Regiao;
import com.loja.resumo.domain.ResultadoPagamento;
import com.loja.resumo.web.ItemRequest;
import com.loja.resumo.web.ResumoRequest;
import com.loja.resumo.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<ItemPedido> itens = converterItens(pedido.itens());
        validarItens(itens);
        BigDecimal subtotalProdutos = somarProdutos(itens);
        BigDecimal pesoTotalKg = somarPeso(itens);

        NivelClube nivelClube = parseEnum(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.disponivelPara(pesoTotalKg)) {
            throw new ErroPedidoException("MODALIDADE_INDISPONIVEL");
        }

        BeneficioClube beneficioClube = nivelClube.calcularBeneficio(subtotalProdutos);
        BigDecimal frete = beneficioClube.freteGratis()
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : modalidade.calcularFrete(pesoTotalKg);

        BigDecimal descontoCupom = calcularDescontoCupom(pedido.cupom(), subtotalProdutos, itens, frete);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new ErroPedidoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                Dinheiro.arredondar(subtotalProdutos),
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultadoPagamento.ajustePagamento(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                beneficioClube.credito(),
                beneficioClube.brinde()
        );
    }

    private List<ItemPedido> converterItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null) {
            return List.of();
        }
        return itensRequest.stream()
                .map(item -> new ItemPedido(
                        item.nome(),
                        item.precoUnitario() == null ? null : BigDecimal.valueOf(item.precoUnitario()),
                        item.quantidade() == null ? 0 : item.quantidade(),
                        item.pesoKg() == null ? null : BigDecimal.valueOf(item.pesoKg())))
                .toList();
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new ErroPedidoException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal somarProdutos(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::totalProdutos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::totalPesoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, BigDecimal subtotalProdutos,
                                               List<ItemPedido> itens, BigDecimal frete) {
        if (codigoCupom == null) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
        Cupom cupom = parseEnum(Cupom.class, codigoCupom, "CUPOM_INVALIDO");
        ContextoCupom contexto = new ContextoCupom(subtotalProdutos, itens, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new ErroPedidoException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.calcularDesconto(contexto);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new ErroPedidoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException(codigoErro);
        }
    }
}
