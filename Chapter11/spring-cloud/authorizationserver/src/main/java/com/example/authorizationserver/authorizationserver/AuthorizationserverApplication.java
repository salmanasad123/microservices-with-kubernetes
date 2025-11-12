package com.example.authorizationserver.authorizationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Authorization Server supports both the use of the OpenID Connect discovery endpoint
 * and the digital signing of access tokens. It also provides an endpoint that can be accessed using the
 * discovery information to get keys for verifying the digital signature of a token.
 *
 * COMPLETE OATH FLOW:
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
 *  This step tells Google: “Hey, PhotoApp wants permission to access the user’s photos.”
 *  User Logs In and Grants Consent
 *  Google shows login page → you enter username/password.
 *  Then Google asks: “Do you allow PhotoApp to access your photos?”
 *  This is where the ClientSettings.requireAuthorizationConsent(true) in Spring code corresponds conceptually.
 *
 *  Authorization Code Issued
 *  After consent, Google redirects back to PhotoApp’s redirect URI with a code:
 *  https://photoapp.com/oauth/callback?code=AUTH_CODE&state=xyz
 *  This code is short-lived and can be exchanged for a token.
 *  Token Exchange
 *  PhotoApp backend sends POST request to Google’s token endpoint with:
 *
 *  client_id=PHOTO_APP_CLIENT_ID
 *  client_secret=PHOTO_APP_CLIENT_SECRET
 *  code=AUTH_CODE
 *  redirect_uri=https://photoapp.com/oauth/callback
 *  grant_type=authorization_code
 *
 *  Google responds with access_token (and optionally refresh_token) which PhotoApp uses to access Google Drive API.
 *  Client Registration Details
 *
 *  When does registration happen?
 *  During app development (one-time). Not every user click.
 *  What does the client provide?
 *  redirect_uri (where Google should send the code after authorization).
 *  Some basic info like app name, logo, website.
 *
 *  What does Google provide back?
 *  client_id → public identifier of PhotoApp.
 *  client_secret → secret key to authenticate PhotoApp when exchanging code for token.
 *  So yes, client is registered before user interaction, not dynamically per user.
 *
 *  Where OpenID Connect (OIDC) fits
 *  OAuth2 does not itself validate user identity; it only gives access to resources.
 *  OpenID Connect adds identity layer on top of OAuth2.
 *  With OIDC:
 *  Scope openid is requested: .scope(OidcScopes.OPENID)
 *  Authorization Server returns ID Token (JWT) that tells PhotoApp “This user is Salman, logged in successfully.”
 *  This allows PhotoApp to authenticate the user, not just access resources.
 *
 *  In your scenario:
 *  If PhotoApp wants to know who you are (your identity, email, etc.), it uses OpenID Connect.
 *  If PhotoApp only wants photos, OAuth2 scopes are enough.
 *
 *  Key Mapping to your Spring Boot code
 *  Spring Boot Code	Google Scenario
 *  RegisteredClient:     	    PhotoApp registration with Google
 *  clientId, clientSecret: 	    client_id and client_secret Google gives to PhotoApp
 *  redirectUri:	                redirect URI Google uses to send auth code back
 *  authorizationGrantType:  	flow types PhotoApp supports (AUTHORIZATION_CODE in this scenario)
 *  scope(OidcScopes.OPENID):	requesting identity info from Google via OIDC
 *  scope("product:read"):	    requesting access to Google Drive API (read photos)
 *
 */

@SpringBootApplication
public class AuthorizationserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationserverApplication.class, args);
	}

}
