package com.loja.checkout.servico;

import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoRecusadoException;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculoResumoService {

    public ResumoResponse calcular(ResumoRequest requisicao) {
        List<Item> itens = validarItens(requisicao.itens());
        NivelClube nivelClube = validarNivelClube(requisicao.nivelClube());
        Regiao regiao = validarRegiao(requisicao.regiao());
        ModalidadeEntrega modalidade = validarModalidade(requisicao.modalidadeEntrega());

        BigDecimal pesoTotal = itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Cupom cupom = validarCupom(requisicao.cupom());

        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(modalidade.custo(pesoTotal));

        if (cupom != null && !cupom.aplicavel(itens, subtotalProdutos, frete)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(cupom.desconto(itens, subtotalProdutos, frete));

        FormaPagamento formaPagamento = validarFormaPagamento(requisicao.formaPagamento());
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.ajuste();

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                nivelClube.credito(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos)
        );
    }

    private List<Item> validarItens(List<ItemRequest> itensRequisicao) {
        if (itensRequisicao == null || itensRequisicao.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = itensRequisicao.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade() == null ? 0 : i.quantidade(), i.pesoKg()))
                .toList();
        if (itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private NivelClube validarNivelClube(String nivelClube) {
        return converter(nivelClube, NivelClube::valueOf, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    private Regiao validarRegiao(String regiao) {
        return converter(regiao, Regiao::valueOf, CodigoErro.REGIAO_INVALIDA);
    }

    private ModalidadeEntrega validarModalidade(String modalidade) {
        return converter(modalidade, ModalidadeEntrega::valueOf, CodigoErro.MODALIDADE_INVALIDA);
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        return converter(formaPagamento, FormaPagamento::valueOf, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    private Cupom validarCupom(String cupom) {
        if (cupom == null) {
            return null;
        }
        return converter(cupom, Cupom::valueOf, CodigoErro.CUPOM_INVALIDO);
    }

    private <T extends Enum<T>> T converter(String valor, java.util.function.Function<String, T> parser, CodigoErro codigoErro) {
        if (valor == null) {
            throw new PedidoRecusadoException(codigoErro);
        }
        try {
            return parser.apply(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusadoException(codigoErro);
        }
    }
}
