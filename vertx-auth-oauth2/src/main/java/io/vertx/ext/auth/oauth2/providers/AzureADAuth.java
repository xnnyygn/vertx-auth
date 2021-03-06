package io.vertx.ext.auth.oauth2.providers;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.*;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;

import static io.vertx.ext.auth.oauth2.OAuth2FlowType.AUTH_JWT;

/**
 * Simplified factory to create an {@link OAuth2Auth} for Azure AD.
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
@VertxGen
public interface AzureADAuth extends OpenIDConnectAuth {

  /**
   * Create a OAuth2Auth provider for Microsoft Azure Active Directory
   *
   * @param clientId     the client id given to you by Azure
   * @param clientSecret the client secret given to you by Azure
   * @param guid         the guid of your application given to you by Azure
   */
  static OAuth2Auth create(Vertx vertx, String clientId, String clientSecret, String guid) {
    return create(vertx, clientId, clientSecret, guid, new HttpClientOptions());
  }

  /**
   * Create a OAuth2Auth provider for Microsoft Azure Active Directory
   *
   * @param clientId          the client id given to you by Azure
   * @param clientSecret      the client secret given to you by Azure
   * @param guid              the guid of your application given to you by Azure
   * @param httpClientOptions custom http client options
   */
  static OAuth2Auth create(Vertx vertx, String clientId, String clientSecret, String guid, HttpClientOptions httpClientOptions) {
    return
      OAuth2Auth.create(vertx, new OAuth2ClientOptions(httpClientOptions)
        .setFlow(OAuth2FlowType.AUTH_CODE)
        .setClientID(clientId)
        .setClientSecret(clientSecret)
        .setTenant(guid)
        .setSite("https://login.windows.net/{tenant}")
        .setTokenPath("/oauth2/token")
        .setAuthorizationPath("/oauth2/authorize")
        .setScopeSeparator(",")
        .setExtraParameters(
          new JsonObject().put("resource", "{tenant}")));
  }

  /**
   * Create a OAuth2Auth provider for OpenID Connect Discovery. The discovery will use the default site in the
   * configuration options and attempt to load the well known descriptor. If a site is provided (for example when
   * running on a custom instance) that site will be used to do the lookup.
   * <p>
   * If the discovered config includes a json web key url, it will be also fetched and the JWKs will be loaded
   * into the OAuth provider so tokens can be decoded.
   * <p>
   * With this provider, if the given configuration is using the flow type {@link OAuth2FlowType#AUTH_JWT} then
   * the extra parameters object will include {@code requested_token_use = on_behalf_of} as required by
   * <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-oauth2-on-behalf-of-flow">https://docs.microsoft.com/en-us/azure/active-directory</a>.
   *
   * @param vertx   the vertx instance
   * @param config  the initial config
   * @param handler the instantiated Oauth2 provider instance handler
   */
  static void discover(final Vertx vertx, final OAuth2ClientOptions config, final Handler<AsyncResult<OAuth2Auth>> handler) {
    // don't override if already set
    final String site = config.getSite() == null ? "https://login.windows.net/common" : config.getSite();

    final JsonObject extraParameters = new JsonObject().put("resource", "{tenant}");

    if (config.getFlow() != null && AUTH_JWT == config.getFlow()) {
      // this is a "on behalf of" mode
      extraParameters.put("requested_token_use", "on_behalf_of");
    }

    OpenIDConnectAuth.discover(
      vertx,
      new OAuth2ClientOptions(config)
        // Azure OpenId does not return the same url where the request was sent to
        .setValidateIssuer(false)
        .setSite(site)
        .setScopeSeparator(",")
        .setExtraParameters(extraParameters),
      handler);
  }

  /**
   * Create a OAuth2Auth provider for OpenID Connect Discovery. The discovery will use the default site in the
   * configuration options and attempt to load the well known descriptor. If a site is provided (for example when
   * running on a custom instance) that site will be used to do the lookup.
   * <p>
   * If the discovered config includes a json web key url, it will be also fetched and the JWKs will be loaded
   * into the OAuth provider so tokens can be decoded.
   *
   * @see AzureADAuth#discover(Vertx, OAuth2ClientOptions, Handler)
   * @param vertx   the vertx instance
   * @param config  the initial config
   * @return future with instantiated Oauth2 provider instance handler
   */
  static Future<OAuth2Auth> discover(final Vertx vertx, final OAuth2ClientOptions config) {
    Promise<OAuth2Auth> promise = Promise.promise();
    discover(vertx, config, promise);
    return promise.future();
  }
}
