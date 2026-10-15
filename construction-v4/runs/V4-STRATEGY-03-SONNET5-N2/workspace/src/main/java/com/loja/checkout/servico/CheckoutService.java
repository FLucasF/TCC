package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;
import com.loja.checkout.api.ResumoPedidoResponse;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.erro.ErroNegocioException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.pedido.ItemPedido;
import com.loja.checkout.pedido.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static java.math.RoundingMode.HALF_EVEN;

@Service
public class CheckoutService {

    public ResumoPedidoResponse calcularResumo(ResumoPedidoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new ErroNegocioException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteBase = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.freteGratis() ? new BigDecimal("0.00") : freteBase;

        Cupom cupom = validarCupom(request.cupom());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        if (cupom != null && !cupom.aplicavel(itens, subtotalProdutos, frete)) {
            throw new ErroNegocioException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new ErroNegocioException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(itens, subtotalProdutos, frete)
                : new BigDecimal("0.00");

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, HALF_EVEN);

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.totalFinal().subtract(totalPedido).setScale(2, HALF_EVEN);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoPedidoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new ErroNegocioException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> itens = itensRequest.stream()
                .map(item -> new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList();
        for (ItemPedido item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroNegocioException("PEDIDO_INVALIDO");
            }
        }
        return itens;
    }

    private NivelClube validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom validarCupom(String cupom) {
        if (cupom == null) {
            return null;
        }
        try {
            return Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, HALF_EVEN);
    }
}
