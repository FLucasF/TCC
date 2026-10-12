package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.loja.checkout.api.PedidoRequest.ItemRequest;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import com.loja.checkout.pedido.Item;
import com.loja.checkout.pedido.NivelClube;
import com.loja.checkout.pedido.Pedido;
import com.loja.checkout.pedido.Regiao;

@Component
public class MontadorPedido {

    private final List<ModalidadeEntrega> modalidades;
    private final List<Cupom> cupons;

    public MontadorPedido(List<ModalidadeEntrega> modalidades, List<Cupom> cupons) {
        this.modalidades = modalidades;
        this.cupons = cupons;
    }

    public Pedido montar(PedidoRequest request) {
        List<Item> itens = montarItens(request.itens());

        NivelClube nivel = enumDe(NivelClube.class, request.nivelClube())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = enumDe(Regiao.class, request.regiao())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = porCodigo(modalidades, ModalidadeEntrega::codigo, request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));

        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.atende(pesoTotal)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = null;
        if (request.cupom() != null) {
            cupom = porCodigo(cupons, Cupom::codigo, request.cupom())
                    .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        }

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();

        return new Pedido(itens, modalidade, cupom, request.formaPagamento(), parcelas, nivel, regiao);
    }

    private List<Item> montarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::montarItem).toList();
    }

    private Item montarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static <E extends Enum<E>> Optional<E> enumDe(Class<E> tipo, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants()).filter(e -> e.name().equals(valor)).findFirst();
    }

    private static <T> Optional<T> porCodigo(List<T> lista, Function<T, String> codigo, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return lista.stream().filter(e -> valor.equals(codigo.apply(e))).findFirst();
    }
}
