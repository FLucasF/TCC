package com.loja.checkout.api;

import com.loja.checkout.api.ResumoRequest.ItemRequest;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ValoresPedido;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.BaseCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.Frete;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.Parcelamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> entregas, Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveisClube, Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = pedidoDe(request.itens());
        NivelClube nivel = niveisClube.buscar(request.nivelClube())
                .orElseThrow(() -> new ErroCheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.porNome(request.regiao())
                .orElseThrow(() -> new ErroCheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = entregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new ErroCheckoutException("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        Frete frete = modalidade.calcular(pedido);
        BigDecimal valorFrete = nivel.isentaFrete() ? Dinheiro.ZERO : Dinheiro.centavos(frete.valor());

        BigDecimal descontoCupom = descontoCupom(request.cupom(),
                new BaseCupom(pedido, subtotalProdutos, valorFrete));
        BigDecimal imposto = regiao.imposto(subtotalProdutos.subtract(descontoCupom));
        ValoresPedido valores = new ValoresPedido(subtotalProdutos, descontoCupom, valorFrete, imposto);

        FormaPagamento pagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Optional.ofNullable(request.parcelas()).orElse(1);
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroCheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(valores)) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Parcelamento parcelamento = pagamento.calcular(valores, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                valorFrete,
                frete.prazoDias(),
                imposto,
                Dinheiro.centavos(parcelamento.totalFinal().subtract(valores.total())),
                Dinheiro.centavos(parcelamento.totalFinal()),
                parcelas,
                Dinheiro.centavos(parcelamento.valorParcela()),
                nivel.credito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private BigDecimal descontoCupom(String codigo, BaseCupom base) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new ErroCheckoutException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(base)) {
            throw new ErroCheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(base);
    }

    private Pedido pedidoDe(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(this::itemInvalido)) {
            throw new ErroCheckoutException("PEDIDO_INVALIDO");
        }
        return new Pedido(itens.stream()
                .map(item -> new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private boolean itemInvalido(ItemRequest item) {
        return item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
