# 🚀 API de Autenticação e Autorização JWT (Emissão e Validação Interna)

Este roteiro te guiará na construção de uma API Spring Boot que é o coração da sua autenticação.  
Ela será responsável por gerar tokens JWT para usuários que fizerem login e por validar esses mesmos tokens para proteger seus próprios recursos.

Abordaremos as melhores práticas de **segurança**, **testabilidade** e **documentação**.

---

## 📦 Dependências do Projeto

Para começar, as dependências essenciais do projeto estão configuradas no arquivo `pom.xml`.  
As versões mais recentes do Spring Boot 3.x garantem que tudo funcione perfeitamente.  
As principais dependências incluem:

- `spring-boot-starter-web`: 🌐 Para construir as APIs RESTful.
- `spring-boot-starter-security`: 🔒 O core da segurança do Spring, para autenticação e autorização.
- `spring-boot-starter-oauth2-resource-server`: 🔑 Permite que a API valide tokens JWT, agindo como um Servidor de Recursos.
- `spring-boot-starter-data-jpa`: 🗄️ Para persistência de dados com JPA.
- `com.h2database:h2`: 💾 Banco de dados em memória, perfeito para desenvolvimento e testes.
- `com.auth0:java-jwt`: 🛡️ Biblioteca para gerar e validar JWTs programaticamente.
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`: 📚 Para gerar a documentação automática da API com Swagger UI.
- `org.springframework.boot:spring-boot-devtools`: 🛠️ Ferramentas que aceleram o desenvolvimento, como *hot reload*.
- `org.projectlombok:lombok`: 🍬 Reduz código repetitivo (getters, setters, etc.).
- `org.springframework.boot:spring-boot-starter-test`: ✅ Inclui JUnit 5 e Mockito, essenciais para testes.

---

## ⚙️ Configuração do Ambiente de Desenvolvimento

Toda a configuração da aplicação está centralizada no arquivo `src/main/resources/application.yml`.  
Este arquivo controla o comportamento da API, incluindo:

- Porta do servidor (padrão: `8080`);
- Configuração do banco de dados em memória H2;
- Propriedades do JWT, como a chave secreta e o tempo de expiração do token;
- Caminhos para a documentação Swagger/OpenAPI.

> ⚠️ **IMPORTANTE**: Para produção, a chave secreta do JWT (`jwt.secret`) deve ser gerenciada de forma segura, como uma variável de ambiente ou por meio de um serviço de *secrets*, e **não** deixada diretamente no arquivo de configuração.

---

## 🛡️ Implementação da API de Autenticação

A lógica de autenticação está distribuída em várias classes, seguindo as melhores práticas de separação de responsabilidades:

- **Entidade de Usuário**  
  Define o modelo de dados do usuário.  
  Local: `src/main/java/com/example/Autenticacao/Autorizacao/model/User.java`

- **Repositório de Usuário**  
  Interface para interagir com o banco de dados e buscar os usuários.  
  Local: `src/main/java/com/example/Autenticacao/Autorizacao/repository/UserRepository.java`

- **Serviço de Geração e Validação de Tokens JWT**  
  Componente central para criar e verificar os JWTs.  
  Local: `src/main/java/com/example/authserver/service/JwtService.java`

- **Serviço de Autenticação**  
  Contém a lógica de negócio para validar as credenciais do usuário e gerar um token.  
  Local: `src/main/java/com/example/Autenticacao/Autorizacao/service/AuthService.java`

- **Controlador de Autenticação**  
  Expõe os endpoints `/auth/login` e `/auth/validate` para o mundo externo.  
  Local: `src/main/java/com/example/Autenticacao/Autorizacao/controller/AuthController.java`

---

## 🔑 Configuração do Spring Security para JWT

A configuração do Spring Security está definida para usar a chave secreta local para decodificar e validar os JWTs emitidos pela própria API.  
As principais configurações estão em:

- `src/main/java/com/example/Autenticacao/Autorizacao/config/SecurityConfig.java`

Neste arquivo, são configurados:

- Quais endpoints são públicos (como `/auth/login`, `/h2-console/**` e a documentação Swagger);
- Que todas as outras requisições exigem um JWT válido;
- A desabilitação do CSRF e o uso de sessões *stateless*, ideal para APIs RESTful.

> Para demonstrar a proteção de endpoints, foi criado um controlador de teste:  
> `src/main/java/com/example/Autenticacao/Autorizacao/controller/TestProtectedController.java`  
> Ele contém um endpoint (`/api/admin`) que só pode ser acessado por usuários com a role `ADMIN`.

---

## ✅ Testes com JUnit

Testes de integração robustos foram escritos para validar todo o fluxo de autenticação, a geração de tokens e a proteção de endpoints.  
Você pode encontrar os casos de teste em:

- `src/test/java/com/example/Autenticacao/Autorizacao/AuthIntegrationTests.java`

---

## 📈 Testes de Carga com JMeter

O **JMeter** é essencial para avaliar o desempenho da sua API sob estresse.

### 6.1. Instalação do JMeter

1. **Baixar**: Acesse [Apache JMeter Downloads](https://jmeter.apache.org/download_jmeter.cgi)  
2. **Extrair**: Descompacte o arquivo.  
3. **Executar**: No diretório `bin`, execute `jmeter.bat` (Windows) ou `jmeter.sh` (Linux/macOS).

### 6.2. Executando o Plano de Teste

Este repositório inclui um plano de teste de carga pré-configurado:

- Arquivo: `Login_teste_stress.jmx`
- Local: pasta `jmeter_tests`

Para usá-lo:

1. Abra o JMeter.
2. Vá em **File > Open** e selecione o arquivo `Login_teste_stress.jmx`.
3. Com a aplicação rodando localmente, clique no botão verde **Start** no JMeter para iniciar o teste.
4. Analise os resultados nos listeners **View Results Tree** e **Summary Report** para verificar:
   - Tempo de resposta
   - Taxa de erros
   - Vazão da API

---

## 📖 Documentação com Swagger / OpenAPI

A API possui documentação interativa gerada automaticamente pelo Springdoc/OpenAPI.  
A configuração personalizada (título, descrição, esquema de segurança) está em:

- `src/main/java/com/example/Autenticacao/Autorizacao/config/SwaggerConfig.java`

Para acessar a documentação e interagir com os endpoints:

- Inicie a aplicação
- Acesse: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---
