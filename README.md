# C-PET

Camada de inteligência que estima, prioriza, conecta, alerta e acompanha animais em situação de rua ou precária. O C-PET não presta o atendimento: orquestra ONGs, clínicas, unidades móveis e órgãos públicos que já existem.

MVP da Global Solution FIAP 2026.2, tema CitizenTech.

**Quem usa**

- Moradores de áreas vulneráveis, que hoje caem em canais separados e sem retorno: reportam em um minuto e acompanham pelo código.
- A central de triagem (perfil OPERADOR): vê a fila por prioridade, entende o porquê do score e encaminha.
- Os recursos parceiros (perfil AGENTE): recebem só os casos encaminhados a eles.
- Famílias e gestores, nos dados abertos por célula e por bairro.

## Requisitos

- Java 17 ou superior (o projeto compila para 17)
- Maven 3.9
- Internet só para os tiles do mapa (OpenStreetMap). Sem rede, o mapa abre sem o fundo e todo o resto funciona.

## Rodar com H2 (padrão)

```bash
mvn spring-boot:run
```

Abra http://localhost:8080. O perfil `h2` é o padrão quando nenhum perfil é informado: banco em memória em modo Oracle, com schema e seed carregados na subida.

## Rodar com Oracle

Crie um usuário vazio no banco e informe a conexão por variáveis de ambiente:

