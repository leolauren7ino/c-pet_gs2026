# Decisões do MVP

Onde o enunciado deixou uma escolha em aberto, ficou a opção mais simples. Cada item abaixo diz o que foi escolhido e por quê.

## Stack e ambiente

- **Lombok** (`@Getter`, `@Setter`, `@Data`) nas entidades e nas propriedades. Corta código repetitivo e não aparece em runtime. Ele é só dependência de compilação, então o jar final não o carrega.
- **Java 17 como alvo** (`maven.compiler.release=17`). O build local usou o JDK 21, que compila para 17 sem problema.
- **Repositórios aninhados** na classe `Repositorios` com `@EnableJpaRepositories(considerNestedRepositories = true)`, para manter os seis repositórios num arquivo só.
- **Entidades com ids numéricos** (`Long`) em vez de relacionamentos JPA. As chaves estrangeiras existem no banco (`schema.sql`), mas o código só usa o id. Menos mapeamento, menos efeito colateral.
- **Sequences de 1000 em diante** para CASO, REPORTE e HISTORICO_STATUS. O seed usa ids de 1 a 999, então o reset nunca colide com ids novos. Vale igual para Oracle e H2.
- **Datas do seed relativas a `LOCALTIMESTAMP`** (`LOCALTIMESTAMP - INTERVAL '3' DAY - INTERVAL '5' HOUR`). Assim cada reset deixa os dados com cara de "hoje". Os intervalos de dia e hora ficam abaixo de 100, o limite padrão do Oracle.
- **`seed.sql` gerado** por `tools/gerar_seed.py` (Python 3, sem dependências). O arquivo gerado é versionado, então rodar o app não exige Python.
- **Banco criado na subida**, antes de o servidor abrir a porta (`SmartInitializingSingleton`, para nenhuma requisição ver o banco vazio): `BancoService` roda `schema.sql` se a tabela CASO não existir e `seed.sql` se BAIRRO estiver vazia, o seed em uma transação. Sem `ddl-auto`, sem `spring.sql.init`, que quebraria numa segunda subida em Oracle.
- **Reset** (`POST /api/demo/reset`) apaga as tabelas em ordem de FK e reexecuta o `seed.sql`. Pode ser desligado com `cpet.demo.reset-habilitado=false`.
- **Perfil padrão h2** via `spring.profiles.default`. Só vale quando nenhum perfil é informado.

## Regras

- **Score recalculado a cada leitura** para casos abertos, porque o envelhecimento muda com o tempo. A coluna `score` guarda o último valor calculado. Casos fechados congelam o envelhecimento em `atualizado_em`.
- **Densidade** conta os casos abertos da célula, incluindo o próprio caso.
- **Dias em aberto** são dias completos (24 h), arredondados para baixo.
- **Recursos elegíveis**: além da tabela tipo do caso para tipo de recurso, o recurso precisa listar o tipo do caso em `tipos_aceitos`. Sem isso o campo não serviria para nada.
- **Distância** das sugestões é arredondada para 0,1 km e desempata pelo nome.
- **Duplicata** soma o novo REPORTE ao caso existente, atualiza `atualizado_em`, mantém o maior `qtd_animais` e não cria entrada no histórico. O cidadão recebe o código do caso original e a resposta informa `duplicata: true`.
- **Bairro do caso** é o bairro cujo ponto central está mais próximo da coordenada.
- **Código de acompanhamento**: `CP-` e 6 caracteres de um alfabeto sem 0, O, 1, I e L.
- **Triagem implícita**: encaminhar um caso REPORTADO passa antes por TRIAGEM e grava as duas transições no histórico. Evita um clique a mais no painel.
- **Aceite** do agente não muda o status. Fica registrado no histórico como ENCAMINHADO para ENCAMINHADO (autor: o recurso). O painel do recurso mostra "aceito" a partir daí.
- **Recusa e atendimento exigem observação.** Recusa devolve a vaga, limpa `recurso_id` e volta o caso para TRIAGEM. Atendimento devolve a vaga e mantém `recurso_id` como registro.
- **Arquivar** um caso ENCAMINHADO também devolve a vaga.
- **Alertas** (regra alterada): `GET /api/alertas` devolve só casos ainda abertos (REPORTADO, TRIAGEM ou ENCAMINHADO) com atualização nas últimas 24 h, mais as células com 5 ou mais casos abertos. Casos ATENDIDO e ARQUIVADO saem da lista mesmo que tenham sido atualizados agora. O badge do painel soma as duas listas. Antes, a lista incluía qualquer status e o número ficava alto demais.
- **Casos novos** nascem com `simulado = 0`. A faixa "Dados simulados para demonstração" aparece enquanto existir qualquer caso simulado.

