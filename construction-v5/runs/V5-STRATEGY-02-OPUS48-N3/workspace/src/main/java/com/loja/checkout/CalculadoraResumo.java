package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.RegistroClube;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.comum.PedidoRecusadoException;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.cupom.RegistroCupons;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.RegistroEntregas;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.RegistroPagamentos;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.pedido.ItemPedido;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra. Aqui mora o que é igual a todo pedido: a ordem do
 * cálculo e a ordem das validações. O que varia por caso (entrega, cupom, nível,
 * pagamento) é resolvido por lookup nos registros e delegado a cada caso.
 */
@Service
public class CalculadoraResumo {

    private final RegistroEntregas entregas;
    private final RegistroCupons cupons;
    private final RegistroClube clube;
    private final RegistroPagamentos pagamentos;

    public CalculadoraResumo(RegistroEntregas entregas, RegistroCupons cupons,
                             RegistroClube clube, RegistroPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.clube = clube;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest req) {
        // 1. Itens válidos -> subtotal e peso.
        List<ItemPedido> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somaTotalItens(itens));
        BigDecimal peso = somaPesoItens(itens);

        // 2. Nível do clube.
        NivelClube nivel = clube.buscar(req.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException("NIVEL_CLUBE_INVALIDO"));

        // 3. Região.
        Regiao regiao = Regiao.porNome(req.regiao())
                .orElseThrow(() -> new PedidoRecusadoException("REGIAO_INVALIDA"));

        // 4. Modalidade de entrega.
        ModalidadeEntrega modalidade = entregas.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException("MODALIDADE_INVALIDA"));

        // 5. Modalidade atende o pedido?
        if (!modalidade.atende(peso)) {
            throw new PedidoRecusadoException("MODALIDADE_INDISPONIVEL");
        }

        // Frete exibido: OURO (ou quem tenha frete grátis) zera o frete.
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(peso);

        // 6 e 7. Cupom (ausente = sem cupom).
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (temCupom(req.cupom())) {
            Cupom cupom = cupons.buscar(req.cupom())
                    .orElseThrow(() -> new PedidoRecusadoException("CUPOM_INVALIDO"));
            CupomContexto ctx = new CupomContexto(subtotal, frete, itens);
            if (!cupom.aplicavel(ctx)) {
                throw new PedidoRecusadoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(ctx);
        }

        // Seguro: percentual da região sobre os produtos (sem desconto e sem frete).
        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentualSeguro()));

        // Total do pedido.
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8. Forma de pagamento.
        FormaPagamento forma = pagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException("FORMA_PAGAMENTO_INVALIDA"));

        // 9. Parcelas permitidas (ausente = 1).
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
        }

        // 10. Forma atende o pedido?
        if (!forma.atende(totalPedido)) {
            throw new PedidoRecusadoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste do pagamento sobre o total.
        ResultadoPagamento pagamento = forma.calcula(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(pagamento.valorFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.valorFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ResumoRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validos = new ArrayList<>();
        for (ResumoRequest.Item item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new PedidoRecusadoException("PEDIDO_INVALIDO");
            }
            validos.add(new ItemPedido(item.nome(), item.precoUnitario(),
                    item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal somaTotalItens(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalLinha());
        }
        return soma;
    }

    private BigDecimal somaPesoItens(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoLinha());
        }
        return soma;
    }

    private boolean temCupom(String cupom) {
        return cupom != null && !cupom.isBlank();
    }
}
