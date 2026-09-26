# run-pilot.ps1
# Piloto TCC - harness vs default em cenario de padroes de projeto.
#
# Estrutura esperada:
#   $Base\prompt.txt   -> o prompt neutro da tarefa (uma mensagem so)
#   $Base\CLAUDE.md    -> a regra unica da condicao B
#   $Base\runs\        -> criado automaticamente, um diretorio por run
#   $Base\runs.csv     -> saida acumulada
#
# Uso:  .\run-pilot.ps1 -Base "J:\TCC\piloto" -Reps 3

param(
    [string]$Base = "J:\TCC\piloto",
    [int]$Reps = 3,
    [string[]]$Modelos = @("opus", "sonnet"),
    [switch]$Shuffle
)

$promptFile = Join-Path $Base "prompt.txt"
$harness    = Join-Path $Base "CLAUDE.md"
$csv        = Join-Path $Base "runs.csv"

if (-not (Test-Path $promptFile)) { throw "Nao achei $promptFile" }
if (-not (Test-Path $harness))    { throw "Nao achei $harness" }

$header = "run_id,timestamp,modelo,condicao,rep,input_tokens,cache_creation,cache_read,total_input,output_tokens,num_turns,duration_ms,cost_usd,dir"
if (-not (Test-Path $csv)) { $header | Out-File $csv -Encoding utf8 }

# ---------------------------------------------------------------
# Isolamento de ambiente.
#
# O diretorio vazio NAO garante contexto vazio. O Claude Code sobe
# a arvore de diretorios concatenando todo CLAUDE.md que encontra,
# carrega ~/.claude/CLAUDE.md em qualquer projeto, e grava/rele
# auto memory por projeto (o que contaminaria a repeticao 2 e 3
# com o que a repeticao 1 aprendeu).
#
# Confirme o resultado com /context dentro de um diretorio de run:
# e ele, nao a listagem de arquivos, que diz o que foi carregado.
# ---------------------------------------------------------------

# Desliga a auto memory (notas que o modelo decide guardar).
$env:DISABLE_AUTO_MEMORY = "1"

# Aponta o config para uma pasta descartavel, isolando settings,
# skills, agents, commands, hooks, plugins e MCP servers de usuario.
# Variavel nao documentada e com bug conhecido de ainda carregar
# ~/.claude/CLAUDE.md junto -> valide com /context.
$configDir = Join-Path $Base "_claude-config"
New-Item -ItemType Directory -Force -Path $configDir | Out-Null
$env:CLAUDE_CONFIG_DIR = $configDir

# Preflight: aborta se algum ancestral do diretorio de runs tiver
# CLAUDE.md. Por isso vale manter $Base fora da arvore do TCC.
$runsRoot = Join-Path $Base "runs"
$probe = $runsRoot
$achados = @()
while ($probe -and (Split-Path $probe -Parent)) {
    $cand = Join-Path $probe "CLAUDE.md"
    if (Test-Path $cand) { $achados += $cand }
    $probe = Split-Path $probe -Parent
}
if ($achados.Count -gt 0) {
    Write-Host "CLAUDE.md em diretorio ancestral - vazaria nas duas condicoes:" -ForegroundColor Red
    $achados | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    throw "Mova -Base para fora dessa arvore (ex: C:\eval) e rode de novo."
}

$userMem = Join-Path $HOME ".claude\CLAUDE.md"
if (Test-Path $userMem) {
    Write-Warning "Existe memoria de usuario em $userMem."
    Write-Warning "Se /context mostrar esse arquivo, renomeie durante o lote."
}

# Monta a lista de runs. Ordem embaralhada com -Shuffle para diluir
# efeito de ordem (carga do servidor variando ao longo da sessao).
$plano = foreach ($m in $Modelos) {
    foreach ($c in @("A", "B")) {
        1..$Reps | ForEach-Object { [pscustomobject]@{ Modelo = $m; Cond = $c; Rep = $_ } }
    }
}
if ($Shuffle) { $plano = $plano | Get-Random -Count $plano.Count }

foreach ($run in $plano) {
    $runId = "$($run.Modelo)-$($run.Cond)-$($run.Rep)"
    $dir = Join-Path $Base "runs\$runId"

    if (Test-Path $dir) {
        Write-Warning "$runId ja existe, pulando. Apague o diretorio para rodar de novo."
        continue
    }
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    # Condicao A = diretorio vazio (full default).
    # Condicao B = mesmo diretorio + a regra unica no CLAUDE.md.
    if ($run.Cond -eq "B") { Copy-Item $harness (Join-Path $dir "CLAUDE.md") }

    Write-Host "`n=== $runId ===" -ForegroundColor Cyan

    Push-Location $dir
    try {
        $raw = Get-Content $promptFile -Raw |
               claude -p `
                   --model $run.Modelo `
                   --output-format json `
                   --dangerously-skip-permissions 2>&1 | Out-String
    }
    finally { Pop-Location }

    # Guarda o retorno bruto sempre: e a prova da medicao.
    $raw | Out-File (Join-Path $dir "_result.json") -Encoding utf8

    try { $j = $raw | ConvertFrom-Json }
    catch {
        Write-Warning "$runId : JSON invalido. Ver _result.json"
        continue
    }

    if ($j.is_error) { Write-Warning "$runId : run retornou is_error=true" }

    $u  = $j.usage
    $i  = [int]$u.input_tokens
    $cc = [int]$u.cache_creation_input_tokens
    $cr = [int]$u.cache_read_input_tokens

    $linha = @(
        $runId
        (Get-Date -Format "s")
        $run.Modelo
        $run.Cond
        $run.Rep
        $i
        $cc
        $cr
        ($i + $cc + $cr)
        [int]$u.output_tokens
        [int]$j.num_turns
        [int]$j.duration_ms
        $j.total_cost_usd
        $dir
    ) -join ","

    Add-Content $csv $linha

    $seg = [math]::Round($j.duration_ms / 1000, 1)
    Write-Host "ok  ${seg}s  in=$($i + $cc + $cr)  out=$($u.output_tokens)  turns=$($j.num_turns)" -ForegroundColor Green
}

Write-Host "`nCSV: $csv" -ForegroundColor Yellow
Write-Host "Proximo passo: pontuar o codigo de cada run\ pela rubrica, as cegas." -ForegroundColor Yellow