```bash
export CPET_DB_URL=jdbc:oracle:thin:@//host:1521/SERVICO
export CPET_DB_USER=cpet
export CPET_DB_PASSWORD=...
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

No PowerShell: `$env:CPET_DB_URL = "..."` e as demais.

Na primeira subida o app executa `db/schema.sql` (se a tabela CASO não existir) e `db/seed.sql` (se BAIRRO estiver vazia). Nas subidas seguintes não mexe nos dados. Os dois arquivos também podem ser executados à mão no SQL Developer; funcionam em Oracle 12c ou superior.

## Testes

```bash
mvn test
```

44 testes: score, duplicata (Haversine, 50 m, 48 h), encaminhamento (3 mais próximos, vagas, tipos), transições de status e um teste de integração com MockMvc do fluxo completo (reportar, priorizar, encaminhar, aceitar, atender e acompanhar), mais recusa, privacidade e permissões. As regras são lidas do `application.yml` real.

## Telas

| Tela | Quem | O que faz |
|---|---|---|
| `index.html` | cidadão, sem login | Reporta com foto, pino no mapa (ou geolocalização), tipo, quantidade, descrição e contato opcional. Mostra o código. |
| `acompanhar.html` | cidadão | Busca pelo código e mostra a linha do tempo. |
| `dados.html` | público | Densidade agregada por célula e por bairro. |
| `entrar.html` | todos | "Entrar como" com os usuários do seed, sem senha. Também reinicia a demonstração. |
| `painel.html` | OPERADOR | Fila por score com filtros, mapa de casos e células críticas, detalhe com reportes, foto, "por que esta prioridade", 3 recursos sugeridos, encaminhar e arquivar, badge de alertas. |
| `recurso.html` | AGENTE | Casos encaminhados ao recurso: aceitar, recusar e marcar atendido, sempre com observação. |

Os usuários do seed: Marina Duarte (operadora), Rafael Nogueira (agente da Clínica Popular Pata Cuidada) e Beatriz Lemos (agente do Instituto Patas Unidas).

## Regras (todas em `application.yml`, bloco `cpet.regras`)

- **Duplicata:** reporte do mesmo tipo a até 50 m de um caso aberto criado nas últimas 48 h vira novo REPORTE do caso existente.
- **Score (0 a 100):** urgência do tipo (FERIDO 40, DOENTE 30, FILHOTE, MAUS_TRATOS e RISCO_SANITARIO 25, COLONIA 15) + densidade da célula (3 por caso aberto, teto 25) + vulnerabilidade do bairro (0 a 20) + envelhecimento (2 por dia, teto 15). A API devolve o detalhamento por fator.
- **Encaminhamento:** tabela tipo do caso para tipo de recurso, só recursos ativos, com vagas e que aceitem o tipo. Devolve os 3 mais próximos com a distância em km. Encaminhar desconta uma vaga; atender, recusar ou arquivar devolve.
- **Alertas:** `GET /api/alertas` traz os casos atualizados nas últimas 24 h e as células com 5 ou mais casos abertos.
- **Status:** REPORTADO, TRIAGEM, ENCAMINHADO e depois ATENDIDO ou ARQUIVADO. A recusa do agente volta para TRIAGEM. Toda transição é validada e gravada em `HISTORICO_STATUS`.

## Privacidade

- Endpoints públicos devolvem só agregados por célula (coordenadas arredondadas a 2 casas) e o andamento de um código, sem observações internas.
- Coordenada exata, contato e fotos só para usuário autenticado.
- Upload de JPG ou PNG até 5 MB: o formato é conferido pelos bytes, a imagem é recodificada (o que remove o EXIF) e salva com nome UUID em `./uploads`.
- O formulário mostra o aviso de consentimento ao lado do campo de contato.

## Dados simulados

O seed traz 6 bairros de São Paulo, 8 recursos fictícios, 40 casos com concentração em Capão Redondo e Brasilândia, e 3 usuários. Tudo vem com `simulado = 1` e a interface exibe a faixa "Dados simulados para demonstração".

`POST /api/demo/reset` apaga tudo e recria o seed (há um botão em `entrar.html`). Para regerar o `seed.sql` com datas novas: `python -X utf8 tools/gerar_seed.py`.

## Roteiro de demonstração (2 minutos)

Antes de gravar, abra `entrar.html` e use "Reiniciar dados simulados".

1. **0:00 Reportar (25 s).** Em `index.html`, dê zoom em Capão Redondo e toque no mapa. Escolha "Animal ferido", escreva uma linha, anexe uma foto e deixe um contato. Envie e mostre o código em destaque.
2. **0:25 Reportar de novo no mesmo ponto (10 s).** Envie outro reporte do mesmo tipo no mesmo ponto. A tela diz que já havia um reporte e devolve o mesmo código: é a regra de duplicata.
3. **0:35 Acompanhar (10 s).** Em `acompanhar.html`, o código mais recente já vem preenchido neste aparelho: status Reportado e a linha do tempo com as próximas etapas.
4. **0:45 Painel (40 s).** Em `entrar.html`, entre como Marina Duarte. Mostre a fila ordenada por score, o badge de alertas (duas células críticas) e as células tracejadas no mapa. Abra o caso novo: o bloco "Por que esta prioridade" com os quatro fatores, a foto e o contato, e os 3 recursos com distância em km e vagas. Clique em Encaminhar na Clínica Popular Pata Cuidada.
5. **1:25 Recurso (25 s).** Saia, entre como Rafael Nogueira. O caso está em "Aguardando aceite" e as vagas caíram em 1. Aceite e depois marque como atendido com uma observação. As vagas voltam.
6. **1:50 Fechamento (10 s).** Em `acompanhar.html`, atualize a busca: a linha do tempo mostra Atendido. Termine em `dados.html`, que mostra a densidade por célula sem localização exata.

## Estrutura

```
src/main/java/br/com/cpet
  config/    propriedades das regras e do upload
  dominio/   entidades, enums, repositórios
  regras/    Haversine, score, duplicata, sugestão de recursos (lógica pura)
  servico/   casos, alertas, dados abertos, foto, banco
  web/       controllers REST, sessão, tratamento de erros
src/main/resources
  db/        schema.sql e seed.sql (Oracle e H2)
  static/    HTML, CSS, JS, fontes e Leaflet locais
docs/        DESIGN.md e DECISOES.md
tools/       gerador do seed
```

Decisões e simplificações de MVP (incluindo o acesso sem senha) estão em `docs/DECISOES.md`. A direção de design está em `docs/DESIGN.md`.
