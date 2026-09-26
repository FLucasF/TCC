package com.loja.checkout.service;

import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.Money;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.NegocioException;
import com.loja.checkout.pagamento.CalculadoraPagamento;
import com.loja.checkout.pagamento.CalculadoraPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoCompraService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final CalculadoraPagamentoRegistry calculadoraPagamentoRegistry;

    public ResumoCompraService(ModalidadeEntregaRegistry modalidadeEntregaRegistry,
                                CupomRegistry cupomRegistry,
                                CalculadoraPagamentoRegistry calculadoraPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.calculadoraPagamentoRegistry = calculadoraPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemPedido> itens = validarECriarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());

        BigDecimal pesoTotal = itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new NegocioException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Money.round(itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal freteBase = Money.round(modalidade.calcularFrete(pesoTotal));
        BigDecimal freteExibido = nivelClube.isentoFrete() ? BigDecimal.ZERO : freteBase;

        Cupom cupom = validarCupom(request.cupom());
        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(subtotalProdutos, itens, freteExibido);
            if (!cupom.aplicavel(contextoCupom)) {
                throw new NegocioException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Money.round(cupom.calcularDesconto(contextoCupom));
        }

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        CalculadoraPagamento calculadoraPagamento = calculadoraPagamentoRegistry.buscar(formaPagamento)
                .orElseThrow(() -> new NegocioException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!calculadoraPagamento.parcelasValidas(parcelas)) {
            throw new NegocioException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal imposto = Money.round(subtotalProdutos.subtract(descontoCupom).multiply(regiao.percentualImposto()));

        BigDecimal totalPedido = Money.round(subtotalProdutos
                .subtract(descontoCupom)
                .add(freteExibido)
                .add(imposto));

        if (!calculadoraPagamento.disponivelPara(totalPedido)) {
            throw new NegocioException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = calculadoraPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Money.round(subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                freteExibido,
                modalidade.prazoDias(),
                imposto,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemPedido> validarECriarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new NegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itensRequest.stream()
                .map(this::validarECriarItem)
                .toList();
    }

    private ItemPedido validarECriarItem(ItemRequest itemRequest) {
        if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                || itemRequest.pesoKg() == null || itemRequest.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(), itemRequest.pesoKg());
    }

    private NivelClube validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new NegocioException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new NegocioException(CodigoErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private ModalidadeEntrega validarModalidade(String modalidadeEntrega) {
        return modalidadeEntregaRegistry.buscar(modalidadeEntrega)
                .orElseThrow(() -> new NegocioException(CodigoErro.MODALIDADE_INVALIDA));
    }

    private Cupom validarCupom(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        return cupomRegistry.buscar(codigoCupom)
                .orElseThrow(() -> new NegocioException(CodigoErro.CUPOM_INVALIDO));
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new NegocioException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
    }
}
