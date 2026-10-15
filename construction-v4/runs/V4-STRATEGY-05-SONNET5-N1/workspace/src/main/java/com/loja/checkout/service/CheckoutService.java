package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NivelClubeRegistry;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.entrega.OpcaoEntregaRegistry;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final NivelClubeRegistry nivelClubeRegistry;
    private final OpcaoEntregaRegistry opcaoEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public CheckoutService(NivelClubeRegistry nivelClubeRegistry,
                            OpcaoEntregaRegistry opcaoEntregaRegistry,
                            CupomRegistry cupomRegistry,
                            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.nivelClubeRegistry = nivelClubeRegistry;
        this.opcaoEntregaRegistry = opcaoEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivel = nivelClubeRegistry.buscar(request.nivelClube());
        if (nivel == null) {
            throw new PedidoInvalidoException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        Regiao regiao = Regiao.from(request.regiao());
        if (regiao == null) {
            throw new PedidoInvalidoException(CodigoErro.REGIAO_INVALIDA);
        }

        OpcaoEntrega opcaoEntrega = opcaoEntregaRegistry.buscar(request.modalidadeEntrega());
        if (opcaoEntrega == null) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INVALIDA);
        }
        if (!opcaoEntrega.disponivel(pesoTotal)) {
            throw new PedidoInvalidoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = cupomRegistry.buscar(request.cupom());
            if (cupom == null) {
                throw new PedidoInvalidoException(CodigoErro.CUPOM_INVALIDO);
            }
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento());
        if (formaPagamento == null) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal freteBase = Arredondamento.centavos(opcaoEntrega.calcularFrete(pesoTotal));
        BigDecimal frete = nivel.isentoFrete() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Arredondamento.centavos(cupom.calcularDesconto(itens, subtotalProdutos, frete));

        BigDecimal seguro = Arredondamento.centavos(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Arredondamento.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = Arredondamento.centavos(nivel.calcularCredito(subtotalProdutos));
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                opcaoEntrega.prazoEntregaDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                resultadoPagamento.parcelas(),
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoInvalidoException(CodigoErro.PEDIDO_INVALIDO);
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
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
