package com.loja.checkout.servico;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.clube.Nivel;
import com.loja.checkout.dominio.clube.Niveis;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.Entrega;
import com.loja.checkout.dominio.entrega.Entregas;
import com.loja.checkout.dominio.pagamento.Pagamento;
import com.loja.checkout.dominio.pagamento.Pagamentos;
import com.loja.checkout.dominio.regiao.Regiao;
import com.loja.checkout.erro.ErroPedido;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest req) {
        List<Item> itens = validarItens(req.itens);

        Nivel nivel = Niveis.buscar(req.nivelClube)
                .orElseThrow(() -> new ErroPedido("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = Regiao.buscar(req.regiao)
                .orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));

        Entrega entrega = Entregas.buscar(req.modalidadeEntrega)
                .orElseThrow(() -> new ErroPedido("MODALIDADE_INVALIDA"));

        BigDecimal pesoTotal = pesoTotal(itens);
        if (!entrega.disponivelPara(pesoTotal)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.arredondar(subtotal(itens));
        BigDecimal fretePadrao = Dinheiro.arredondar(entrega.frete(pesoTotal));
        BigDecimal frete = Dinheiro.arredondar(nivel.ajustarFrete(fretePadrao));

        Cupom cupom = null;
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (req.cupom != null) {
            cupom = Cupons.buscar(req.cupom)
                    .orElseThrow(() -> new ErroPedido("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(itens, subtotal)) {
                throw new ErroPedido("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = Dinheiro.arredondar(cupom.desconto(itens, subtotal, frete));
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        Pagamento pagamento = Pagamentos.buscar(req.formaPagamento)
                .orElseThrow(() -> new ErroPedido("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivelPara(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Pagamento.Ajuste ajuste = pagamento.calcular(totalPedido, parcelas);

        ResumoResponse r = new ResumoResponse();
        r.subtotalProdutos = subtotal;
        r.descontoCupom = descontoCupom;
        r.frete = frete;
        r.prazoEntregaDias = entrega.prazoDias();
        r.seguro = seguro;
        r.ajustePagamento = ajuste.ajuste();
        r.totalFinal = ajuste.totalFinal();
        r.parcelas = parcelas;
        r.valorParcela = ajuste.valorParcela();
        r.creditoProximaCompra = Dinheiro.arredondar(nivel.credito(subtotal));
        r.brinde = nivel.brinde(subtotal);
        return r;
    }

    private static List<Item> validarItens(List<ItemRequest> entrada) {
        if (entrada == null || entrada.isEmpty()) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>(entrada.size());
        for (ItemRequest i : entrada) {
            if (i == null
                    || i.precoUnitario == null || i.precoUnitario.signum() <= 0
                    || i.quantidade == null || i.quantidade <= 0
                    || i.pesoKg == null || i.pesoKg.signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            itens.add(new Item(i.nome, i.precoUnitario, i.quantidade, i.pesoKg));
        }
        return itens;
    }

    private static BigDecimal subtotal(List<Item> itens) {
        BigDecimal s = BigDecimal.ZERO;
        for (Item i : itens) s = s.add(i.total());
        return s;
    }

    private static BigDecimal pesoTotal(List<Item> itens) {
        BigDecimal p = BigDecimal.ZERO;
        for (Item i : itens) p = p.add(i.pesoTotal());
        return p;
    }
}
