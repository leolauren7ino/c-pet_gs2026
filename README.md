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

**Execução de demonstração em H2 em modo Oracle; script Oracle incluso e não testado em instância real.**

Crie um usuário vazio no banco e informe a conexão por variáveis de ambiente:

```bash
export CPET_DB_URL=jdbc:oracle:thin:@//host:1521/SERVICO
export CPET_DB_USER=cpet
export CPET_DB_PASSWORD=...
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

No PowerShell: `$env:CPET_DB_URL = "..."` e as demais.

Na primeira subida o app executa `db/schema.sql` (se a tabela CASO não existir) e `db/seed.sql` (se BAIRRO estiver vazia). Nas subidas seguintes não mexe nos dados. Os dois arquivos também podem ser executados à mão no SQL Developer; funcionam em Oracle 12c ou superior.

### O que foi verificado no banco, e o que é suposição

Verificado no **H2 em modo Oracle** (em memória e em arquivo), por teste automatizado e por execução do jar:

- `schema.sql` e `seed.sql` rodam na primeira subida.
- Segunda subida sobre um banco já criado não dá erro, não recria o schema e não apaga os dados (o app só cria o schema se a tabela CASO não existir e só semeia se BAIRRO estiver vazia).
- `POST /api/demo/reset` apaga as seis tabelas e recria o seed pela mesma rotina nas duas bases, em uma única transação.

**Suposição, nunca testada em Oracle real:**

- Os tipos (`NUMBER`, `VARCHAR2`, `TIMESTAMP`), as sequences `START WITH 1000 INCREMENT BY 1 NOCACHE` e as constraints `CHECK` aceitos pelo Oracle 12c ou superior. Não há `IDENTITY`: os ids vêm de sequences e do seed.
- As datas do seed no formato `LOCALTIMESTAMP - INTERVAL '2' DAY - INTERVAL '5' HOUR`.
- O Hibernate gerando `SEQ_CASO.nextval` e a leitura de `getSchema()` do driver `ojdbc11` para detectar a tabela.
- Banco em UTF-8 (por exemplo AL32UTF8), para os acentos do seed.
- Usuário com permissão de criar tabelas e sequences.
- Atomicidade do seed e do reset: o DML roda em transação, mas o DDL do Oracle confirma sozinho.

Se algo falhar no Oracle, execute `schema.sql` e `seed.sql` à mão no SQL Developer, na ordem, e suba o app: ele pula o que já existe.

## Testes

```bash
mvn test
```

52 testes: score, duplicata (Haversine, 50 m, 48 h), encaminhamento (3 mais próximos, vagas, tipos), transições de status e um teste de integração com MockMvc do fluxo completo (reportar, priorizar, encaminhar, aceitar, atender e acompanhar), mais recusa, privacidade e permissões. As regras são lidas do `application.yml` real.

## Telas

| Tela | Quem | O que faz |
|---|---|---|
| `index.html` | cidadão, sem login | Reporta com foto, pino no mapa (ou geolocalização), tipo, quantidade, descrição e contato opcional. Mostra o código. |
| `acompanhar.html` | cidadão | Busca pelo código e mostra a linha do tempo. |
| `dados.html` | público | Densidade agregada por célula e por bairro. |
| `entrar.html` | todos | "Entrar como" com os usuários do seed, sem senha. Também reinicia a demonstração. |
| `painel.html` | OPERADOR | Fila por score com filtros, mapa de casos e células críticas, detalhe com reportes, foto, "por que esta prioridade", 3 recursos sugeridos, encaminhar e arquivar, badge de alertas. |
| `recurso.html` | AGENTE | Casos encaminhados ao recurso: aceitar, recusar e marcar atendido, sempre com observação. |

Os usuários do seed: Carlos Almeida (operador), Juliana Rocha (agente da Clínica Veterinária Jardim Sul) e Paulo Mendes (agente do Associação Abrigo Central).

## Cobertura dos casos de uso

A lista oficial UC01 a UC07 não foi informada ao autor desta seção, então os nomes abaixo são **descrições do que o código faz**, na ordem em que o enunciado trata os fluxos (UC01, 02, 05 e 06 funcionais; UC03, 04 e 07 protótipos). Confira os nomes com a lista do grupo antes de entregar.

| UC | Cobertura | Telas | Endpoints reais |
|---|---|---|---|
| UC01 (supõe-se: reportar animal) | Funcional | `index.html` | `POST /api/reportes` |
| UC02 (supõe-se: acompanhar caso) | Funcional | `acompanhar.html` | `GET /api/acompanhar/{codigo}` |
| UC05 (supõe-se: triagem e encaminhamento pela central) | Funcional | `painel.html` | `GET /api/casos`, `GET /api/casos/{id}`, `POST /api/casos/{id}/encaminhar`, `POST /api/casos/{id}/arquivar`, `GET /api/alertas` |
| UC06 (supõe-se: recurso parceiro atende o caso) | Funcional | `recurso.html` | `GET /api/recurso`, `GET /api/recurso/casos`, `POST /api/recurso/casos/{id}/aceitar`, `.../recusar`, `.../atender` |
| UC03 (arrecadação) | Protótipo estático, dados fictícios | `arrecadacao.html` | nenhum |
| UC04 (lar temporário) | Protótipo estático, dados fictícios | `lar-temporario.html` | nenhum |
| UC07 (painel do gestor) | Protótipo, só números agregados existentes | `gestor.html` | lê `GET /api/dados/resumo` e `GET /api/dados/bairros` (já existentes, públicos) |

Apoio: `dados.html` (`/api/dados/celulas`, `/api/dados/bairros`, `/api/dados/resumo`), `entrar.html` (`/api/sessao`, `/api/usuarios`, `/api/demo/reset`).

**Lacunas conhecidas, sem funcionalidade inventada:**

- Nenhuma mensagem sai do sistema. Encaminhar e alertar apenas registram e exibem dentro do C-PET; as telas dizem "simulado na demonstração".
- Não há notificação ao cidadão quando o status muda: ele precisa consultar pelo código.
- Não há cadastro de recursos nem edição de vagas pela interface (vêm do seed).
- UC03, UC04 e UC07 não têm persistência nem ações.

**Roadmap fase 2 (não implementado):** padrinho recorrente, notificação de proximidade e reativação de usuário (CRM).

## Regras (todas em `application.yml`, bloco `cpet.regras`)

- **Duplicata:** reporte do mesmo tipo a até 50 m de um caso aberto criado nas últimas 48 h vira novo REPORTE do caso existente.
- **Score (0 a 100):** urgência do tipo (FERIDO 40, DOENTE 30, FILHOTE, MAUS_TRATOS e RISCO_SANITARIO 25, COLONIA 15) + densidade da célula (3 por caso aberto, teto 25) + vulnerabilidade do bairro (0 a 20) + envelhecimento (2 por dia, teto 15). A API devolve o detalhamento por fator.
- **Encaminhamento:** tabela tipo do caso para tipo de recurso, só recursos ativos, com vagas e que aceitem o tipo. Devolve os 3 mais próximos com a distância em km. Encaminhar desconta uma vaga; atender, recusar ou arquivar devolve.
- **Alertas:** `GET /api/alertas` traz os casos abertos atualizados nas últimas 24 h e as células com 5 ou mais casos abertos.
- **Status:** REPORTADO, TRIAGEM, ENCAMINHADO e depois ATENDIDO ou ARQUIVADO. A recusa do agente volta para TRIAGEM. Toda transição é validada e gravada em `HISTORICO_STATUS`.

## Privacidade

- Endpoints públicos devolvem só agregados por célula (coordenadas arredondadas a 2 casas) e o andamento de um código, sem observações internas.
- Coordenada exata, contato e fotos enviadas por cidadãos só para usuário autenticado. As 6 imagens do seed são ilustrações geradas, sem pessoas, e ficam em `static/seed-fotos`, abertas por serem simuladas.
- Um teste automatizado (`PrivacidadeTest`) garante que nenhum endpoint público devolve coordenada exata, contato, descrição, nome de agente ou caminho de foto.
- Upload de JPG ou PNG até 5 MB: o formato é conferido pelos bytes, a imagem é recodificada (o que remove o EXIF) e salva com nome UUID em `./uploads`.
- O formulário mostra o aviso de consentimento ao lado do campo de contato.

## Dados simulados

O seed traz 6 bairros de São Paulo, 8 recursos fictícios, 40 casos com concentração em Capão Redondo e Brasilândia, e 3 usuários, além de 6 ilustrações associadas a casos abertos. Cerca de 5 casos abertos aparecem como atualizados nas últimas 24 h. Tudo vem com `simulado = 1` e a interface exibe a faixa "Dados simulados para demonstração".

`POST /api/demo/reset` apaga tudo e recria o seed (há um botão em `entrar.html`). Para regerar o `seed.sql` com datas novas: `python -X utf8 tools/gerar_seed.py`.

## Antes de gravar

- [ ] **Subir e esperar.** Depois de iniciar o app, espere o log `Started CpetApplication` e abra a tela `entrar.html` antes de gravar. Isso deixa o servidor aquecido e confirma que a lista de usuários carrega.
- [ ] **Reiniciar os dados simulados.** Abra `entrar.html` e clique em "Reiniciar dados simulados". Isso apaga o que foi criado nos ensaios e recarrega os 40 casos, com as datas recalculadas para agora. Faça isso logo antes de gravar: os alertas dependem das últimas 24 horas.
- [ ] **Internet ligada.** Os tiles do mapa vêm do OpenStreetMap. Sem rede, os pinos aparecem sobre fundo liso.
- [ ] **Porta e URL.** O app responde em `http://localhost:8080` (ou na porta que você passou em `--server.port`). Abra sempre pela mesma URL, porque a sessão e o código guardado ficam atrelados ao endereço.
- [ ] **Perfil do navegador limpo.** Use uma janela anônima ou um perfil novo, sem extensões, em zoom 100%. Isso evita cache de JavaScript antigo e o código de reportes de ensaio na tela Acompanhar.
- [ ] **Foto de ensaio pronta.** Use uma das ilustrações de `src/main/resources/static/seed-fotos` (por exemplo `ferido.jpg`). São JPG, sem pessoas, bem abaixo de 5 MB.
- [ ] **Apagar fotos de ensaio.** Remova a pasta `uploads/` do projeto, se existir, para não sobrar foto de teste.
- [ ] **Rodar `mvn test`** uma vez antes de gravar. Se algo falhar, não grave.

