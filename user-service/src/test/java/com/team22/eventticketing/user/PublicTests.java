package com.team22.eventticketing.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team22.eventticketing.user.config.JwtConfigurationManager;
import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PublicTests {

    @Autowired private WebApplicationContext wac;
    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JwtConfigurationManager jwtConfig = JwtConfigurationManager.getInstance();

    private static final String REGISTER_PATH    = "/api/auth/register";
    private static final String LOGIN_PATH       = "/api/auth/login";
    private static final String PROTECTED_PATH   = "/api/users";

    private static final String ADMIN_EMAIL    = "pub-admin-fixture@test.local";
    private static final String ADMIN_PASSWORD = "adminPass123";
    private Long adminUserId;
    private String cachedAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        // Create a real ADMIN user in DB once per test (idempotent)
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            User admin = new User("Test Admin", ADMIN_EMAIL,
                    passwordEncoder.encode(ADMIN_PASSWORD), "01999999999", Role.ADMIN);
            admin.setStatus(com.team22.eventticketing.user.entity.UserStatus.ACTIVE);
            userRepository.save(admin);
        }
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        adminUserId = admin.getId();
        cachedAdminToken = Jwts.builder()
                .subject(ADMIN_EMAIL)
                .claim("uid", adminUserId)
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirationMs()))
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }

    @AfterEach
    void tearDownAdmin() {
        userRepository.findByEmail(ADMIN_EMAIL).map(User::getId).ifPresent(userRepository::deleteById);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String email() { return "pub-" + UUID.randomUUID() + "@test.local"; }
    private static String phone() { return "01" + (100_000_000 + (int)(Math.random() * 800_000_000)); }

    private Map<String, Object> regBody(String email, String password) {
        return Map.of("name", "Test User", "email", email, "password", password, "phone", phone());
    }

    private String post(String path, Object body) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
    }

    private MvcResult postRaw(String path, Object body) throws Exception {
        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(body)))
                .andReturn();
    }

    /** Register user, login, return JWT string. */
    private String registerAndGetToken(String email, String password) throws Exception {
        post(REGISTER_PATH, regBody(email, password));
        String resp = post(LOGIN_PATH, Map.of("email", email, "password", password));
        return mapper.readTree(resp).get("token").asText();
    }

    /** Extract uid from a raw register response body. */
    private Long uidFrom(String registerRespBody) throws Exception {
        String token = mapper.readTree(registerRespBody).get("token").asText();
        return Jwts.parser().verifyWith(jwtConfig.getSigningKey()).build()
                .parseSignedClaims(token).getPayload().get("uid", Long.class);
    }

    private String adminToken() { return cachedAdminToken; }

    private String userPath(Long id)     { return "/api/users/" + id; }
    private String activityPath(Long id) { return "/api/users/" + id + "/activity"; }

    private String tamperedSignature(String token) {
        String[] p = token.split("\\.");
        char[] sig = p[2].toCharArray();
        sig[0] = (sig[0] == 'A') ? 'B' : 'A';
        return p[0] + "." + p[1] + "." + new String(sig);
    }

    private String forgedRolePayload(String token) {
        String[] p = token.split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"h@h.com\",\"uid\":1,\"role\":\"ADMIN\"}".getBytes());
        return p[0] + "." + fakePayload + "." + p[2];
    }

    private void deleteIfExists(Long id) {
        if (id != null && userRepository.existsById(id)) userRepository.deleteById(id);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC01 — Register happy path returns 2xx + JWT with expiresIn
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC01 — RegisterHappyPathTests")
    void tc01_registerHappyPath() throws Exception {
        String email = email();
        Long uid = null;
        try {
            MvcResult result = postRaw(REGISTER_PATH, regBody(email, "pass123"));
            Assertions.assertTrue(result.getResponse().getStatus() >= 200
                    && result.getResponse().getStatus() < 300,
                    "TC01: register response must be 2xx; got " + result.getResponse().getStatus());
            String body = result.getResponse().getContentAsString();
            Assertions.assertNotNull(mapper.readTree(body).get("token"),
                    "TC01: register response must include 'token' field per spec; body=" + body);
            String token = mapper.readTree(body).get("token").asText();
            Assertions.assertFalse(token.isBlank(),
                    "TC01: 'token' must be a non-blank string; got " + token);
            Assertions.assertTrue(mapper.readTree(body).get("expiresIn").asLong() > 0,
                    "TC01: register response must include positive 'expiresIn' per spec; body=" + body);
            uid = uidFrom(body);
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC02 — Login happy path returns 2xx + 3-segment JWT
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC02 — LoginHappyPathTests")
    void tc02_loginHappyPath() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            MvcResult result = postRaw(LOGIN_PATH, Map.of("email", email, "password", "pass123"));
            Assertions.assertTrue(result.getResponse().getStatus() >= 200
                    && result.getResponse().getStatus() < 300,
                    "TC02: login must return 2xx");
            String body = result.getResponse().getContentAsString();
            String token = mapper.readTree(body).get("token").asText();
            Assertions.assertFalse(token.isBlank(), "TC02: 'token' must be a non-blank string");
            Assertions.assertEquals(3, token.split("\\.").length,
                    "TC02: 'token' must be a 3-segment JWT (a.b.c); got '" + token + "'");
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC03 — GET own user with valid JWT returns 2xx + JSON object
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC03 — ReadOwnProfileHappyPathTests")
    void tc03_readOwnProfile() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = registerAndGetToken(email.replace("pub-", "tok-"), "pass123");
            // use the uid user's token (registered above) — simpler: re-login
            String loginResp = post(LOGIN_PATH, Map.of("email", email, "password", "pass123"));
            String token = mapper.readTree(loginResp).get("token").asText();
            mockMvc.perform(get(userPath(uid)).header("Authorization", "Bearer " + token))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$").isMap());
        } finally {
            deleteIfExists(uid);
            // clean up the second user created by registerAndGetToken
            userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().startsWith("tok-"))
                .map(User::getId).forEach(this::deleteIfExists);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC04 — Duplicate email returns 4xx (not 401 or 403)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC04 — RegisterDuplicateEmailTests")
    void tc04_duplicateEmail() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            MvcResult result = postRaw(REGISTER_PATH, regBody(email, "other"));
            int status = result.getResponse().getStatus();
            Assertions.assertTrue(status >= 400 && status < 500,
                    "TC04: duplicate-email register must return a 4xx client error; got " + status);
            Assertions.assertNotEquals(401, status, "401 indicates misclassified error");
            Assertions.assertNotEquals(403, status, "403 indicates misclassified error");
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC05 — Wrong password returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC05 — LoginWrongPasswordTests")
    void tc05_wrongPassword() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "correct")));
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("email", email, "password", "WRONG"))))
                    .andExpect(status().isUnauthorized());
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC06 — Valid admin JWT accepted on protected endpoint
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC06 — AuthValidTokenAcceptedTests")
    void tc06_validAdminToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().is2xxSuccessful());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC07 — Missing Authorization header returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC07 — AuthMissingHeaderTests")
    void tc07_missingAuthHeader() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC08 — Tampered-signature JWT returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC08 — AuthTamperedSignatureTests")
    void tc08_tamperedSignature() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)
                        .header("Authorization", "Bearer " + tamperedSignature(adminToken())))
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC09 — Login with unknown email returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC09 — LoginUnknownEmailTests")
    void tc09_unknownEmail() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                Map.of("email", "nobody-" + UUID.randomUUID() + "@no.test", "password", "x"))))
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC10 — Empty Bearer token returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC10 — AuthEmptyBearerTests")
    void tc10_emptyBearer() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC11 — Basic scheme returns strictly 401 (not 5xx)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC11 — AuthBasicSchemeTests")
    void tc11_basicScheme() throws Exception {
        String creds = Base64.getEncoder().encodeToString("user:pass".getBytes());
        MvcResult result = mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Basic " + creds))
                .andReturn();
        int status = result.getResponse().getStatus();
        Assertions.assertFalse(status >= 500, "TC11: Basic-scheme auth must NOT 5xx (status " + status + ")");
        Assertions.assertEquals(401, status, "TC11: Basic-scheme auth must return strictly 401; got " + status);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC12 — Garbage non-JWT token returns strictly 401
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC12 — AuthGarbageTokenTests")
    void tc12_garbageToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer not_a_valid_jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC13 — Forged role claim (payload tampered post-signing) is rejected
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC13 — AuthForgedRoleClaimTests")
    void tc13_forgedRoleClaim() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String loginResp = post(LOGIN_PATH, Map.of("email", email, "password", "pass123"));
            String jwt = mapper.readTree(loginResp).get("token").asText();
            String forged = forgedRolePayload(jwt);
            MvcResult result = mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + forged)).andReturn();
            Assertions.assertFalse(result.getResponse().getStatus() >= 200
                            && result.getResponse().getStatus() < 300,
                    "TC13: forged role payload must NOT be accepted; got " + result.getResponse().getStatus());
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC14 — Register with missing email field returns 4xx (not 5xx)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC14 — RegisterMissingFieldTests")
    void tc14_missingEmailField() throws Exception {
        MvcResult result = postRaw(REGISTER_PATH,
                Map.of("name", "No Email", "password", "pass123", "phone", phone()));
        int status = result.getResponse().getStatus();
        Assertions.assertFalse(status >= 500,
                "TC14: controller must not crash on missing email; body=" + result.getResponse().getContentAsString());
        Assertions.assertTrue(status >= 400 && status < 500,
                "TC14: register with missing email must return 4xx; got " + status);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC15 — Register with role:ADMIN in body must NOT yield an ADMIN account
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC15 — RegisterRoleEscalationTests")
    void tc15_roleEscalation() throws Exception {
        String email = email();
        Long uid = null;
        try {
            Map<String, Object> body = Map.of(
                    "name", "Hacker", "email", email, "password", "pass123", "phone", phone(), "role", "ADMIN");
            MvcResult result = postRaw(REGISTER_PATH, body);
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500,
                    "TC15: register with role=ADMIN must NOT 5xx; got " + status);
            if (status >= 200 && status < 300) {
                uid = uidFrom(result.getResponse().getContentAsString());
                User saved = userRepository.findById(uid).orElseThrow();
                Assertions.assertNotEquals(Role.ADMIN, saved.getRole(),
                        "TC15: controller mapped body role field into entity — privilege escalation bug");
            }
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC16 — Login with empty password returns NOT 2xx (and not 5xx)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC16 — LoginEmptyPasswordTests")
    void tc16_emptyPassword() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "realPass")));
            MvcResult result = postRaw(LOGIN_PATH, Map.of("email", email, "password", ""));
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500,
                    "TC16: must not NPE on empty password; body=" + result.getResponse().getContentAsString());
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC16: login with empty password must NOT return 2xx; got " + status);
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC17 — Cross-user IDOR read must NOT be 2xx
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC17 — IdorReadOtherUserTests")
    void tc17_idorRead() throws Exception {
        String emailA = email(), emailB = email();
        Long uidA = null, uidB = null;
        try {
            uidA = uidFrom(post(REGISTER_PATH, regBody(emailA, "passA")));
            uidB = uidFrom(post(REGISTER_PATH, regBody(emailB, "passB")));
            String tokenA = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", emailA, "password", "passA"))).get("token").asText();
            MvcResult result = mockMvc.perform(get(userPath(uidB)).header("Authorization", "Bearer " + tokenA)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500,
                    "TC17: must reject cleanly, not crash; body=" + result.getResponse().getContentAsString());
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC17: user A can read user B's profile — IDOR; body=" + result.getResponse().getContentAsString());
        } finally { deleteIfExists(uidA); deleteIfExists(uidB); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC18 — Cross-user IDOR update must NOT be 2xx AND B's data must not change
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC18 — IdorUpdateOtherUserTests")
    void tc18_idorUpdate() throws Exception {
        String emailA = email(), emailB = email();
        Long uidA = null, uidB = null;
        try {
            uidA = uidFrom(post(REGISTER_PATH, regBody(emailA, "passA")));
            uidB = uidFrom(post(REGISTER_PATH, regBody(emailB, "passB")));
            String tokenA = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", emailA, "password", "passA"))).get("token").asText();
            String originalName = userRepository.findById(uidB).orElseThrow().getName();

            User patch = new User();
            patch.setName("HACKED");
            MvcResult result = mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(userPath(uidB))
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(patch)))
                    .andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500, "TC18: cross-user update must NOT 5xx; status=" + status);
            Assertions.assertTrue(status == 403 || status == 404,
                    "TC18: cross-user update must return 403 or 404; got " + status);
            Assertions.assertEquals(originalName, userRepository.findById(uidB).orElseThrow().getName(),
                    "TC18: B's name changed — update committed before auth check");
        } finally { deleteIfExists(uidA); deleteIfExists(uidB); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC19 — Cross-user IDOR delete must NOT be 2xx AND B must still exist
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC19 — IdorDeleteOtherUserTests")
    void tc19_idorDelete() throws Exception {
        String emailA = email(), emailB = email();
        Long uidA = null, uidB = null;
        try {
            uidA = uidFrom(post(REGISTER_PATH, regBody(emailA, "passA")));
            uidB = uidFrom(post(REGISTER_PATH, regBody(emailB, "passB")));
            String tokenA = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", emailA, "password", "passA"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    delete(userPath(uidB)).header("Authorization", "Bearer " + tokenA)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500, "TC19: cross-user delete must NOT 5xx; status=" + status);
            Assertions.assertTrue(status == 403 || status == 404,
                    "TC19: cross-user delete must return 403 or 404; got " + status);
            Assertions.assertTrue(userRepository.existsById(uidB),
                    "TC19: user B was deleted — delete executed before auth check");
        } finally { deleteIfExists(uidA); deleteIfExists(uidB); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC20 — Owner can UPDATE own profile AND DB reflects new name
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC20 — OwnerUpdateOwnProfileTests")
    void tc20_ownerUpdateOwnProfile() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();

            User existing = userRepository.findById(uid).orElseThrow();
            existing.setName("Updated Name");
            existing.setCreatedAt(null);

            mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(userPath(uid))
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(existing)))
                    .andExpect(status().is2xxSuccessful());

            Assertions.assertEquals("Updated Name",
                    userRepository.findById(uid).orElseThrow().getName(),
                    "TC20: update not actually persisting");
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC21 — Admin can READ any user's record
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC21 — AdminReadAnyUserTests")
    void tc21_adminReadAnyUser() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            mockMvc.perform(get(userPath(uid)).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$").isMap());
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC22 — Admin can UPDATE any user AND DB reflects the change
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC22 — AdminUpdateAnyUserTests")
    void tc22_adminUpdateAnyUser() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            User existing = userRepository.findById(uid).orElseThrow();
            existing.setName("Admin Updated");
            existing.setCreatedAt(null);

            mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(userPath(uid))
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(existing)))
                    .andExpect(status().is2xxSuccessful());

            Assertions.assertEquals("Admin Updated",
                    userRepository.findById(uid).orElseThrow().getName(),
                    "TC22: admin update did not persist");
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC23 — Admin DELETE hard-deletes user AND GET returns 404 after
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC23 — AdminDeleteAnyUserTests")
    void tc23_adminDeleteAnyUser() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            mockMvc.perform(delete(userPath(uid)).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().is2xxSuccessful());
            Assertions.assertFalse(userRepository.existsById(uid),
                    "TC23: user still in DB after admin DELETE — must hard-delete");
            mockMvc.perform(get(userPath(uid)).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNotFound());
            uid = null; // already gone
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC24 — Owner GET own activity returns 2xx + paginated envelope
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC24 — ActivityOwnerHappyPathTests")
    void tc24_ownerGetOwnActivity() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            mockMvc.perform(get(activityPath(uid)).header("Authorization", "Bearer " + jwt))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").exists())
                    .andExpect(jsonPath("$.size").exists())
                    .andExpect(jsonPath("$.totalElements").exists());
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC25 — Non-existent user ID returns strictly 404 (admin token)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC25 — ActivityNonExistentIdTests")
    void tc25_activityNonExistentId() throws Exception {
        mockMvc.perform(get(activityPath(Long.MAX_VALUE)).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC26 — Negative user ID returns 4xx (admin token)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC26 — ActivityNegativeIdTests")
    void tc26_activityNegativeId() throws Exception {
        MvcResult result = mockMvc.perform(
                get(activityPath(-1L)).header("Authorization", "Bearer " + adminToken())).andReturn();
        int status = result.getResponse().getStatus();
        Assertions.assertTrue(status >= 400 && status < 500,
                "TC26: negative user ID must return 4xx; got " + status);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC27 — String user ID returns 4xx (path-var binding fails)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC27 — ActivityStringIdTests")
    void tc27_activityStringId() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/users/abc/activity").header("Authorization", "Bearer " + adminToken())).andReturn();
        int status = result.getResponse().getStatus();
        Assertions.assertTrue(status >= 400 && status < 500,
                "TC27: string user ID must return 4xx (Bad Request); got " + status);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC28 — size=0 must NOT 5xx
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC28 — ActivitySizeZeroTests")
    void tc28_activitySizeZero() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uid)).param("size", "0").header("Authorization", "Bearer " + jwt)).andReturn();
            Assertions.assertFalse(result.getResponse().getStatus() >= 500,
                    "TC28: size=0 must NOT 5xx");
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC29 — size=-1 returns 4xx
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC29 — ActivityNegativeSizeTests")
    void tc29_activityNegativeSize() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uid)).param("size", "-1").header("Authorization", "Bearer " + jwt)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500, "TC29: size=-1 must NOT 5xx; status=" + status);
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC29: size=-1 must NOT be 2xx; status=" + status);
            Assertions.assertTrue(status >= 400 && status < 500,
                    "TC29: size=-1 must return 4xx; got " + status);
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC30 — size=abc returns 4xx (Integer binding fails)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC30 — ActivityStringSizeTests")
    void tc30_activityStringSize() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uid)).param("size", "abc").header("Authorization", "Bearer " + jwt)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC30: size=abc must NOT be 2xx; status=" + status);
            Assertions.assertTrue(status >= 400 && status < 500,
                    "TC30: size=abc must return 4xx; got " + status);
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC31 — Cross-user activity (regular user) returns strictly 403
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC31 — ActivityCrossUserRegularTests")
    void tc31_activityCrossUserRegular() throws Exception {
        String emailA = email(), emailB = email();
        Long uidA = null, uidB = null;
        try {
            uidA = uidFrom(post(REGISTER_PATH, regBody(emailA, "passA")));
            uidB = uidFrom(post(REGISTER_PATH, regBody(emailB, "passB")));
            String tokenA = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", emailA, "password", "passA"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uidB)).header("Authorization", "Bearer " + tokenA)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 500, "TC31: cross-user activity must NOT 5xx; status=" + status);
            Assertions.assertEquals(403, status,
                    "TC31: cross-user activity must return strictly 403; got " + status);
        } finally { deleteIfExists(uidA); deleteIfExists(uidB); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC32 — Admin GET another user's activity returns 2xx + content array
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC32 — ActivityCrossUserAdminTests")
    void tc32_activityCrossUserAdmin() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            mockMvc.perform(get(activityPath(uid)).header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.content").isArray());
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC33 — page=-1 returns 4xx
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC33 — ActivityNegativePageTests")
    void tc33_activityNegativePage() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uid)).param("page", "-1").header("Authorization", "Bearer " + jwt)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC33: page=-1 must NOT be 2xx; status=" + status);
            Assertions.assertTrue(status >= 400 && status < 500,
                    "TC33: page=-1 must return 4xx; got " + status);
        } finally { deleteIfExists(uid); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TC34 — page=abc returns 4xx (Integer binding fails)
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC34 — ActivityStringPageTests")
    void tc34_activityStringPage() throws Exception {
        String email = email();
        Long uid = null;
        try {
            uid = uidFrom(post(REGISTER_PATH, regBody(email, "pass123")));
            String jwt = mapper.readTree(post(LOGIN_PATH,
                    Map.of("email", email, "password", "pass123"))).get("token").asText();
            MvcResult result = mockMvc.perform(
                    get(activityPath(uid)).param("page", "abc").header("Authorization", "Bearer " + jwt)).andReturn();
            int status = result.getResponse().getStatus();
            Assertions.assertFalse(status >= 200 && status < 300,
                    "TC34: page=abc must NOT be 2xx; status=" + status);
            Assertions.assertTrue(status >= 400 && status < 500,
                    "TC34: page=abc must return 4xx; got " + status);
        } finally { deleteIfExists(uid); }
    }
}
