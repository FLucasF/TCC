# anonimizar.ps1
# Prepara a avaliacao cega. Copia o codigo de cada run para um diretorio
# com ID neutro, remove as pistas da condicao, embaralha a ordem e emite
# duas planilhas:
#
#   cego\rubrica.csv  -> voce preenche as notas olhando so o codigo
#   cego\mapa.csv     -> id_cego -> run_id. NAO ABRA antes de pontuar.
#
# Uso:  .\anonimizar.ps1 -Base "C:\eval"

param(
    [string]$Base = "C:\eval"
)

$runsRoot = Join-Path $Base "runs"
$out      = Join-Path $Base "cego"

if (-not (Test-Path $runsRoot)) { throw "Nao achei $runsRoot" }
if (Test-Path $out) { throw "$out ja existe. Apague antes de refazer." }

# Artefatos que revelam a condicao ou nao fazem parte do codigo avaliado.
$excluir = @("CLAUDE.md", "CLAUDE.local.md", "_result.json", "README.md", "readme.md")
$excluirDir = @(".claude", "target", ".git")

$runs = Get-ChildItem $runsRoot -Directory | Get-Random -Count 1000 -ErrorAction SilentlyContinue
if (-not $runs) { $runs = Get-ChildItem $runsRoot -Directory }

New-Item -ItemType Directory -Force -Path $out | Out-Null

$mapa = @()
$i = 0

foreach ($run in $runs) {
    $i++
    $idCego = "C{0:D2}" -f $i
    $destino = Join-Path $out $idCego
    New-Item -ItemType Directory -Force -Path $destino | Out-Null

    Get-ChildItem $run.FullName -Recurse -File | ForEach-Object {
        $rel = $_.FullName.Substring($run.FullName.Length).TrimStart('\')

        if ($excluir -contains $_.Name) { return }
        if ($excluirDir | Where-Object { $rel -like "$_\*" }) { return }

        $alvo = Join-Path $destino $rel
        New-Item -ItemType Directory -Force -Path (Split-Path $alvo -Parent) | Out-Null
        Copy-Item $_.FullName $alvo
    }

    $mapa += [pscustomobject]@{ id_cego = $idCego; run_id = $run.Name }
}

# Mapa fica escrito, mas o combinado e nao abrir antes de fechar as notas.
$mapa | Export-Csv (Join-Path $out "mapa.csv") -NoTypeInformation -Encoding utf8

# Planilha de pontuacao, na ordem cega.
$rubrica = $mapa | ForEach-Object {
    [pscustomobject]@{
        id_cego   = $_.id_cego
        observer  = ""
        strategy  = ""
        factory   = ""
        overeng   = ""
        notas     = ""
    }
}
$rubrica | Export-Csv (Join-Path $out "rubrica.csv") -NoTypeInformation -Encoding utf8

Write-Host "$i runs anonimizadas em $out" -ForegroundColor Green
Write-Host "Preencha rubrica.csv. So depois abra mapa.csv." -ForegroundColor Yellow

# Ultimo aviso: as vezes o modelo cita a diretriz em comentario ou em
# nome de classe. Passe um grep antes de pontuar:
Write-Host "`nChecando vazamento textual nas copias..." -ForegroundColor Cyan
$termos = "padr(a|ã)o de projeto|design pattern|diretriz|CLAUDE\.md|extens(i|í)vel"
$vaz = Get-ChildItem $out -Recurse -File -Include *.java, *.xml, *.md |
       Select-String -Pattern $termos -AllMatches
if ($vaz) {
    Write-Warning "Mencoes encontradas - revise antes de pontuar:"
    $vaz | ForEach-Object { Write-Host "  $($_.Path):$($_.LineNumber)" }
} else {
    Write-Host "Nenhuma mencao obvia." -ForegroundColor Green
}
