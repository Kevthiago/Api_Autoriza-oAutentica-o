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

📄 Exemplo de configuração `application.yml`:

```
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: umaChaveSecretaMuitoLongaEComplexaParaAssinarTokensJWT
  expiration: 3600000

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    disable-swagger-default-url: true
  api-docs:
    path: /v3/api-docs
```

⚠️ IMPORTANTE: Para produção, a chave secreta do JWT (jwt.secret) deve ser gerenciada de forma segura, como uma variável de ambiente ou por meio de um serviço de secrets, e não deixada diretamente no arquivo de configuração.

---

## 🛡️ Implementação da API de Autenticação
A lógica de autenticação está distribuída em várias classes, seguindo as melhores práticas de separação de responsabilidades:

✅ Entidade de Usuário
Define o modelo de dados do usuário.
Local: `src/main/java/com/example/Autenticacao/Autorizacao/model/User.java`

```
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;
}
```

✅ Repositório de Usuário
Interface para interagir com o banco de dados e buscar os usuários.
Local: `src/main/java/com/example/Autenticacao/Autorizacao/repository/UserRepository.java`

✅ Serviço de Geração e Validação de Tokens JWT
Componente central para criar e verificar os JWTs.
Local: `src/main/java/com/example/authserver/service/JwtService.java`

🔧 Método de geração do token JWT:

```
public String generateToken(String username, String role) {
    return JWT.create()
            .withSubject(username)
            .withClaim("role", role)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
            .sign(Algorithm.HMAC256(secretKey));
}
```

✅ Serviço de Autenticação
Contém a lógica de negócio para validar as credenciais do usuário e gerar um token.
Local: `src/main/java/com/example/Autenticacao/Autorizacao/service/AuthService.java`

✅ Controlador de Autenticação
Expõe os endpoints /auth/login e /auth/validate para o mundo externo.
Local: `src/main/java/com/example/Autenticacao/Autorizacao/controller/AuthController.java`

🚪 Endpoint de login com validação e emissão do token:

```
@Operation(summary = "Realiza o login do usuário e emite um token JWT")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido, retorna o token JWT"),
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
})
@PostMapping("/login")
public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
    try {
        String token = authService.authenticateUserAndGenerateToken(username, password);
        return ResponseEntity.ok(token);
    } catch (BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ocorreu um erro interno ao tentar logar.");
    }
}
```

🔑 Configuração do Spring Security para JWT
A configuração do Spring Security está definida para usar a chave secreta local para decodificar e validar os JWTs emitidos pela própria API.
As principais configurações estão em:

Local: `src/main/java/com/example/Autenticacao/Autorizacao/config/SecurityConfig.java`

🔐 Exemplo da configuração SecurityFilterChain:

```
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/login").permitAll()
            .requestMatchers("/auth/validate").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()
        )
        .headers(headers -> headers.frameOptions(frameOptions -> headers.frameOptions().sameOrigin()))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
            // O Spring usará o JwtDecoder definido como Bean
        }));

    return http.build();
}
```

Para demonstrar a proteção de endpoints, foi criado um controlador de teste:
src/main/java/com/example/Autenticacao/Autorizacao/controller/TestProtectedController.java
Ele contém um endpoint (/api/admin) que só pode ser acessado por usuários com a role ADMIN.

## ✅ Testes com JUnit
Testes de integração robustos foram escritos para validar todo o fluxo de autenticação, a geração de tokens e a proteção de endpoints.
Você pode encontrar os casos de teste em:

Local: `src/test/java/com/example/Autenticacao/Autorizacao/AuthIntegrationTests.java`

## 📈 Testes de Carga com JMeter
O JMeter é essencial para avaliar o desempenho da sua API sob estresse.

Instalação do JMeter
- Baixar: Acesse Apache JMeter Downloads
- Extrair: Descompacte o arquivo.
- Executar: No diretório bin, execute jmeter.bat (Windows) ou jmeter.sh (Linux/macOS).

Executando o Plano de Teste
Este repositório inclui um plano de teste de carga pré-configurado:

- Arquivo: Login_teste_stress.jmx
- Local: pasta `jmeter_tests`

Para usá-lo:

- Abra o JMeter.
- Vá em File > Open e selecione o arquivo Login_teste_stress.jmx.
- Com a aplicação rodando localmente, clique no botão verde Start no JMeter para iniciar o teste.
- Analise os resultados nos listeners View Results Tree e Summary Report para verificar:
  * Tempo de resposta  
  * Taxa de erros  
  * Vazão da API


## 📖 Documentação com Swagger / OpenAPI
A API possui documentação interativa gerada automaticamente pelo Springdoc/OpenAPI.
A configuração personalizada (título, descrição, esquema de segurança) está em:

Local: `src/main/java/com/example/Autenticacao/Autorizacao/config/SwaggerConfig.java`

🧾 Configuração do Swagger com suporte ao esquema de segurança:

```
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API de Autenticação JWT Interna")
                .version("1.0.0")
                .description("API para autenticação, geração e validação interna de tokens JWT. Ideal para microsserviços.")
                .termsOfService("http://swagger.io/terms/")
                .contact(new Contact().name("Seu Nome/Equipe").email("seu.email@example.com"))
                .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT para autenticação. Use: Authorization: Bearer <token>")));
    }
}
```

Para acessar a documentação e interagir com os endpoints:

- Inicie a aplicação
- Acesse: `http://localhost:8080/swagger-ui.html`
