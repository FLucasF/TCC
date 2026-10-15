package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.dto.ResumoCheckoutResponse;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.service.clube.BeneficioClube;
import com.loja.checkout.service.clube.BeneficioClubeRegistry;
import com.loja.checkout.service.cupom.CalculadoraCupom;
import com.loja.checkout.service.cupom.CalculadoraCupomRegistry;
import com.loja.checkout.service.entrega.CalculadoraFrete;
import com.loja.checkout.service.entrega.CalculadoraFreteRegistry;
import com.loja.checkout.service.pagamento.CalculadoraPagamento;
import com.loja.checkout.service.pagamento.CalculadoraPagamentoRegistry;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final CalculadoraFreteRegistry freteRegistry;
    private final CalculadoraCupomRegistry cupomRegistry;
    private final CalculadoraPagamentoRegistry pagamentoRegistry;
    private final BeneficioClubeRegistry clubeRegistry;

    public CheckoutService(CalculadoraFreteRegistry freteRegistry,
                            CalculadoraCupomRegistry cupomRegistry,
                            CalculadoraPagamentoRegistry pagamentoRegistry,
                            BeneficioClubeRegistry clubeRegistry) {
        this.freteRegistry = freteRegistry;
        this.cupomRegistry = cupomRegistry;
        this.pagamentoRegistry = pagamentoRegistry;
        this.clubeRegistry = clubeRegistry;
    }

    public ResumoCheckoutResponse calcularResumo(ResumoCheckoutRequest request) {
        List<ItemPedidoRequest> itens = validarItens(request.itens());
        NivelClube nivelClube = parseEnum(request.nivelClube(), NivelClube.class, ErroCodigo.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parseEnum(request.regiao(), Regiao.class, ErroCodigo.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parseEnum(request.modalidadeEntrega(), ModalidadeEntrega.class,
                ErroCodigo.MODALIDADE_INVALIDA);

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotal(itens));
        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        CalculadoraFrete calculadoraFrete = freteRegistry.obter(modalidade);
        if (!calculadoraFrete.disponivelPara(pesoTotalKg)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal freteCalculado = Dinheiro.arredondar(calculadoraFrete.calcularFrete(pesoTotalKg));
        int prazoEntregaDias = calculadoraFrete.prazoDias();

        BeneficioClube beneficioClube = clubeRegistry.obter(nivelClube);
        BigDecimal frete = beneficioClube.freteGratis() ? Dinheiro.zero() : freteCalculado;

        BigDecimal descontoCupom = Dinheiro.zero();
        String codigoCupom = request.cupom();
        if (codigoCupom != null && !codigoCupom.isBlank()) {
            CalculadoraCupom cupom = cupomRegistry.obter(codigoCupom);
            if (cupom == null) {
                throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
            }
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(itens, subtotalProdutos, frete));
        }

        FormaPagamento formaPagamento = parseEnum(request.formaPagamento(), FormaPagamento.class,
                ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        CalculadoraPagamento calculadoraPagamento = pagamentoRegistry.obter(formaPagamento);
        if (!calculadoraPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!calculadoraPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = calculadoraPagamento.calcular(totalPedido, parcelas);
        BigDecimal creditoProximaCompra = Dinheiro.arredondar(beneficioClube.calcularCredito(subtotalProdutos));
        boolean brinde = beneficioClube.temBrinde(subtotalProdutos);

        return new ResumoCheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemPedidoRequest> validarItens(List<ItemPedidoRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        for (ItemPedidoRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemPedidoRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemPedidoRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }

    private <T extends Enum<T>> T parseEnum(String valor, Class<T> tipo, ErroCodigo erroSeInvalido) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(erroSeInvalido);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException(erroSeInvalido);
        }
    }
}
