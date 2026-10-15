package com.loja.checkout.service;

import com.loja.checkout.api.dto.CheckoutRequestDto;
import com.loja.checkout.api.dto.CheckoutResponseDto;
import com.loja.checkout.api.dto.ItemDto;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.PedidoContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaService;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoService;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaService modalidadeEntregaService;
    private final CupomService cupomService;
    private final FormaPagamentoService formaPagamentoService;

    public CheckoutService(ModalidadeEntregaService modalidadeEntregaService,
                            CupomService cupomService,
                            FormaPagamentoService formaPagamentoService) {
        this.modalidadeEntregaService = modalidadeEntregaService;
        this.cupomService = cupomService;
        this.formaPagamentoService = formaPagamentoService;
    }

    public CheckoutResponseDto calcularResumo(CheckoutRequestDto requisicao) {
        PedidoContexto pedido = validarEConstruirPedido(requisicao);

        ModalidadeEntrega modalidade = modalidadeEntregaService.buscar(requisicao.modalidadeEntrega());
        if (!modalidade.disponivelPara(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Arredondamento.paraCentavos(modalidade.calcularFrete(pedido));

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (requisicao.cupom() != null) {
            Cupom cupom = cupomService.buscar(requisicao.cupom());
            if (!cupom.aplicavel(pedido, frete)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Arredondamento.paraCentavos(cupom.calcularDesconto(pedido, frete));
        }

        BigDecimal totalPedido = Arredondamento.paraCentavos(
                pedido.subtotalProdutos().subtract(descontoCupom).add(frete));

        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        FormaPagamento formaPagamento = formaPagamentoService.buscar(requisicao.formaPagamento());
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        return new CheckoutResponseDto(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela()
        );
    }

    private PedidoContexto validarEConstruirPedido(CheckoutRequestDto requisicao) {
        List<ItemDto> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        List<ItemPedido> itensPedido = itens.stream()
                .map(this::validarEConverterItem)
                .toList();

        return PedidoContexto.criar(itensPedido);
    }

    private ItemPedido validarEConverterItem(ItemDto item) {
        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }
}
