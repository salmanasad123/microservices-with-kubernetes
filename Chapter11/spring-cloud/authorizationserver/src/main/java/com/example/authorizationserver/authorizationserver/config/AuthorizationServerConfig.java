package com.example.authorizationserver.authorizationserver.config;

import com.example.authorizationserver.authorizationserver.jose.Jwks;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** COMPLETE OATH FLOW:
 * Scenario: Using a Photo Edit App to Edit photos and that Photo Edit app requires access to my
 * Google Drive to get the photos:
 *
 * Roles in your scenario:
 * Resource Owner: You (Salman) – you own the photos on Google Drive
 * Client: PhotoApp – the application that wants to access your photos
 * Authorization Server: Google OAuth2 server – it authenticates you and gives tokens
 * Resource Server: Google Drive API – it hosts your photos and enforces token access
 *
 * So yes, your identification of roles is correct.
 *
 * OAuth 2.0 Authorization Flow
 * Step by step:
 *
 * Client Registration:
 * When: Before the user ever clicks the button, i.e., when the PhotoApp is being developed.
 * What happens: The PhotoApp registers itself with Google’s Authorization Server.
 * Google gives the PhotoApp a client_id and client_secret.
 * PhotoApp also provides redirect URIs (e.g., https://photoapp.com/oauth/callback) during registration.
 * This is exactly like your Spring Boot code, where writerClient has clientId, clientSecret, and redirectUris.
 *
 * User Clicks “Upload from Google Drive”
 * PhotoApp redirects the user to Google Authorization Server with query params:
 * https://accounts.google.com/o/oauth2/v2/auth?
 *   client_id=PHOTO_APP_CLIENT_ID
 *   &redirect_uri=https://photoapp.com/oauth/callback
 *   &response_type=code
 *   &scope=photos.read
 *   &state=xyz
 *
 * This step tells Google: “Hey, PhotoApp wants permission to access the user’s photos.”
 * User Logs In and Grants Consent
 * Google shows login page → you enter username/password.
 * Then Google asks: “Do you allow PhotoApp to access your photos?”
 * This is where the ClientSettings.requireAuthorizationConsent(true) in Spring code corresponds conceptually.
 *
 * Authorization Code Issued
 * After consent, Google redirects back to PhotoApp’s redirect URI with a code:
 * https://photoapp.com/oauth/callback?code=AUTH_CODE&state=xyz
 * This code is short-lived and can be exchanged for a token.
 * Token Exchange
 * PhotoApp backend sends POST request to Google’s token endpoint with:
 *
 * client_id=PHOTO_APP_CLIENT_ID
 * client_secret=PHOTO_APP_CLIENT_SECRET
 * code=AUTH_CODE
 * redirect_uri=https://photoapp.com/oauth/callback
 * grant_type=authorization_code
 *
 * Google responds with access_token (and optionally refresh_token) which PhotoApp uses to access Google Drive API.
 * Client Registration Details
 *
 * When does registration happen?
 * During app development (one-time). Not every user click.
 * What does the client provide?
 * redirect_uri (where Google should send the code after authorization).
 * Some basic info like app name, logo, website.
 *
 * What does Google provide back?
 * client_id → public identifier of PhotoApp.
 * client_secret → secret key to authenticate PhotoApp when exchanging code for token.
 * So yes, client is registered before user interaction, not dynamically per user.
 *
 * Where OpenID Connect (OIDC) fits
 * OAuth2 does not itself validate user identity; it only gives access to resources.
 * OpenID Connect adds identity layer on top of OAuth2.
 * With OIDC:
 * Scope openid is requested: .scope(OidcScopes.OPENID)
 * Authorization Server returns ID Token (JWT) that tells PhotoApp “This user is Salman, logged in successfully.”
 * This allows PhotoApp to authenticate the user, not just access resources.
 *
 * In your scenario:
 * If PhotoApp wants to know who you are (your identity, email, etc.), it uses OpenID Connect.
 * If PhotoApp only wants photos, OAuth2 scopes are enough.
 *
 * Key Mapping to your Spring Boot code
 * Spring Boot Code	Google Scenario
 * RegisteredClient:     	    PhotoApp registration with Google
 * clientId, clientSecret: 	    client_id and client_secret Google gives to PhotoApp
 * redirectUri:	                redirect URI Google uses to send auth code back
 * authorizationGrantType:  	flow types PhotoApp supports (AUTHORIZATION_CODE in this scenario)
 * scope(OidcScopes.OPENID):	requesting identity info from Google via OIDC
 * scope("product:read"):	    requesting access to Google Drive API (read photos)
 *
 *
 * Token Validation Phase — Resource Server side
 *
 * Now the user (Salman) ne PhotoApp ko access token mil gaya from Google Authorization Server.
 * Ab PhotoApp Google Drive API (Resource Server) ko hit karega to access your photos.
 *
 * Request Example:
 * GET https://www.googleapis.com/drive/v3/files
 * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6...
 *
 * Ab yahaan se token validation ka process start hota hai.
 * 🔹 Step 1: Resource Server receives request
 * Google Drive API (Resource Server) ko request milti hai header me access token ke sath.
 * 🔹 Step 2: Token validation options
 * Resource Server ke paas do options hote hain token verify karne ke liye:
 * Local Validation (Most common)
 * Resource Server auth-server ke jwk-set-uri (public key endpoint) se ek baar public key fetch karta hai.
 * Example config:
 * spring.security.oauth2.resourceserver.jwt.issuer-uri: https://accounts.google.com
 * OR
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri: https://www.googleapis.com/oauth2/v3/certs
 *
 * Jab first request aati hai, Spring Security automatically:
 * issuer-uri ya jwk-set-uri ke endpoint ko hit karta hai.
 * Public keys (JWKs) fetch karta hai aur local cache me store kar leta hai.
 * Us JWT token ko locally decode + signature verify karta hai using that public key.
 * Agar signature valid hai → token trusted hai.
 * Agar expire ya tampered hai → reject kar deta hai (401 Unauthorized).
 * 👉 So yahaan resource server token ko auth server ko bhejta nahi, balki khud verify karta hai using public key.
 * ⚙️ Internally kya hota hai (Spring Boot Resource Server me)
 * Spring Security JwtDecoder bean banata hai jo issuer-uri se JWKSet fetch karta hai:
 *
 * @Bean
 * public JwtDecoder jwtDecoder() {
 *     return JwtDecoders.fromIssuerLocation("https://accounts.google.com");
 * }
 *
 * Aur phir jab request aati hai:
 * JWT decode hota hai,
 * Signature verify hoti hai using RSA public key,
 * Claims (sub, iss, exp, aud, scope, etc.) extract hote hain.
 * Agar sab valid ho to request allow hoti hai aur security context me user set ho jata hai.
 */

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Ye method ek Spring Security Filter Chain bana raha hai specifically Authorization Server ke
     * endpoints ke liye (jaise /oauth2/authorize, /oauth2/token, /oauth2/jwks, /userinfo, etc.)
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

        // Replaced this call with the implementation of applyDefaultSecurity() to be able to add a custom redirect_uri validator
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // create authorization server configurer.
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);

        // Register a custom redirect_uri validator, that allows redirect uris based on https://localhost during development.
        // Ye line ek custom validator register kar rahi hai jo redirect_uri validate karega jab client authorization request bhejta hai.
        // Normally, redirect URIs strictly match karni chahiye un URIs se jo client registration me diye gaye hain.
        //Lekin dev environment me https://localhost ko allow karne ke liye ye custom validator lagaya gaya hai.
        authorizationServerConfigurer.authorizationEndpoint(authorizationEndpoint ->
                authorizationEndpoint.authenticationProviders(configureAuthenticationValidator()));

        // Ye matcher batata hai ke ye security chain sirf un endpoints par apply hogi jo authorization server ke endpoints hain —
        // jaise /oauth2/authorize, /oauth2/token, /oauth2/jwks, /userinfo, etc.
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http.securityMatcher(endpointsMatcher)
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(authorizationServerConfigurer);

        // Ye line OpenID Connect 1.0 ko enable karti hai — jisse /userinfo, /openid-configuration,
        // aur ID Token issue karne wali functionality activate hoti hai.
        // Without this, server sirf OAuth2 tokens issue karega;
        // With this, wo user identity (ID token) bhi provide karega.
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0

        http.exceptionHandling((ExceptionHandlingConfigurer<HttpSecurity> exceptions) -> {
                    exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
                })
                .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> {
                    httpSecurityOAuth2ResourceServerConfigurer.jwt();
                });

        return http.build();
    }

    /**
     * Two OAuth clients are registered, reader and writer. The reader client is granted a product:read
     * scope, and the writer client is granted both a product:read and product:write scope.
     * The clients are configured to have their client secret set to secret-reader and secret-writer,
     * respectively.
     *
     * By default, for security reasons, the authorization server does not allow redirect URIs that
     * start with https://localhost
     *
     * Here we are registering clients with the OAuth2 Authorization Server. The spring bean RegisteredClientRepository
     * will manage clients for the Authorization Server.RegisteredClientRepository ek repository hai jisme aapke
     * OAuth clients store hote hain.Yahan hum do clients create kar rahe hain: writer aur reader.
     *
     * .redirectUri("https://my.redirect.uri")
     * .redirectUri("https://localhost:8443/openapi/webjars/swagger-ui/oauth2-redirect.html")
     * Ye URIs woh jagah hain jahan authorization server code/token bhejega.
     * Pehla URI app ka production redirect URI hai, doosra Swagger UI ke liye hai.
     *
     * .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
     * Ye setting user se explicit consent mangti hai jab client unke data ko access kare.
     * Ye content jaise hum goolge mae detay hain ke "allow access".
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient writerClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("writer")
                .clientSecret(passwordEncoder.encode("secret-writer"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)

                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)

                .redirectUri("https://my.redirect.uri")
                .redirectUri("https://localhost:8443/openapi/webjars/swagger-ui/oauth2-redirect.html")

                .scope(OidcScopes.OPENID)
                .scope("product:read")  // define api permissions
                .scope("product:write") // define api permissions

                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
                .build();

        RegisteredClient readerClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("reader")
                .clientSecret(passwordEncoder.encode("secret-reader"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("https://my.redirect.uri")
                .redirectUri("https://localhost:8443/openapi/webjars/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("product:read")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
                .build();

        return new InMemoryRegisteredClientRepository(writerClient, readerClient);

    }

    /**
     * Ye bean Authorization Server ko batata hai ke tokens sign karne ke liye kaunsi keys use karni hain.
     * JWK = JSON Web Key, ek standard format hai jisme public/private keys store hoti hain.
     * RSAKey rsaKey = Jwks.generateRsa();
     * → Ye ek RSA key pair (public + private key) bana raha hai.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = Jwks.generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> securityContextJWKSource =
                (JWKSelector jwkSelector, SecurityContext securityContext) -> {
            return jwkSelector.select(jwkSet);
        };
        return securityContextJWKSource;
    }

    /**
     * JwtDecoder ka kaam hota hai incoming JWT tokens verify aur decode karna.
     * Ye bean kehta hai ke authorization server apni JWK source (public key) use kare
     * tokens verify karne ke liye.
     * So jab koi request aati hai /userinfo ya koi resource endpoint pe,
     * ye token ke signature ko check karega using the public key from jwkSource.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Ye bean Authorization Server ke basic metadata/config define karta hai.
     * issuer("http://auth-server:9999") → ye batata hai ke ye tokens kahan se issue hue hain.
     * Ye value token ke iss (issuer) claim me jaati hai.
     * Resource servers isse check karte hain jab wo token verify karte hain (jaise issuer-uri config me).
     *
     * Ye bean batata hai:
     * “Main authorization server hoon aur mera official address hai http://auth-server:9999.”
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://auth-server:9999").build();
    }

    /**
     * Ye method ek custom validator setup karta hai for authorization code requests.
     * OAuth2AuthorizationCodeRequestAuthenticationProvider
     * → ye wo component hai jo handle karta hai jab koi client /oauth2/authorize endpoint hit karta hai.
     */
    private Consumer<List<AuthenticationProvider>> configureAuthenticationValidator() {
        return (authenticationProviders) ->
                authenticationProviders.forEach((authenticationProvider) -> {
                    if (authenticationProvider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authenticationValidator =
                                // Override default redirect_uri validator
                                new CustomRedirectUriValidator()
                                        // Reuse default scope validator
                                        .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR);

                        ((OAuth2AuthorizationCodeRequestAuthenticationProvider) authenticationProvider)
                                .setAuthenticationValidator(authenticationValidator);
                    }
                });
    }

    /**
     * Ye class actual redirect_uri validation logic implement karti hai.
     * Jab koi client (e.g. "writer") /oauth2/authorize call karta hai,
     * wo ek redirect_uri bhejta hai (jahan code bhejna hota hai).
     * Ye validator check karta hai:
     * if (!registeredClient.getRedirectUris().contains(requestedRedirectUri)) throw error;
     * Agar client ne registered URIs me ye URI nahi di → request invalid.
     * Agar URI match kar jae → “Redirect uri is OK!” log kar deta hai.
     */

    static class CustomRedirectUriValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

        @Override
        public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
            OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                    authenticationContext.getAuthentication();
            RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
            String requestedRedirectUri = authorizationCodeRequestAuthentication.getRedirectUri();

            LOG.trace("Will validate the redirect uri {}", requestedRedirectUri);

            // Use exact string matching when comparing client redirect URIs against pre-registered URIs
            if (!registeredClient.getRedirectUris().contains(requestedRedirectUri)) {
                LOG.trace("Redirect uri is invalid!");
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
                throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, null);
            }
            LOG.trace("Redirect uri is OK!");
        }
    }
}
