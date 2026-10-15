package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public CheckoutService(List<ModalidadeEntrega> modalidadesList,
                           List<Cupom> cuponsList,
                           List<NivelClube> niveisList,
                           List<FormaPagamento> pagamentosList) {
        this.modalidades = modalidadesList.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cuponsList.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveisList.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
        this.pagamentos = pagamentosList.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube nivel = niveis.get(pedido.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = resolverRegiao(pedido.regiao());

        ModalidadeEntrega entrega = modalidades.get(pedido.modalidadeEntrega());
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(pedido.itens());
        if (!entrega.disponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        Cupom cupom = resolverCupom(pedido.cupom(), subtotal, pedido.itens());

        FormaPagamento pagamento = pagamentos.get(pedido.formaPagamento());
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = arredondar(entrega.calcularFrete(pesoTotal));
        if (nivel.freteGratis()) frete = BigDecimal.ZERO.setScale(2);

        BigDecimal desconto = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            desconto = cupom.calcularDesconto(pedido.itens(), subtotal, frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        BigDecimal credito = arredondar(subtotal.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.brinde(subtotal);

        return new ResumoResponse(
                subtotal, desconto, frete, entrega.prazoDias(), seguro,
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela(),
                credito, brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private Regiao resolverRegiao(String codigo) {
        if (codigo == null) throw new CheckoutException("REGIAO_INVALIDA");
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private Cupom resolverCupom(String codigo, BigDecimal subtotal, List<ItemRequest> itens) {
        if (codigo == null || codigo.isBlank()) return null;
        Cupom cupom = cupons.get(codigo);
        if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        if (!cupom.aplicavel(itens, subtotal)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        return cupom;
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
