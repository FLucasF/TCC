package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons,
                         List<NivelClube> niveis,
                         List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request);
        NivelClube nivel = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega(), itens);
        Cupom cupom = validarCupom(request.cupom());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento forma = validarFormaPagamento(request.formaPagamento(), parcelas);

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal frete = nivel.isFreteGratis()
                ? BigDecimal.ZERO
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (cupom != null) {
            validarCupomAplicavel(cupom, subtotalProdutos, itens);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, itens, frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);
        BigDecimal totalPedido = Arredondamento.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        validarFormaPagamentoDisponivel(forma, totalPedido);

        ResultadoPagamento resultado = forma.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Arredondamento.centavos(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivel.calcularCredito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos)
        );
    }

    private List<ItemPedido> validarItens(ResumoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return request.itens().stream().map(item -> {
            if (item.nome() == null || item.nome().isBlank()
                    || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
        }).toList();
    }

    private NivelClube validarNivelClube(String codigo) {
        if (codigo == null || !niveis.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return niveis.get(codigo);
    }

    private Regiao validarRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String codigo, List<ItemPedido> itens) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return modalidades.get(codigo);
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String codigo) {
        if (codigo == null) {
            return null;
        }
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupons.get(codigo);
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotal, List<ItemPedido> itens) {
        if (!cupom.isAplicavel(subtotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento validarFormaPagamento(String codigo, int parcelas) {
        if (codigo == null || !formasPagamento.containsKey(codigo)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento forma = formasPagamento.get(codigo);
        if (!forma.isParcelasValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        return forma;
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento forma, BigDecimal totalPedido) {
        if (!forma.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(
                    Arredondamento.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
