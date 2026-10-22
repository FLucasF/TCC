package br.com.maretalta.report;

import java.util.List;

/**
 * Um relatório já calculado: o que muda de um para outro é o conteúdo; a forma de escrever
 * (título, cabeçalho, linhas, total, e o que mais vier) fica toda em {@link ReportWriter}.
 */
public record Report(String name, String period, List<String> header, List<List<String>> rows, List<String> total) {}