## Roteiro de demonstração (cerca de 3 minutos de tela)

Os números abaixo valem para o seed recém-reiniciado e um reporte dentro da célula de Capão Redondo.

1. **Reportar com foto (0:00 a 0:40).** Em `index.html`, dê zoom em Capão Redondo e toque no mapa (ou arraste o pino). Escolha "Animal ferido", escreva uma linha, clique em "Anexar foto" e escolha a imagem, preencha um contato e envie. A tela mostra o código em destaque e avisa que ele fica guardado neste aparelho.
2. **Acompanhar (0:40 a 0:55).** Clique em "Acompanhar este caso": status Reportado, com as próximas etapas em cinza.
3. **Painel e prioridade (0:55 a 1:50).** Em `entrar.html`, entre como Carlos Almeida.
   - A fila vem ordenada por score. O badge de Alertas mostra 8: o caso novo, 5 casos abertos atualizados nas últimas 24 h e 2 células críticas.
   - Abra os Alertas: células de Capão Redondo (8 abertos) e Brasilândia (6), com o mapa mostrando as duas em tracejado.
   - Abra o caso novo. Em "Por que esta prioridade", leia os quatro fatores (urgência 40, densidade 24, vulnerabilidade 18, tempo em aberto 0, total 82). Mostre a foto e o contato.
