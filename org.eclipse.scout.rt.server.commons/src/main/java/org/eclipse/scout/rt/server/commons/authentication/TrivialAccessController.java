/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.server.commons.authentication;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.security.IPrincipalProducer;
import org.eclipse.scout.rt.platform.security.IPrincipalVerifier;
import org.eclipse.scout.rt.platform.security.SimplePrincipalProducer;
import org.eclipse.scout.rt.platform.util.Assertions;
import org.eclipse.scout.rt.platform.util.ObjectUtility;
import org.eclipse.scout.rt.platform.util.StringUtility;

/**
 * Access controller to fast-check user's access to requested resources, and handles '/login' and '/logout' requests.
 * Requests to '/auth' are not handled.
 * <p>
 * The name trivial results from this controller's characteristics to only evaluate pre-calculated authentication
 * information, but never performs an expensive verification. That is why this controller is to be installed as the very
 * first access controller.
 * <p>
 * For requests to '/login', the request is dispatched to login.html<br/>
 * For requests to '/logout', the associated HTTP session is invalidated (if any), and the request dispatched to
 * logout.html.
 * <p>
 * This access controller continues chain if one of the following criteria applies:
 * <ul>
 * <li>if running within a {@link Subject} that contains the principal as set in
 * {@link HttpServletRequest#getRemoteUser()};</li>
 * <li>if {@link HttpServletRequest#getRemoteUser()} or {@link HttpServletRequest#getUserPrincipal()} is set;</li>
 * <li>if a {@link HttpSession} exists with a {@link Principal} set in
 * {@link ServletFilterHelper#SESSION_ATTRIBUTE_FOR_PRINCIPAL};</li>
 * <li>if the request path is subject for exclusion;
 * </ul>
 * Also, any request to "" is redirected to "/".
 *
 * @since 5.1
 */
public class TrivialAccessController implements IAccessController {

  private TrivialAuthConfig m_config;

  public TrivialAccessController init(final TrivialAuthConfig config) {
    m_config = config;
    return this;
  }

  @Override
  public boolean handle(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
    if (!m_config.isEnabled()) {
      return false;
    }

    ServletFilterHelper helper = BEANS.get(ServletFilterHelper.class);
    if (helper.isUnloadRequest(request) && !isAuthenticatedRequest(request)) {
      // Unload requests are handled by the {@code UnloadRequestHandler}.
      // The unload request is supposed to close a UI session by the browser. If the current session is not authenticated then there is no session to unload.
      // Answering the unauthenticated unload request here prevents other controllers (e.g. saml or oidc) to produce unwanted redirect urls after authentication (we want to avoid the /unload/ url as redirect url).
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }

    switch (helper.getTarget(request)) {
      case "/login":
        if (m_config.isHandleAuthentication()) {
          handleLoginRequest(request, response);
          return true;
        }
        else {
          return false;
        }
      case "/logout":
        if (m_config.isHandleAuthentication()) {
          handleLogoutRequest(request, response);
          return true;
        }
        else {
          return false;
        }
      case "/unsupported-browser.html":
      case "/legacy-browsers.js": { // see LegacyBrowserScriptLoader
        chain.doFilter(request, response);
        return true;
      }
      case "/auth":
        return false;
      default:
        return handleRequest(request, response, chain);
    }
  }

  @Override
  public void destroy() {
    // NOOP
  }

  protected boolean handleRequest(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
    ServletFilterHelper helper = BEANS.get(ServletFilterHelper.class);
    if (helper.redirectIncompleteBasePath(request, response, false)) {
      return true;
    }

    final AuthenticationStatus authenticationStatus = getAuthenticationStatus(request);
    // Is running within a valid subject?
    if (AuthenticationStatusType.SUBJECT_VALID == authenticationStatus.getStatus()) {
      if (helper.redirectAfterLogin(request, response, helper)) {
        return true;
      }
      chain.doFilter(request, response);
      return true;
    }

    // Is already authenticated?
    if (AuthenticationStatusType.PRINCIPAL_INVALID == authenticationStatus.getStatus()) {
      return false;
    }
    if (AuthenticationStatusType.PRINCIPAL_VALID == authenticationStatus.getStatus()) {
      if (helper.redirectAfterLogin(request, response, helper)) {
        return true;
      }
      helper.continueChainAsSubject(authenticationStatus.getPrincipal(), request, response, chain);
      return true;
    }

    // Is request path excluded from authentication?
    if (m_config.getPathInfoFilter().accepts(StringUtility.emptyIfNull(request.getServletPath()) + StringUtility.emptyIfNull(request.getPathInfo()))) {
      chain.doFilter(request, response);
      return true;
    }

    return false;
  }

