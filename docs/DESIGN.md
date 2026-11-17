# Direção de design do C-PET (tema escuro, identidade do pitch final)

Referência: `docs/identidade/Pitch_C-PET_para_CitizenTech.pdf` (páginas 1, 3, 5, 6, 7, 8, 9 e 12). A identidade é escura, menta, com contornos finos, títulos condensados em caixa alta, linhas de rede e ícones de linha. O creme e o terracota das versões anteriores foram descartados.

## 1. Cores

Medi as cores renderizando as páginas do PDF a 40 dpi e contando os pixels próximos de cada valor (`tools/medir_cores_pdf.py`). Resultado, comparado aos valores indicados:

| Token | Valor adotado | O que o PDF mostra (cor mais frequente na vizinhança) |
|---|---|---|
| `--fundo` | `#0F0F0E` | `#0E0E0D` (cerca de 77% dos pixels das páginas medidas) |
| `--superficie-a` | `#0A1D21` | cartões e painéis escuros; vizinhança `#0A1D21` a `#131313` |
| `--superficie-b` | `#0E1A1B` | idem |
| `--menta` | `#3CC3A4` | `#3CC2A3` (títulos em destaque, botões, contornos) |
| `--menta-claro` | `#90D8C8` | `#8BDAC8` |
| `--menta-claro-2` | `#ADE4D8` | `#BAE9DE` (próximo; mantive o valor indicado) |
| `--teal-profundo` | `#105868` | `#155769` ("PET" do wordmark) |
| `--texto` | `#EAF1EF` | o deck usa quase branco (`#F7F7F7`); mantive o valor indicado |
| `--texto-2` | `#9DB2AE` | cinza do deck `#A9A9A9`, mantive o indicado |
| `--borda` | menta a 35% | contornos finos menta translúcidos |

Sinais, definidos por mim porque o deck não tem cor de alerta: âmbar `#E6B450` (`--ambar`, atenção e score médio), vermelho claro `#F2877D` (`--erro`, texto e contorno de urgência e erro) e vermelho sólido `#B3261E` (`--erro-solido`, fundo com texto branco: score alto, contador de alertas). Rampa de densidade de `dados.html`: teal profundo, menta, âmbar, vermelho claro, vermelho sólido (`--rampa-1` a `--rampa-5`, com a cor do texto de cada uma).

**Todas as cores vivem em `css/tokens.css`.** Nenhum CSS, HTML ou JS define cor fora dele (os mapas leem os tokens com `CPET.cor("--nome")`). Os nomes antigos (`--papel`, `--teal`, `--ferrugem`...) continuam existindo no mesmo arquivo como apelidos dos novos, para os componentes antigos funcionarem sem renomear.

**Contraste.** `python tools/contraste.py` confere 36 pares texto/fundo e a borda dos campos (todos os textos sobre as três superfícies, sobre os fundos translúcidos, o texto dos botões e das rampas) e sai com erro se algum ficar abaixo de 4,5:1. Resultado desta versão: nenhum par abaixo do mínimo (o menor é menta sobre `--menta-fundo-forte`, 5,07:1). Duas exceções declaradas, que não são texto: o teal profundo sobre o fundo dá 2,39:1, por isso só aparece no logotipo grande e em decoração (logotipo é isento de contraste mínimo); e a borda de card a 35% dá 2,09:1, por isso os campos de formulário usam `--borda-campo` (menta a 70%, 4,51:1).

## 2. Tipografia

Fontes locais em `static/fonts` (licença OFL, arquivos `OFL-*.txt` ao lado). O projeto não tinha as fontes, então baixei Bebas Neue e DM Sans do repositório `google/fonts` uma única vez e converti para woff2; nada é carregado de CDN em tempo de execução. Títulos e rótulos de seção em Bebas Neue, caixa alta, espaçamento de 0,03 a 0,08 em; corpo e interface em DM Sans (fonte variável, 100 a 1000). Os textos em "mono" (código, coordenadas, números) usam DM Sans com números tabulares. A segunda metade dos títulos de destaque vai em menta (`.dest`).

## 3. Componentes

- **Cartões:** superfície `#0A1D21`, contorno de 1 px menta a 35%, raio de 18 px, sem sombra.
- **Botões:** primário menta cheio com texto `#0F0F0E` em caixa alta; secundário com contorno menta; perigo com contorno vermelho claro.
- **Chips e etiquetas:** retângulo de raio 8 px com contorno. A etiqueta de UC é um chip de uma linha ("UC01 · Estima"); o nome completo e o ator ficam no `title` e em `rastreabilidade.html`.
- **Ícones:** SVG de linha (traço 1,6 a 1,8), soltos ou dentro de quadrado arredondado com contorno menta (`.ico-q`).
- **Campos:** fundo `#0F0F0E`, contorno menta forte, foco menta com anel; `color-scheme: dark` para controles nativos.
- **Linha do tempo:** fio e pontos menta, em `acompanhar.html` e no painel.
- **Selos OPERADOR e AGENTE:** chips no mesmo estilo.
- **Herói:** brilho radial suave em menta e rede de nós e linhas em SVG, só em `index.html` e `sobre.html`. Sem degradê roxo ou azul.

## 4. Cabeçalho e marca

Cabeçalho fixo `#0F0F0E`. O emblema vem de `img/emblema.png`, gerado por `tools/gerar_emblema.py` a partir de `img/logo-cpet.png`: recorta o círculo, apaga o texto do topo e os fragmentos dos cantos e troca o fundo `#121212` do PNG por `#0F0F0E`, para não aparecer disco. O wordmark do cabeçalho é Bebas Neue com "C-" em menta e "PET" em menta claro (variante clara, porque o teal profundo não tem contraste em texto pequeno). A página Sobre usa o wordmark grande como no deck (menta e teal profundo). Menu em DM Sans médio, link ativo com sublinhado menta. Em 390 px a navegação vira barra inferior fixa.

## 5. Mapa

Os tiles do OpenStreetMap passam por um filtro CSS (`--mapa-filtro`: `invert`, `hue-rotate`, `brightness`, `contrast`, saturação baixa e um tom teal por `sepia` e `hue-rotate`), o que dá um mapa grafite com tons teal. Controles, atribuição, popups e tooltips usam a superfície escura e a menta. Pinos de score usam `--score-alto`, `--score-medio` e `--score-baixo`; o pino selecionado ganha anel claro.

## 6. Qualidade

Escala de espaçamento de 4 a 48 px (`--e1` a `--e7`). Transições de 120 ms, desligadas com `prefers-reduced-motion`. Sem emoji, sem dependência externa nova (Bootstrap e Leaflet continuam locais; Bootstrap só para grade e formulários).