4. **Encaminhar (1:50 a 2:10).** Entre os 3 recursos sugeridos, com distância em km e vagas, clique em "Encaminhar" na Clínica Veterinária Jardim Sul. O status vira Encaminhado.
5. **Agente atualiza (2:10 a 2:50).** Clique em Sair e entre como Juliana Rocha.
   - O caso novo está no topo, "Aguardando aceite", e o topo da tela diz "2 aguardando seu aceite". As vagas caíram para 1.
   - Clique em "Aceitar caso". Depois em "Marcar atendido", tente confirmar em branco (a tela exige a observação), escreva algo e registre. As vagas voltam a 2.
6. **Acompanhar de novo (2:50 a 3:05).** Abra `acompanhar.html`: o código já vem preenchido, o status é Atendido e a linha do tempo tem as quatro etapas.
7. **Alertas e dados abertos (3:05 a 3:30).** Volte ao painel como Carlos Almeida: o badge caiu para 7, porque o caso atendido saiu dos alertas. Abra `dados.html`, filtre por "Colônia de animais" ou "Risco sanitário" e mostre as bolhas numeradas, o ranking e a frase de que nada ali traz endereço exato.

Se sobrar tempo, repita o reporte no mesmo ponto: a tela diz que já havia um reporte e devolve o mesmo código (regra de duplicata).

Se algo sair do esperado durante a gravação, volte a `entrar.html`, reinicie os dados simulados e comece de novo.

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
