package com.loja.checkout.service;

import com.loja.checkout.api.CheckoutException;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.dominio.AjustePagamento;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<Item> itens = converterItens(request.itens());
        validarItens(itens);

        BigDecimal subtotalProdutos = Dinheiro.arredondar(itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivelClube = parseEnum(request.nivelClube(), NivelClube.class, "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(request.regiao(), Regiao.class, "REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = parseEnum(request.modalidadeEntrega(), ModalidadeEntrega.class,
                "MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivelClube.freteGratis()
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : modalidade.custo(pesoTotal);

        Cupom cupom = parseCupom(request.cupom());
        if (cupom != null && !cupom.aplicavel(itens, subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.arredondar(BigDecimal.ZERO)
                : Dinheiro.arredondar(cupom.desconto(itens, subtotalProdutos, frete));

        FormaPagamento formaPagamento = parseEnum(request.formaPagamento(), FormaPagamento.class,
                "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal totalPedidoSemImposto = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.disponivel(totalPedidoSemImposto)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal imposto = regiao.imposto(subtotalProdutos.subtract(descontoCupom));
        BigDecimal totalPedido = Dinheiro.arredondar(totalPedidoSemImposto.add(imposto));

        AjustePagamento ajustePagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivelClube.credito(subtotalProdutos));
        boolean brinde = nivelClube.brinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                imposto,
                ajustePagamento.ajuste(),
                ajustePagamento.totalFinal(),
                parcelas,
                ajustePagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> converterItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null) {
            return List.of();
        }
        return itensRequest.stream()
                .map(item -> new Item(
                        item.nome(),
                        item.precoUnitario(),
                        item.quantidade() == null ? 0 : item.quantidade(),
                        item.pesoKg()))
                .toList();
    }

    private void validarItens(List<Item> itens) {
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private Cupom parseCupom(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigoCupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private <E extends Enum<E>> E parseEnum(String valor, Class<E> tipo, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