## Privacidade e acesso

- **"Entrar como" sem senha**: simplificação de MVP. A tela lista os usuários do seed e abre uma sessão HTTP. Não serve para produção; o próximo passo seria OAuth2 ou SSO da prefeitura.
- Endpoints públicos: `POST /api/reportes`, `GET /api/acompanhar/{codigo}`, `GET /api/dados/*`, `GET /api/usuarios`, `GET /api/info`. Nenhum devolve coordenada exata nem contato. A linha do tempo pública mostra só o status e a hora, sem as observações internas.
- **Teste de privacidade** (`PrivacidadeTest`): cria um reporte com contato, descrição, foto e coordenada de 5 casas e confere que nenhum endpoint público (dados abertos, informações gerais e acompanhamento de todos os 40 códigos do seed mais o novo) devolve esses valores, as coordenadas exatas dos casos, nomes de agentes e recursos, nem caminho de foto. Também varre o JSON atrás de campos proibidos e de `lat`/`lng` com mais de 2 casas. Rodei o teste com o arredondamento desligado para ver que ele falha.
- O centro de bairro exposto em `/api/dados/bairros` passou a sair com 2 casas (antes saía com até 4). Não é o ponto de um caso, mas o contrato público fica uniforme.
- **Fotos do seed** são ilustrações geradas (`tools/GerarFotos.java`) e ficam em `static/seed-fotos`, acessíveis sem login por serem simuladas. Fotos enviadas por cidadãos continuam só em `/api/fotos/{uuid}`, com login.
- `GET /api/usuarios` é público de propósito, para a tela "Entrar como" do MVP (lista nomes e o recurso de cada usuário do seed).
- Células públicas usam a coordenada arredondada a 2 casas (cerca de 1 km), a mesma gravada em `caso.celula`.
- **Fotos** só saem em `/api/fotos/{nome}` para usuário autenticado. O nome é validado contra o padrão UUID, o que impede path traversal.
- **Upload**: o tipo vem dos bytes (cabeçalho JPG ou PNG), não do nome nem do `Content-Type`. A imagem é decodificada e gravada de novo com `ImageIO`, o que descarta EXIF e outros metadados. Limite de 5 MB.

## Interface

- **Mapa**: Leaflet servido de `static/vendor/leaflet`, com tiles do OpenStreetMap. Os tiles precisam de internet; sem rede o mapa abre com fundo liso, e os pinos e células continuam aparecendo.
- **Fontes locais** em `static/fonts` (Schibsted Grotesk e IBM Plex Mono, licença OFL). Bootstrap 5 local, só para grade e formulários.
- **Skill de design**: foi seguido nas regras de gosto (copy direta, sem emoji nem degradê, tokens, hierarquia clara). As partes de landing page (hero dealt, gate de checagem, fotos de banco) não se aplicam a uma ferramenta de trabalho e ficaram de fora.

## Ajustes depois do teste com três perfis de uso

Percorri as telas como uma moradora de 55 anos no celular, um voluntário de ONG com pressa e um servidor de zoonoses. Os cinco problemas mais graves para a demonstração foram corrigidos sem tocar nas regras de negócio:

1. **Erro do formulário invisível.** No celular o botão Enviar ficava a 2098 px do topo e os avisos a 815 px e 1333 px; ao enviar incompleto nada mudava na tela. Agora a página rola até o primeiro aviso e uma mensagem junto ao botão diz o que falta.
2. **Primeiro passo confuso.** "Usar minha localização" virou o botão principal, acima do mapa, e a confirmação do pino deixou de mostrar coordenadas.
3. **Código sem recuperação.** O código do reporte é guardado no `localStorage` do aparelho, e a tela Acompanhar o oferece e já abre com o mais recente. Nada vai para o servidor; quem limpar os dados do navegador perde a lista.
4. **Dados abertos sem tipo.** O agregado por célula e por bairro ganhou `abertosPorTipo` (só contagens), com filtro por tipo, números dentro dos círculos, balão ao toque e ranking das células com mais casos. Continua sem coordenada exata.
5. **Fila do agente.** Casos aguardando aceite vêm primeiro, o topo mostra quantos aguardam, o contato vira link de e-mail ou telefone e o ponto tem botão "Ver o ponto no mapa".

Ficaram de fora: o badge de alertas conta todos os casos atualizados em 24 h (inclusive atendidos), então o número no seed é alto; e o campo de foto usa o texto do navegador, que muda com o idioma dele.
