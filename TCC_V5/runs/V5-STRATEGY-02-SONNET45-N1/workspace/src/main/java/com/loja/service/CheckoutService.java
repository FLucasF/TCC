package com.loja.service;

import com.loja.domain.Item;
import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.enums.Regiao;
import com.loja.exception.*;
import com.loja.strategy.clube.*;
import com.loja.strategy.cupom.*;
import com.loja.strategy.entrega.*;
import com.loja.strategy.pagamento.*;
import com.loja.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades = new HashMap<>();
    private final Map<String, NivelClube> niveisClube = new HashMap<>();
    private final Map<String, FormaPagamento> formasPagamento = new HashMap<>();

    public CheckoutService() {
        modalidades.put("ECONOMICA", new Economica());
        modalidades.put("EXPRESSA", new Expressa());
        modalidades.put("RETIRADA_LOJA", new RetiradaLoja());
        modalidades.put("MOTOBOY", new Motoboy());

        niveisClube.put("BRONZE", new Bronze());
        niveisClube.put("PRATA", new Prata());
        niveisClube.put("OURO", new Ouro());

        formasPagamento.put("PIX", new Pix());
        formasPagamento.put("BOLETO", new Boleto());
        formasPagamento.put("CARTAO", new Cartao());
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        List<Item> itens = converterItens(request.getItens());
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);

        NivelClube clube = niveisClube.get(request.getNivelClube());
        ModalidadeEntrega modalidade = modalidades.get(request.getModalidadeEntrega());
        FormaPagamento formaPagamento = formasPagamento.get(request.getFormaPagamento());
        Regiao regiao = Regiao.valueOf(request.getRegiao());

        BigDecimal freteCalculado = modalidade.calcularFrete(calcularPesoTotal(itens));
        BigDecimal frete;
        BigDecimal descontoCupom;

        if (clube.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
            Cupom cupom = obterCupom(request.getCupom(), itens);
            descontoCupom = cupom != null ?
                cupom.calcularDesconto(subtotalProdutos, BigDecimal.ZERO.setScale(2)) :
                BigDecimal.ZERO.setScale(2);
        } else {
            frete = freteCalculado;
            Cupom cupom = obterCupom(request.getCupom(), itens);
            descontoCupom = cupom != null ?
                cupom.calcularDesconto(subtotalProdutos, frete) :
                BigDecimal.ZERO.setScale(2);
        }

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.ganhaBrinde(subtotalProdutos);

        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(modalidade.getPrazo());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(parcelas);
        response.setValorParcela(valorParcela);
        response.setCreditoProximaCompra(credito);
        response.setBrinde(brinde);

        return response;
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new PedidoInvalidoException();
        }
        for (ItemRequest item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoInvalidoException();
            }
        }

        if (request.getNivelClube() == null || !niveisClube.containsKey(request.getNivelClube())) {
            throw new NivelClubeInvalidoException();
        }

        if (request.getRegiao() == null) {
            throw new RegiaoInvalidaException();
        }
        try {
            Regiao.valueOf(request.getRegiao());
        } catch (IllegalArgumentException e) {
            throw new RegiaoInvalidaException();
        }

        if (request.getModalidadeEntrega() == null || !modalidades.containsKey(request.getModalidadeEntrega())) {
            throw new ModalidadeInvalidaException();
        }

        ModalidadeEntrega modalidade = modalidades.get(request.getModalidadeEntrega());
        List<Item> itens = converterItens(request.getItens());
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.aceita(pesoTotal)) {
            throw new ModalidadeIndisponivelException();
        }

        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            if (!cupomValido(request.getCupom())) {
                throw new CupomInvalidoException();
            }

            Cupom cupom = obterCupom(request.getCupom(), itens);
            BigDecimal subtotal = calcularSubtotalProdutos(itens);
            if (!cupom.aplicavel(subtotal)) {
                throw new CupomNaoAplicavelException();
            }
        }

        if (request.getFormaPagamento() == null || !formasPagamento.containsKey(request.getFormaPagamento())) {
            throw new FormaPagamentoInvalidaException();
        }

        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        FormaPagamento formaPagamento = formasPagamento.get(request.getFormaPagamento());
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new ParcelamentoInvalidoException();
        }

        BigDecimal subtotal = calcularSubtotalProdutos(itens);
        NivelClube clube = niveisClube.get(request.getNivelClube());
        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = clube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteCalculado;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.getCupom() != null && !request.getCupom().isEmpty()) {
            Cupom cupom = obterCupom(request.getCupom(), itens);
            BigDecimal freteParaCupom = clube.freteGratis() ? BigDecimal.ZERO.setScale(2) : frete;
            descontoCupom = cupom.calcularDesconto(subtotal, freteParaCupom);
        }

        Regiao regiao = Regiao.valueOf(request.getRegiao());
        BigDecimal seguro = calcularSeguro(subtotal, regiao);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.aceita(totalPedido)) {
            throw new FormaPagamentoIndisponivelException();
        }
    }

    private boolean cupomValido(String codigo) {
        return codigo.equals("BEMVINDO10") ||
               codigo.equals("MENOS50") ||
               codigo.equals("FRETEGRATIS") ||
               codigo.equals("LEVE3PAGUE2");
    }

    private Cupom obterCupom(String codigo, List<Item> itens) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }

        switch (codigo) {
            case "BEMVINDO10":
                return new BemVindo10();
            case "MENOS50":
                return new Menos50();
            case "FRETEGRATIS":
                return new FreteGratis();
            case "LEVE3PAGUE2":
                return new Leve3Pague2(itens);
            default:
                return null;
        }
    }

    private List<Item> converterItens(List<ItemRequest> itensRequest) {
        return itensRequest.stream()
            .map(this::converterItem)
            .collect(Collectors.toList());
    }

    private Item converterItem(ItemRequest request) {
        Item item = new Item();
        item.setNome(request.getNome());
        item.setPrecoUnitario(request.getPrecoUnitario());
        item.setQuantidade(request.getQuantidade());
        item.setPesoKg(request.getPesoKg());
        return item;
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.calcularSubtotal());
        }
        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.calcularPeso());
        }
        return peso;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return Arredondamento.arredondar(
            subtotalProdutos.multiply(regiao.getTaxa())
        );
    }
}
