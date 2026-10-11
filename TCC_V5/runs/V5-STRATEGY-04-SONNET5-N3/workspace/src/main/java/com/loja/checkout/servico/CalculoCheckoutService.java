package com.loja.checkout.servico;

import com.loja.checkout.CalculoCheckoutException;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoCheckoutRequest;
import com.loja.checkout.api.ResumoCheckoutResponse;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculoCheckoutService {

    public ResumoCheckoutResponse calcular(ResumoCheckoutRequest request) {
        List<ItemPedido> itens = converterItens(request);
        validarItens(itens);

        NivelClube nivelClube = parseEnum(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = pesoTotal(itens);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CalculoCheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(subtotalProdutos(itens));

        BigDecimal freteBase = Dinheiro.arredondar(modalidade.frete(itens, pesoTotal));
        BigDecimal frete = nivelClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        Cupom cupom = null;
        if (request.cupom() != null) {
            cupom = parseEnum(Cupom.class, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotalProdutos, itens)) {
                throw new CalculoCheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.desconto(subtotalProdutos, itens, frete));

        BigDecimal seguro = Dinheiro.arredondar(regiao.percentualSeguro().multiply(subtotalProdutos));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CalculoCheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CalculoCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                nivelClube.percentualCredito().multiply(subtotalProdutos));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoCheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemPedido> converterItens(ResumoCheckoutRequest request) {
        if (request.itens() == null) {
            return List.of();
        }
        return request.itens().stream()
                .map(this::converterItem)
                .toList();
    }

    private ItemPedido converterItem(ItemRequest item) {
        int quantidade = item.quantidade() == null ? 0 : item.quantidade();
        return new ItemPedido(item.nome(), item.precoUnitario(), quantidade, item.pesoKg());
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens.isEmpty() || itens.stream().anyMatch(item -> !item.valido())) {
            throw new CalculoCheckoutException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal subtotalProdutos(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::totalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal pesoTotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new CalculoCheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CalculoCheckoutException(codigoErro);
        }
    }
}
