package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.NivelClubeRegistry;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.CupomRegistry;
import com.loja.checkout.dominio.entrega.PoliticaEntrega;
import com.loja.checkout.dominio.entrega.PoliticaEntregaRegistry;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. Esta é a parte igual em todos os pedidos: a ordem
 * das etapas do cálculo e a ordem em que os problemas são verificados. O que
 * muda caso a caso (entrega, cupom, pagamento, nível) fica em cada estratégia,
 * escolhida por lookup nos registros.
 */
@Service
public class CalculadoraResumo {

    private final PoliticaEntregaRegistry entregas;
    private final CupomRegistry cupons;
    private final FormaPagamentoRegistry pagamentos;
    private final NivelClubeRegistry niveis;

    public CalculadoraResumo(PoliticaEntregaRegistry entregas,
                             CupomRegistry cupons,
                             FormaPagamentoRegistry pagamentos,
                             NivelClubeRegistry niveis) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
        this.niveis = niveis;
    }

    public ResumoResponse calcular(ResumoRequest req) {
        // 1. Carrinho e itens válidos.
        List<ItemPedido> itens = validarItens(req.itens());

        // 2. Nível do clube.
        NivelClube nivel = niveis.buscar(req.nivelClube())
                .orElseThrow(() -> new PedidoRejeitadoException("NIVEL_CLUBE_INVALIDO"));

        // 3. Região.
        Regiao regiao = Regiao.de(req.regiao())
                .orElseThrow(() -> new PedidoRejeitadoException("REGIAO_INVALIDA"));

        // 4. Modalidade de entrega existe.
        PoliticaEntrega politica = entregas.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRejeitadoException("MODALIDADE_INVALIDA"));

        // 5. Modalidade atende o peso do pedido.
        BigDecimal peso = pesoTotal(itens);
        if (!politica.disponivel(peso)) {
            throw new PedidoRejeitadoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.centavos(somaProdutos(itens));
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : Dinheiro.centavos(politica.custo(peso));

        // 6. Cupom informado existe.
        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = cupons.buscar(req.cupom())
                    .orElseThrow(() -> new PedidoRejeitadoException("CUPOM_INVALIDO"));
        }

        // 7. Cupom cumpre a condição, e então o desconto.
        BigDecimal desconto = Dinheiro.ZERO;
        if (cupom != null) {
            ContextoCupom ctx = new ContextoCupom(subtotal, itens, frete);
            if (!cupom.aplicavel(ctx)) {
                throw new PedidoRejeitadoException("CUPOM_NAO_APLICAVEL");
            }
            desconto = Dinheiro.centavos(cupom.desconto(ctx));
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        // 8. Forma de pagamento existe.
        FormaPagamento forma = pagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new PedidoRejeitadoException("FORMA_PAGAMENTO_INVALIDA"));

        // 9. Número de parcelas permitido para a forma.
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new PedidoRejeitadoException("PARCELAMENTO_INVALIDO");
        }

        // 10. Forma atende o pedido.
        if (!forma.disponivel(totalPedido)) {
            throw new PedidoRejeitadoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = pagamento.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                politica.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRejeitadoException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validados = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || !positivo(item.pesoKg())
                    || item.quantidade() == null
                    || item.quantidade() <= 0) {
                throw new PedidoRejeitadoException("PEDIDO_INVALIDO");
            }
            validados.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validados;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somaProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalItem());
        }
        return soma;
    }

    private BigDecimal pesoTotal(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoItem());
        }
        return soma;
    }
}
