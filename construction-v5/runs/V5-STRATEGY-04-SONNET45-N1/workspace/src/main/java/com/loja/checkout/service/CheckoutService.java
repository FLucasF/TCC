package com.loja.checkout.service;

import com.loja.checkout.clube.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.*;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.CalculadoraPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {
    private final Map<ModalidadeEntrega, CalculadoraEntrega> calculadorasEntrega;
    private final Map<String, Cupom> cupons;
    private final Map<NivelClube, BeneficioClube> beneficiosClube;
    private final Map<FormaPagamento, CalculadoraPagamento> calculadorasPagamento;

    public CheckoutService() {
        this.calculadorasEntrega = criarCalculadorasEntrega();
        this.cupons = criarCupons();
        this.beneficiosClube = criarBeneficiosClube();
        this.calculadorasPagamento = criarCalculadorasPagamento();
    }

    private Map<ModalidadeEntrega, CalculadoraEntrega> criarCalculadorasEntrega() {
        Map<ModalidadeEntrega, CalculadoraEntrega> map = new HashMap<>();
        map.put(ModalidadeEntrega.ECONOMICA, new Economica());
        map.put(ModalidadeEntrega.EXPRESSA, new Expressa());
        map.put(ModalidadeEntrega.RETIRADA_LOJA, new RetiradaLoja());
        map.put(ModalidadeEntrega.MOTOBOY, new Motoboy());
        return map;
    }

    private Map<String, Cupom> criarCupons() {
        Map<String, Cupom> map = new HashMap<>();
        Cupom[] todosCupons = {
            new BemVindo10(),
            new Menos50(),
            new FreteGratis(),
            new Leve3Pague2()
        };
        for (Cupom cupom : todosCupons) {
            map.put(cupom.getCodigo(), cupom);
        }
        return map;
    }

    private Map<NivelClube, BeneficioClube> criarBeneficiosClube() {
        Map<NivelClube, BeneficioClube> map = new HashMap<>();
        map.put(NivelClube.BRONZE, new Bronze());
        map.put(NivelClube.PRATA, new Prata());
        map.put(NivelClube.OURO, new Ouro());
        return map;
    }

    private Map<FormaPagamento, CalculadoraPagamento> criarCalculadorasPagamento() {
        Map<FormaPagamento, CalculadoraPagamento> map = new HashMap<>();
        map.put(FormaPagamento.PIX, new com.loja.checkout.pagamento.Pix());
        map.put(FormaPagamento.CARTAO, new com.loja.checkout.pagamento.Cartao());
        map.put(FormaPagamento.BOLETO, new com.loja.checkout.pagamento.Boleto());
        return map;
    }

    public ResumoResponse calcularResumo(PedidoRequest request) {
        validarPedido(request);

        Pedido pedido = criarPedido(request);
        BeneficioClube beneficioClube = obterBeneficioClube(request.getNivelClube());
        CalculadoraEntrega calculadoraEntrega = obterCalculadoraEntrega(request.getModalidadeEntrega());
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;

        validarModalidadeDisponivel(calculadoraEntrega, pedido);
        validarFormaPagamento(request.getFormaPagamento(), parcelas);

        Resumo resumo = new Resumo();

        BigDecimal subtotalProdutos = pedido.getSubtotalProdutos();
        resumo.setSubtotalProdutos(subtotalProdutos);

        BigDecimal frete = calcularFrete(calculadoraEntrega, beneficioClube, pedido);
        resumo.setFrete(frete);
        resumo.setPrazoEntregaDias(calculadoraEntrega.getPrazoEntregaDias());

        BigDecimal descontoCupom = calcularDescontoCupom(request.getCupom(), pedido, frete);
        resumo.setDescontoCupom(descontoCupom);

        BigDecimal seguro = calcularSeguro(pedido);
        resumo.setSeguro(seguro);

        BigDecimal totalPedido = resumo.getTotalPedido();

        CalculadoraPagamento calculadoraPagamento = obterCalculadoraPagamento(request.getFormaPagamento());
        validarFormaPagamentoDisponivel(calculadoraPagamento, totalPedido);

        BigDecimal ajustePagamento = calculadoraPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = calculadoraPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal valorParcela = calculadoraPagamento.calcularValorParcela(totalPedido, parcelas);

        resumo.setAjustePagamento(ajustePagamento);
        resumo.setTotalFinal(totalFinal);
        resumo.setParcelas(parcelas);
        resumo.setValorParcela(valorParcela);

        BigDecimal credito = beneficioClube.calcularCredito(pedido);
        resumo.setCreditoProximaCompra(credito);

        boolean brinde = beneficioClube.ganhaBrinde(subtotalProdutos);
        resumo.setBrinde(brinde);

        return converterParaResponse(resumo);
    }

    private void validarPedido(PedidoRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemDto item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (request.getNivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (request.getRegiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (request.getModalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private Pedido criarPedido(PedidoRequest request) {
        List<Item> itens = request.getItens().stream()
                .map(dto -> new Item(
                        dto.getNome(),
                        dto.getPrecoUnitario(),
                        dto.getQuantidade(),
                        dto.getPesoKg()
                ))
                .toList();

        return new Pedido(itens, request.getRegiao());
    }

    private BeneficioClube obterBeneficioClube(NivelClube nivel) {
        BeneficioClube beneficio = beneficiosClube.get(nivel);
        if (beneficio == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return beneficio;
    }

    private CalculadoraEntrega obterCalculadoraEntrega(ModalidadeEntrega modalidade) {
        CalculadoraEntrega calculadora = calculadorasEntrega.get(modalidade);
        if (calculadora == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return calculadora;
    }

    private void validarModalidadeDisponivel(CalculadoraEntrega calculadora, Pedido pedido) {
        if (!calculadora.podeAtender(pedido)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarFormaPagamento(FormaPagamento forma, int parcelas) {
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        CalculadoraPagamento calculadora = calculadorasPagamento.get(forma);
        if (calculadora == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!calculadora.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(CalculadoraPagamento calculadora, BigDecimal totalPedido) {
        if (!calculadora.podeAtender(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularFrete(CalculadoraEntrega calculadora, BeneficioClube beneficio, Pedido pedido) {
        if (beneficio.temFreteGratis()) {
            return BigDecimal.ZERO;
        }
        return calculadora.calcularFrete(pedido);
    }

    private BigDecimal calcularDescontoCupom(String codigoCupom, Pedido pedido, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Cupom cupom = cupons.get(codigoCupom);
        if (cupom == null) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        if (!cupom.podeAplicar(pedido, frete)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        return cupom.calcularDesconto(pedido, frete);
    }

    private BigDecimal calcularSeguro(Pedido pedido) {
        return pedido.getSubtotalProdutos()
                .multiply(pedido.getRegiao().getPercentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private CalculadoraPagamento obterCalculadoraPagamento(FormaPagamento forma) {
        return calculadorasPagamento.get(forma);
    }

    private ResumoResponse converterParaResponse(Resumo resumo) {
        ResumoResponse response = new ResumoResponse();
        response.setSubtotalProdutos(resumo.getSubtotalProdutos().setScale(2, RoundingMode.HALF_EVEN));
        response.setDescontoCupom(resumo.getDescontoCupom().setScale(2, RoundingMode.HALF_EVEN));
        response.setFrete(resumo.getFrete().setScale(2, RoundingMode.HALF_EVEN));
        response.setPrazoEntregaDias(resumo.getPrazoEntregaDias());
        response.setSeguro(resumo.getSeguro().setScale(2, RoundingMode.HALF_EVEN));
        response.setAjustePagamento(resumo.getAjustePagamento().setScale(2, RoundingMode.HALF_EVEN));
        response.setTotalFinal(resumo.getTotalFinal().setScale(2, RoundingMode.HALF_EVEN));
        response.setParcelas(resumo.getParcelas());
        response.setValorParcela(resumo.getValorParcela().setScale(2, RoundingMode.HALF_EVEN));
        response.setCreditoProximaCompra(resumo.getCreditoProximaCompra().setScale(2, RoundingMode.HALF_EVEN));
        response.setBrinde(resumo.isBrinde());
        return response;
    }
}
