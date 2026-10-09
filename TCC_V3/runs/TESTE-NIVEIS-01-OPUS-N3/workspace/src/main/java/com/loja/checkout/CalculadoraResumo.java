package com.loja.checkout;

import com.loja.checkout.api.PedidoRecusado;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Único lugar com a ordem das validações e a ordem do cálculo. Não conhece
 * modalidade, cupom, nível ou forma de pagamento nenhum: só as interfaces.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> pagamentos;

    CalculadoraResumo(Catalogo<ModalidadeEntrega> entregas,
                      Catalogo<Cupom> cupons,
                      Catalogo<NivelClube> niveis,
                      Catalogo<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = pedidoValido(request);
        NivelClube nivel = niveis.buscar(request.nivelClube())
                .orElseThrow(() -> new PedidoRecusado("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.porNome(request.regiao())
                .orElseThrow(() -> new PedidoRecusado("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = entregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusado("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(pedido)) {
            throw new PedidoRecusado("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = pedido.subtotal();
        Entrega entrega = modalidade.apurar(pedido);
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : entrega.frete();

        BigDecimal descontoCupom = descontoCupom(request.cupom(), new ContextoCupom(pedido, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento = pagamentos.buscar(request.formaPagamento())
                .orElseThrow(() -> new PedidoRecusado("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = request.parcelasOuUma();
        if (!pagamento.permiteParcelas(parcelas)) {
            throw new PedidoRecusado("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusado("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.totalFinal().subtract(totalPedido),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.credito(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new PedidoRecusado("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusado("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(contexto);
    }

    private Pedido pedidoValido(ResumoRequest request) {
        List<ResumoRequest.ItemRequest> itens = request.itens();
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(this::itemInvalido)) {
            throw new PedidoRecusado("PEDIDO_INVALIDO");
        }
        return new Pedido(itens.stream()
                .map(item -> new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private boolean itemInvalido(ResumoRequest.ItemRequest item) {
        return item == null
                || naoPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoPositivo(item.pesoKg());
    }

    private boolean naoPositivo(BigDecimal valor) {
        return Optional.ofNullable(valor)
                .map(v -> v.compareTo(BigDecimal.ZERO) <= 0)
                .orElse(true);
    }
}
