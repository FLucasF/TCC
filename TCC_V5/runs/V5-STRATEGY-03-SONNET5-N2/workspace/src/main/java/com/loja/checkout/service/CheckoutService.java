package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.PedidoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest request) {
        List<Item> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = itens.stream()
                .map(Item::totalPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pesoTotal = itens.stream()
                .map(Item::totalPeso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivelClube = NivelClube.from(request.nivelClube());
        Regiao regiao = Regiao.from(request.regiao());

        ModalidadeEntrega modalidade = ModalidadeEntrega.from(request.modalidadeEntrega());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = request.cupom() == null ? null : Cupom.from(request.cupom());
        if (cupom != null && !cupom.aplicavel(subtotalProdutos, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPagamento = FormaPagamento.from(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal freteExibido = nivelClube.pagaFrete()
                ? Dinheiro.arredondar(modalidade.calcularFrete(pesoTotal))
                : Dinheiro.arredondar(BigDecimal.ZERO);

        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : Dinheiro.arredondar(cupom.calcularDesconto(subtotalProdutos, itens, freteExibido));

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(freteExibido).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                Dinheiro.arredondar(subtotalProdutos),
                descontoCupom,
                freteExibido,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredondar(resultadoPagamento.ajuste()),
                Dinheiro.arredondar(resultadoPagamento.totalFinal()),
                parcelas,
                Dinheiro.arredondar(resultadoPagamento.valorParcela()),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>();
        for (ItemRequest itemRequest : itensRequest) {
            Item item = new Item(itemRequest.nome(), itemRequest.precoUnitario(),
                    itemRequest.quantidade() == null ? 0 : itemRequest.quantidade(), itemRequest.pesoKg());
            if (!item.valido()) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            itens.add(item);
        }
        return itens;
    }
}