  /**
   * Method invoked on a request targeted to '/login'.<br/>
   * If login page is installed, the default implementation dispatches to '/login.html' page so that the user can enter
   * username and password.
   */
  protected void handleLoginRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    if (m_config.isLoginPageInstalled()) {
      BEANS.get(ServletFilterHelper.class).forwardToLoginForm(request, response);
    }
  }

  /**
   * Method invoked on a request targeted to '/logout'.<br/>
   * The default implementation invalidates HTTP session (if any) and if logout page is installed, dispatches the
   * request to '/logout.html' page.
   */
  protected void handleLogoutRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    BEANS.get(ServletFilterHelper.class).doLogout(request);
    if (m_config.isLoginPageInstalled()) {
      BEANS.get(ServletFilterHelper.class).forwardToLogoutForm(request, response);
    }
  }

  /**
   * Configuration for {@link TrivialAccessController}.
   */
  public static class TrivialAuthConfig {

    private boolean m_enabled = true;
    private IPrincipalProducer m_principalProducer = BEANS.get(SimplePrincipalProducer.class);
    private PathInfoFilter m_exclusionFilter;
    private boolean m_handleAuthentication = true;
    private boolean m_loginPageInstalled = false;
    private IPrincipalVerifier m_principalVerifier;

    public boolean isEnabled() {
      return m_enabled;
    }

    public TrivialAuthConfig withEnabled(final boolean enabled) {
      m_enabled = enabled;
      return this;
    }

    public IPrincipalProducer getPrincipalProducer() {
      return m_principalProducer;
    }

    public TrivialAuthConfig withPrincipalProducer(final IPrincipalProducer principalProducer) {
      m_principalProducer = principalProducer;
      return this;
    }

    public IPrincipalVerifier getPrincipalVerifier() {
      return m_principalVerifier;
    }

    public TrivialAuthConfig withPrincipalVerifier(final IPrincipalVerifier principalVerifier) {
      m_principalVerifier = principalVerifier;
      return this;
    }

    public PathInfoFilter getPathInfoFilter() {
      return m_exclusionFilter;
    }

    /**
     * Exclude resources from authentication.
     * <p>
     * Filter format: separate resources by comma, newline or whitespace; usage of wildcard (*) character is supported;
     */
    public TrivialAuthConfig withExclusionFilter(final String exclusionFilter) {
      m_exclusionFilter = new PathInfoFilter(exclusionFilter);
      return this;
    }

    /**
     * Default true. This filter forwards to login.html / logout.html
     * <p>
     * Set to false when using indirect login such as pac4j, keycloak or other third party identity provider in a
     * servlet filter following this filter that also handle /login and /logout
     */
    public boolean isHandleAuthentication() {
      return m_handleAuthentication;
    }

    /**
     * Indicates whether this web application has a login and logout page installed, meaning that the request is
     * dispatched to that page when requesting to log in, or upon logged out.
     */
    public TrivialAuthConfig withHandleAuthentication(final boolean handleAuthentication) {
      m_handleAuthentication = handleAuthentication;
      return this;
    }

    public boolean isLoginPageInstalled() {
      return m_loginPageInstalled;
    }

    /**
     * Indicates whether this web application has a login and logout page installed, meaning that the request is
     * dispatched to that page when requesting to log in, or upon logged out.
     */
    public TrivialAuthConfig withLoginPageInstalled(final boolean loginPageInstalled) {
      m_loginPageInstalled = loginPageInstalled;
      return this;
    }
  }

  /**
   * Checks if the request is authenticated by either a valid subject or a valid principal.
   *
   * @param request
   *     Http request
   * @return {@code true} if request is authenticated. Otherwise {@code false}.
   */
  protected boolean isAuthenticatedRequest(HttpServletRequest request) {
    return ObjectUtility.isOneOf(getAuthenticationStatus(request).getStatus(), AuthenticationStatusType.SUBJECT_VALID, AuthenticationStatusType.PRINCIPAL_VALID);
  }

  /**
   * Gets the authentication status of the http request.
   * <br/><br/>
   * Possible status are:
   * <ul>
   *   <li><b>'{@code AuthenticationStatusType.NONE}'</b>: No authentication.</li>
   *   <li><b>'{@code AuthenticationStatusType.SUBJECT_VALID}'</b>: Authenticated by valid subject.</li>
   *   <li><b>'{@code AuthenticationStatusType.PRINCIPAL_INVALID}'</b>: No authentication but invalid principal.</li>
   *   <li><b>'{@code AuthenticationStatusType.PRINCIPAL_VALID}'</b>: Authenticated by valid principal.</li>
   * </ul>
   * <p>
   * In case of a status '{@code AuthenticationStatusType.PRINCIPAL_INVALID}' or '{@code AuthenticationStatusType.PRINCIPAL_INVALID}' the found principal is provided too.
   *
   * @param request
   *     Http request
   * @return Authentication status
   */
  @Nonnull
  protected AuthenticationStatus getAuthenticationStatus(HttpServletRequest request) {
    // Is running within a valid subject?
    if (BEANS.get(ServletFilterHelper.class).isRunningWithValidSubject(request)) {
      return new AuthenticationStatus(AuthenticationStatusType.SUBJECT_VALID);
    }

    // Is already authenticated?
    final Principal principal = BEANS.get(ServletFilterHelper.class).findPrincipal(request, m_config.getPrincipalProducer());
    if (principal == null) {
      return new AuthenticationStatus(AuthenticationStatusType.NONE);
    }
    if (m_config.m_principalVerifier == null) {
      return new AuthenticationStatus(AuthenticationStatusType.PRINCIPAL_VALID, principal);
    }
    return new AuthenticationStatus(m_config.m_principalVerifier.verify(principal) ? AuthenticationStatusType.PRINCIPAL_VALID : AuthenticationStatusType.PRINCIPAL_INVALID, principal);
  }

  protected enum AuthenticationStatusType {
    NONE,
    SUBJECT_VALID,
    PRINCIPAL_VALID,
    PRINCIPAL_INVALID
  }

  protected class AuthenticationStatus {
    private final AuthenticationStatusType m_status;
    private final Principal m_principal;

    public AuthenticationStatus(AuthenticationStatusType status) {
      Assertions.assertNotNull(status);
      m_status = status;
      m_principal = null;
    }

    public AuthenticationStatus(AuthenticationStatusType status, Principal principal) {
      Assertions.assertNotNull(status);
      Assertions.assertNotNull(principal);
      m_status = status;
      m_principal = principal;
    }

    public Principal getPrincipal() {
      return m_principal;
    }

    public AuthenticationStatusType getStatus() {
      return m_status;
    }
  }
}
