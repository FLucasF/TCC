package com.loja.checkout.service;

import com.loja.checkout.api.ErroPedidoException;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.domain.AjustePagamento;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.CupomContexto;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = validarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());

        BigDecimal pesoTotalKg = itens.stream()
                .map(Item::totalPeso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.modalidadeEntrega());
        if (!modalidade.disponivel(pesoTotalKg)) {
            throw new ErroPedidoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(itens.stream()
                .map(Item::totalPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal freteBruto = modalidade.calcularFrete(pesoTotalKg);

        Cupom cupom = validarCupom(request.cupom());
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            CupomContexto cupomContexto = new CupomContexto(subtotalProdutos, itens, freteBruto);
            if (!cupom.aplicavel(cupomContexto)) {
                throw new ErroPedidoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(cupomContexto);
        }

        FormaPagamentoValidada formaPagamentoValidada = validarFormaPagamento(request.formaPagamento(), request.parcelas());

        BigDecimal frete = nivelClube.isFreteGratis() ? BigDecimal.ZERO.setScale(2) : freteBruto;
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        if (!formaPagamentoValidada.formaPagamento().disponivel(totalPedido)) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        AjustePagamento ajustePagamento = formaPagamentoValidada.formaPagamento()
                .calcular(totalPedido, formaPagamentoValidada.parcelas());

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento.ajuste(),
                ajustePagamento.totalFinal(),
                formaPagamentoValidada.parcelas(),
                ajustePagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new ErroPedidoException("PEDIDO_INVALIDO");
        }

        List<Item> itens = itensRequest.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(),
                        i.quantidade() == null ? 0 : i.quantidade(), i.pesoKg()))
                .toList();

        for (Item item : itens) {
            if (!item.valido()) {
                throw new ErroPedidoException("PEDIDO_INVALIDO");
            }
        }

        return itens;
    }

    private NivelClube validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new ErroPedidoException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new ErroPedidoException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new ErroPedidoException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom validarCupom(String cupom) {
        if (cupom == null || cupom.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamentoValidada validarFormaPagamento(String formaPagamentoCodigo, Integer parcelasRequest) {
        if (formaPagamentoCodigo == null) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(formaPagamentoCodigo);
        } catch (IllegalArgumentException e) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = parcelasRequest == null ? 1 : parcelasRequest;
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new ErroPedidoException("PARCELAMENTO_INVALIDO");
        }

        return new FormaPagamentoValidada(formaPagamento, parcelas);
    }

    private record FormaPagamentoValidada(FormaPagamento formaPagamento, int parcelas) {
    }
}
