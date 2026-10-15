package com.loja.checkout;

import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.clube.ClubeService;
import com.loja.checkout.clube.ClubeStrategy;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.cupom.CupomStrategy;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ValidadorItens;
import com.loja.checkout.entrega.EntregaService;
import com.loja.checkout.entrega.EntregaStrategy;
import com.loja.checkout.pagamento.PagamentoService;
import com.loja.checkout.pagamento.PagamentoStrategy;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.regiao.RegiaoService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculoResumoService {

    private final ValidadorItens validadorItens;
    private final ClubeService clubeService;
    private final RegiaoService regiaoService;
    private final EntregaService entregaService;
    private final CupomService cupomService;
    private final PagamentoService pagamentoService;

    public CalculoResumoService(ValidadorItens validadorItens, ClubeService clubeService,
            RegiaoService regiaoService, EntregaService entregaService, CupomService cupomService,
            PagamentoService pagamentoService) {
        this.validadorItens = validadorItens;
        this.clubeService = clubeService;
        this.regiaoService = regiaoService;
        this.entregaService = entregaService;
        this.cupomService = cupomService;
        this.pagamentoService = pagamentoService;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        List<Item> itens = validadorItens.validar(request.itens());
        BigDecimal subtotalProdutos = somarSubtotal(itens);
        BigDecimal pesoTotalKg = somarPeso(itens);

        ClubeStrategy clube = clubeService.buscar(request.nivelClube());
        Regiao regiao = regiaoService.buscar(request.regiao());
        EntregaStrategy entrega = entregaService.buscar(request.modalidadeEntrega());
        entregaService.validarDisponibilidade(entrega, pesoTotalKg, subtotalProdutos);

        CupomStrategy cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = cupomService.buscar(request.cupom());
            cupomService.validarAplicavel(cupom, itens, subtotalProdutos);
        }

        PagamentoStrategy pagamento = pagamentoService.buscar(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        pagamentoService.validarParcelas(pagamento, parcelas);

        BigDecimal frete = clube.fretesGratis()
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(entrega.calcularFrete(pesoTotalKg, subtotalProdutos));

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(cupom.calcularDesconto(itens, subtotalProdutos, frete));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        pagamentoService.validarDisponibilidade(pagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = pagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(subtotalProdutos.multiply(clube.percentualCredito()));
        boolean brinde = clube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                Dinheiro.arredondar(subtotalProdutos),
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.arredondar(resultadoPagamento.ajuste()),
                Dinheiro.arredondar(resultadoPagamento.totalFinal()),
                resultadoPagamento.parcelas(),
                Dinheiro.arredondar(resultadoPagamento.valorParcela()),
                creditoProximaCompra,
                brinde);
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.subtotal());
        }
        return subtotal;
    }

    private BigDecimal somarPeso(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.pesoTotal());
        }
        return peso;
    }
}
