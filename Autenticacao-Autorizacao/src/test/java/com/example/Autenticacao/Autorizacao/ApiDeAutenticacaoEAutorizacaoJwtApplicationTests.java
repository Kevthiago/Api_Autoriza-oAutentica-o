package com.example.Autenticacao.Autorizacao;

import com.example.Autenticacao.Autorizacao.repository.UserRepository;
import com.example.Autenticacao.Autorizacao.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiDeAutenticacaoEAutorizacaoJwtApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setup() {
		userRepository.findByUsername("admin").ifPresentOrElse(
				user -> { /* Já existe */ },
				() -> {
					com.example.Autenticacao.Autorizacao.model.User admin = new com.example.Autenticacao.Autorizacao.model.User(
							null, "admin", passwordEncoder.encode("123456"), "ROLE_ADMIN");
					userRepository.save(admin);
				});
		userRepository.findByUsername("user").ifPresentOrElse(
				user -> { /* Já existe */ },
				() -> {
					com.example.Autenticacao.Autorizacao.model.User regularUser = new com.example.Autenticacao.Autorizacao.model.User(
							null, "user", passwordEncoder.encode("password"), "ROLE_USER");
					userRepository.save(regularUser);
				});
	}

	// --- Testes originais usando tokens JWT reais ---

	@Test
	void testLoginSuccess() throws Exception {
		String token = mockMvc.perform(post("/auth/login")
				.param("username", "admin")
				.param("password", "123456")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andExpect(content().string(notNullValue()))
				.andReturn().getResponse().getContentAsString();

		assert jwtService.validateToken(token);
	}

	@Test
	void testLoginFailureInvalidPassword() throws Exception {
		mockMvc.perform(post("/auth/login")
				.param("username", "admin")
				.param("password", "senhaErrada")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(containsString("Senha incorreta.")));
	}

	@Test
	void testProtectedEndpointAccessDeniedWithoutToken() throws Exception {
		mockMvc.perform(get("/api/hello"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testProtectedEndpointAccessWithValidToken() throws Exception {
		String token = mockMvc.perform(post("/auth/login")
				.param("username", "user")
				.param("password", "password")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get("/api/hello")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string("Olá! Você acessou um endpoint protegido com sucesso!"));
	}

	@Test
	void testProtectedAdminEndpointAccessWithAdminToken() throws Exception {
		String adminToken = mockMvc.perform(post("/auth/login")
				.param("username", "admin")
				.param("password", "123456")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get("/api/admin")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(content().string("Bem-vindo, Administrador! Este é um recurso restrito."));
	}

	@Test
	void testProtectedAdminEndpointAccessDeniedWithUserToken() throws Exception {
		String userToken = mockMvc.perform(post("/auth/login")
				.param("username", "user")
				.param("password", "password")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get("/api/admin")
				.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());
	}

	// --- Testes usando @WithMockUser para mockar autenticação ---

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	void testProtectedAdminEndpointAccessWithMockAdmin() throws Exception {
		mockMvc.perform(get("/api/admin"))
				.andExpect(status().isOk())
				.andExpect(content().string("Bem-vindo, Administrador! Este é um recurso restrito."));
	}

	@Test
	@WithMockUser(username = "user", roles = {"USER"})
	void testProtectedAdminEndpointAccessDeniedWithMockUser() throws Exception {
		mockMvc.perform(get("/api/admin"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user", roles = {"USER"})
	void testProtectedHelloEndpointWithMockUser() throws Exception {
		mockMvc.perform(get("/api/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string("Olá! Você acessou um endpoint protegido com sucesso!"));
	}

	@Test
	void testProtectedEndpointAccessDeniedWithoutMockUser() throws Exception {
		mockMvc.perform(get("/api/hello"))
				.andExpect(status().isUnauthorized());
	}

}
