package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.clube.NivelClube;
import com.loja.checkout.service.clube.NivelClubeRegistry;
import com.loja.checkout.service.cupom.ContextoCupom;
import com.loja.checkout.service.cupom.Cupom;
import com.loja.checkout.service.cupom.CupomRegistry;
import com.loja.checkout.service.entrega.ModalidadeEntrega;
import com.loja.checkout.service.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.service.pagamento.PagamentoService;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final NivelClubeRegistry nivelClubeRegistry;
    private final PagamentoService pagamentoService;

    public ResumoService(ModalidadeEntregaRegistry modalidadeEntregaRegistry,
                          CupomRegistry cupomRegistry,
                          NivelClubeRegistry nivelClubeRegistry,
                          PagamentoService pagamentoService) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.nivelClubeRegistry = nivelClubeRegistry;
        this.pagamentoService = pagamentoService;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = Money.round(calcularSubtotal(itens));
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        validarRegiao(request.regiao());

        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteNominal = Money.round(modalidade.calcularFrete(pesoTotal));
        BigDecimal frete = nivelClube.isFreteGratis() ? new BigDecimal("0.00") : freteNominal;
        int prazoEntregaDias = modalidade.getPrazoDias();

        Cupom cupom = validarCupom(request.cupom());
        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(itens, subtotalProdutos, frete);
            if (!cupom.isAplicavel(contextoCupom)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(contextoCupom);
        }

        validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!pagamentoService.isParcelamentoValido(request.formaPagamento(), parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal percentualSeguro = SeguroRegiao.percentual(request.regiao());
        BigDecimal seguro = Money.round(subtotalProdutos.multiply(percentualSeguro));

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamentoService.isDisponivel(request.formaPagamento(), totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = pagamentoService.calcular(request.formaPagamento(), totalPedido, parcelas);

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                seguro,
                resultadoPagamento.ajustePagamento(),
                resultadoPagamento.totalFinal(),
                resultadoPagamento.parcelas(),
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }

    private NivelClube validarNivelClube(String codigo) {
        NivelClube nivelClube = codigo == null ? null : nivelClubeRegistry.buscar(codigo);
        if (nivelClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return nivelClube;
    }

    private void validarRegiao(String regiao) {
        if (regiao == null || !SeguroRegiao.existe(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : modalidadeEntregaRegistry.buscar(codigo);
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return modalidade;
    }

    private Cupom validarCupom(String codigo) {
        if (codigo == null) {
            return null;
        }
        Cupom cupom = cupomRegistry.buscar(codigo);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private void validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null || !pagamentoService.isFormaValida(formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
