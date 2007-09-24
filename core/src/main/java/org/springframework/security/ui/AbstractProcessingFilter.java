/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.ui;

import org.springframework.security.AcegiMessageSource;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.util.RedirectUtils;

import org.springframework.security.context.SecurityContextHolder;

import org.springframework.security.event.authentication.InteractiveAuthenticationSuccessEvent;

import org.springframework.security.ui.rememberme.NullRememberMeServices;
import org.springframework.security.ui.rememberme.RememberMeServices;
import org.springframework.security.ui.savedrequest.SavedRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;

import org.springframework.util.Assert;

import java.io.IOException;

import java.util.Properties;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Abstract processor of browser-based HTTP-based authentication requests.
 * <p>
 * This filter is responsible for processing authentication requests. If
 * authentication is successful, the resulting {@link Authentication} object
 * will be placed into the <code>SecurityContext</code>, which is guaranteed
 * to have already been created by an earlier filter.
 * </p>
 * <p>
 * If authentication fails, the <code>AuthenticationException</code> will be
 * placed into the <code>HttpSession</code> with the attribute defined by
 * {@link #SPRING_SECURITY_LAST_EXCEPTION_KEY}.
 * </p>
 * <p>
 * To use this filter, it is necessary to specify the following properties:
 * </p>
 * <ul>
 * <li><code>defaultTargetUrl</code> indicates the URL that should be used
 * for redirection if the <code>HttpSession</code> attribute named
 * {@link #SPRING_SECURITY_SAVED_REQUEST_KEY} does not indicate the target URL once
 * authentication is completed successfully. eg: <code>/</code>. The
 * <code>defaultTargetUrl</code> will be treated as relative to the web-app's
 * context path, and should include the leading <code>/</code>.
 * Alternatively, inclusion of a scheme name (eg http:// or https://) as the
 * prefix will denote a fully-qualified URL and this is also supported.</li>
 * <li><code>authenticationFailureUrl</code> indicates the URL that should be
 * used for redirection if the authentication request fails. eg:
 * <code>/login.jsp?login_error=1</code>.</li>
 * <li><code>filterProcessesUrl</code> indicates the URL that this filter
 * will respond to. This parameter varies by subclass.</li>
 * <li><code>alwaysUseDefaultTargetUrl</code> causes successful
 * authentication to always redirect to the <code>defaultTargetUrl</code>,
 * even if the <code>HttpSession</code> attribute named {@link
 * # SPRING_SECURITY_SAVED_REQUEST_KEY} defines the intended target URL.</li>
 * </ul>
 * <p>
 * To configure this filter to redirect to specific pages as the result of
 * specific {@link AuthenticationException}s you can do the following.
 * Configure the <code>exceptionMappings</code> property in your application
 * xml. This property is a java.util.Properties object that maps a
 * fully-qualified exception class name to a redirection url target. For
 * example:
 *
 * <pre>
 *  &lt;property name=&quot;exceptionMappings&quot;&gt;
 *    &lt;props&gt;
 *      &lt;prop&gt; key=&quot;org.springframework.security.BadCredentialsException&quot;&gt;/bad_credentials.jsp&lt;/prop&gt;
 *    &lt;/props&gt;
 *  &lt;/property&gt;
 * </pre>
 *
 * The example above would redirect all
 * {@link org.springframework.security.BadCredentialsException}s thrown, to a page in the
 * web-application called /bad_credentials.jsp.
 * </p>
 * <p>
 * Any {@link AuthenticationException} thrown that cannot be matched in the
 * <code>exceptionMappings</code> will be redirected to the
 * <code>authenticationFailureUrl</code>
 * </p>
 * <p>
 * If authentication is successful, an {@link
 * org.springframework.security.event.authentication.InteractiveAuthenticationSuccessEvent}
 * will be published to the application context. No events will be published if
 * authentication was unsuccessful, because this would generally be recorded via
 * an <code>AuthenticationManager</code>-specific application event.
 * </p>
 *  <p>The filter has an optional attribute <tt>invalidateSessionOnSuccessfulAuthentication</tt> that will invalidate
 * the current session on successful authentication. This is to protect against session fixation attacks (see
 * <a href="http://en.wikipedia.org/wiki/Session_fixation">this Wikipedia article</a> for more information).
 * The behaviour is turned off by default. Additionally there is a property <tt>migrateInvalidatedSessionAttributes</tt>
 * which tells if on session invalidation we are to migrate all session attributes from the old session to a newly
 * created one. This is turned on by default, but not used unless <tt>invalidateSessionOnSuccessfulAuthentication</tt>
 * is true.</p>
 *
 * @author Ben Alex
 * @version $Id: AbstractProcessingFilter.java 1909 2007-06-19 04:08:19Z
 * vishalpuri $
 */
public abstract class AbstractProcessingFilter implements Filter, InitializingBean, ApplicationEventPublisherAware,
		MessageSourceAware {
	//~ Static fields/initializers =====================================================================================

	public static final String SPRING_SECURITY_SAVED_REQUEST_KEY = "SPRING_SECURITY_SAVED_REQUEST_KEY";

	public static final String SPRING_SECURITY_LAST_EXCEPTION_KEY = "SPRING_SECURITY_LAST_EXCEPTION";

	//~ Instance fields ================================================================================================

	protected ApplicationEventPublisher eventPublisher;

	protected AuthenticationDetailsSource authenticationDetailsSource = new AuthenticationDetailsSourceImpl();

	private AuthenticationManager authenticationManager;

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected MessageSourceAccessor messages = AcegiMessageSource.getAccessor();

	private Properties exceptionMappings = new Properties();

	private RememberMeServices rememberMeServices = new NullRememberMeServices();

	/** Where to redirect the browser to if authentication fails */
	private String authenticationFailureUrl;

	/**
	 * Where to redirect the browser to if authentication is successful but
	 * SPRING_SECURITY_SAVED_REQUEST_KEY is <code>null</code>
	 */
	private String defaultTargetUrl;

	/**
	 * The URL destination that this filter intercepts and processes (usually
	 * something like <code>/j_spring_security_check</code>)
	 */
	private String filterProcessesUrl = getDefaultFilterProcessesUrl();

	/**
	 * If <code>true</code>, will always redirect to the value of
	 * {@link #getDefaultTargetUrl} upon successful authentication, irrespective
	 * of the page that caused the authentication request (defaults to
	 * <code>false</code>).
	 */
	private boolean alwaysUseDefaultTargetUrl = false;

	/**
	 * Indicates if the filter chain should be continued prior to delegation to
	 * {@link #successfulAuthentication(HttpServletRequest, HttpServletResponse,
	 * Authentication)}, which may be useful in certain environment (eg
	 * Tapestry). Defaults to <code>false</code>.
	 */
	private boolean continueChainBeforeSuccessfulAuthentication = false;

	/**
	 * If true, causes any redirection URLs to be calculated minus the protocol
	 * and context path (defaults to false).
	 */
	private boolean useRelativeContext = false;


    /**
     * Tells if we on successful authentication should invalidate the
     * current session. This is a common guard against session fixation attacks.
     * Defaults to <code>false</code>.
     */
    private boolean invalidateSessionOnSuccessfulAuthentication = false;

    /**
     * If {@link #invalidateSessionOnSuccessfulAuthentication} is true, this
     * flag indicates that the session attributes of the session to be invalidated
     * are to be migrated to the new session. Defaults to <code>true</code> since
     * nothing will happpen unless {@link #invalidateSessionOnSuccessfulAuthentication}
     * is true.
     */
    private boolean migrateInvalidatedSessionAttributes = true;

    //~ Methods ========================================================================================================

	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(filterProcessesUrl, "filterProcessesUrl must be specified");
		Assert.hasLength(defaultTargetUrl, "defaultTargetUrl must be specified");
		Assert.hasLength(authenticationFailureUrl, "authenticationFailureUrl must be specified");
		Assert.notNull(authenticationManager, "authenticationManager must be specified");
		Assert.notNull(this.rememberMeServices);
	}

	/**
	 * Performs actual authentication.
	 *
	 * @param request from which to extract parameters and perform the
	 * authentication
	 *
	 * @return the authenticated user
	 *
	 * @throws AuthenticationException if authentication fails
	 */
	public abstract Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException;

	/**
	 * Does nothing. We use IoC container lifecycle services instead.
	 */
	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (!(request instanceof HttpServletRequest)) {
			throw new ServletException("Can only process HttpServletRequest");
		}

		if (!(response instanceof HttpServletResponse)) {
			throw new ServletException("Can only process HttpServletResponse");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (requiresAuthentication(httpRequest, httpResponse)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Request is to process authentication");
			}

			Authentication authResult;

			try {
				onPreAuthentication(httpRequest, httpResponse);
				authResult = attemptAuthentication(httpRequest);
			}
			catch (AuthenticationException failed) {
				// Authentication failed
				unsuccessfulAuthentication(httpRequest, httpResponse, failed);

				return;
			}

			// Authentication success
			if (continueChainBeforeSuccessfulAuthentication) {
				chain.doFilter(request, response);
			}

			successfulAuthentication(httpRequest, httpResponse, authResult);

			return;
		}

		chain.doFilter(request, response);
	}

	public String getAuthenticationFailureUrl() {
		return authenticationFailureUrl;
	}

	public AuthenticationManager getAuthenticationManager() {
		return authenticationManager;
	}

	/**
	 * Specifies the default <code>filterProcessesUrl</code> for the
	 * implementation.
	 *
	 * @return the default <code>filterProcessesUrl</code>
	 */
	public abstract String getDefaultFilterProcessesUrl();

	/**
	 * Supplies the default target Url that will be used if no saved request is
	 * found or the <tt>alwaysUseDefaultTargetUrl</tt> propert is set to true.
	 * Override this method of you want to provide a customized default Url (for
	 * example if you want different Urls depending on the authorities of the
	 * user who has just logged in).
	 *
	 * @return the defaultTargetUrl property
	 */
	public String getDefaultTargetUrl() {
		return defaultTargetUrl;
	}

	public Properties getExceptionMappings() {
		return new Properties(exceptionMappings);
	}

	public String getFilterProcessesUrl() {
		return filterProcessesUrl;
	}

	public RememberMeServices getRememberMeServices() {
		return rememberMeServices;
	}

	/**
	 * Does nothing. We use IoC container lifecycle services instead.
	 *
	 * @param arg0 ignored
	 *
	 * @throws ServletException ignored
	 */
	public void init(FilterConfig arg0) throws ServletException {
	}

	public boolean isAlwaysUseDefaultTargetUrl() {
		return alwaysUseDefaultTargetUrl;
	}

	public boolean isContinueChainBeforeSuccessfulAuthentication() {
		return continueChainBeforeSuccessfulAuthentication;
	}

	public static String obtainFullRequestUrl(HttpServletRequest request) {
		SavedRequest savedRequest = (SavedRequest) request.getSession().getAttribute(SPRING_SECURITY_SAVED_REQUEST_KEY);

		return (savedRequest == null) ? null : savedRequest.getFullRequestUrl();
	}

	protected void onPreAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException {
	}

	protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			Authentication authResult) throws IOException {
	}

	protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException {
	}

	/**
	 * <p>
	 * Indicates whether this filter should attempt to process a login request
	 * for the current invocation.
	 * </p>
	 * <p>
	 * It strips any parameters from the "path" section of the request URL (such
	 * as the jsessionid parameter in
	 * <em>http://host/myapp/index.html;jsessionid=blah</em>) before matching
	 * against the <code>filterProcessesUrl</code> property.
	 * </p>
	 * <p>
	 * Subclasses may override for special requirements, such as Tapestry
	 * integration.
	 * </p>
	 *
	 * @param request as received from the filter chain
	 * @param response as received from the filter chain
	 *
	 * @return <code>true</code> if the filter should attempt authentication,
	 * <code>false</code> otherwise
	 */
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		String uri = request.getRequestURI();
		int pathParamIndex = uri.indexOf(';');

		if (pathParamIndex > 0) {
			// strip everything after the first semi-colon
			uri = uri.substring(0, pathParamIndex);
		}

		if ("".equals(request.getContextPath())) {
			return uri.endsWith(filterProcessesUrl);
		}

		return uri.endsWith(request.getContextPath() + filterProcessesUrl);
	}

	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url)
			throws IOException {

        RedirectUtils.sendRedirect(request, response, url, useRelativeContext);
	}

	public void setAlwaysUseDefaultTargetUrl(boolean alwaysUseDefaultTargetUrl) {
		this.alwaysUseDefaultTargetUrl = alwaysUseDefaultTargetUrl;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void setAuthenticationDetailsSource(AuthenticationDetailsSource authenticationDetailsSource) {
		Assert.notNull(authenticationDetailsSource, "AuthenticationDetailsSource required");
		this.authenticationDetailsSource = authenticationDetailsSource;
	}

	public void setAuthenticationFailureUrl(String authenticationFailureUrl) {
		this.authenticationFailureUrl = authenticationFailureUrl;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setContinueChainBeforeSuccessfulAuthentication(boolean continueChainBeforeSuccessfulAuthentication) {
		this.continueChainBeforeSuccessfulAuthentication = continueChainBeforeSuccessfulAuthentication;
	}

	public void setDefaultTargetUrl(String defaultTargetUrl) {
		Assert.isTrue(defaultTargetUrl.startsWith("/") | defaultTargetUrl.startsWith("http"),
				"defaultTarget must start with '/' or with 'http(s)'");
		this.defaultTargetUrl = defaultTargetUrl;
	}

	public void setExceptionMappings(Properties exceptionMappings) {
		this.exceptionMappings = exceptionMappings;
	}

	public void setFilterProcessesUrl(String filterProcessesUrl) {
		this.filterProcessesUrl = filterProcessesUrl;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messages = new MessageSourceAccessor(messageSource);
	}

	public void setRememberMeServices(RememberMeServices rememberMeServices) {
		this.rememberMeServices = rememberMeServices;
	}


    public void setInvalidateSessionOnSuccessfulAuthentication(boolean invalidateSessionOnSuccessfulAuthentication) {
        this.invalidateSessionOnSuccessfulAuthentication = invalidateSessionOnSuccessfulAuthentication;
    }


    public void setMigrateInvalidatedSessionAttributes(boolean migrateInvalidatedSessionAttributes) {
        this.migrateInvalidatedSessionAttributes = migrateInvalidatedSessionAttributes;
    }

    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			Authentication authResult) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Authentication success: " + authResult.toString());
		}

		SecurityContextHolder.getContext().setAuthentication(authResult);

		if (logger.isDebugEnabled()) {
			logger.debug("Updated SecurityContextHolder to contain the following Authentication: '" + authResult + "'");
		}


        if (invalidateSessionOnSuccessfulAuthentication) {
            startNewSessionIfRequired(request);
        }

        String targetUrl = determineTargetUrl(request);

		if (logger.isDebugEnabled()) {
			logger.debug("Redirecting to target URL from HTTP Session (or default): " + targetUrl);
		}

		onSuccessfulAuthentication(request, response, authResult);

		rememberMeServices.loginSuccess(request, response, authResult);

		// Fire event
		if (this.eventPublisher != null) {
			eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
		}

		sendRedirect(request, response, targetUrl);
	}

    private void startNewSessionIfRequired(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {

            if (!migrateInvalidatedSessionAttributes) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Invalidating session without migrating attributes.");
                }

                session.invalidate();
                session = null;

                // this is probably not necessary, but seems cleaner since
                // there already was a session going.
                request.getSession(true);

            } else {

                if (logger.isDebugEnabled()) {
                    logger.debug("Invalidating session and migrating attributes.");
                }

                HashMap migratedAttributes = new HashMap();

                Enumeration enumer = session.getAttributeNames();

                while (enumer.hasMoreElements()) {
                    String key = (String) enumer.nextElement();
                    migratedAttributes.put(key, session.getAttribute(key));
                }

                session.invalidate();
                session = request.getSession(true); // we now have a new session

                Iterator iter = migratedAttributes.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    session.setAttribute((String) entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected String determineTargetUrl(HttpServletRequest request) {
		// Don't attempt to obtain the url from the saved request if
		// alwaysUsedefaultTargetUrl is set
		String targetUrl = alwaysUseDefaultTargetUrl ? null : obtainFullRequestUrl(request);

		if (targetUrl == null) {
			targetUrl = getDefaultTargetUrl();
		}

		return targetUrl;
	}

	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException {
		SecurityContextHolder.getContext().setAuthentication(null);

		if (logger.isDebugEnabled()) {
			logger.debug("Updated SecurityContextHolder to contain null Authentication");
		}

		String failureUrl = determineFailureUrl(request, failed);

		if (logger.isDebugEnabled()) {
			logger.debug("Authentication request failed: " + failed.toString());
		}

		try {
			request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, failed);
		}
		catch (Exception ignored) {
		}

		onUnsuccessfulAuthentication(request, response, failed);

		rememberMeServices.loginFail(request, response);

		sendRedirect(request, response, failureUrl);
	}

    protected String determineFailureUrl(HttpServletRequest request, AuthenticationException failed) {
        return exceptionMappings.getProperty(failed.getClass().getName(), authenticationFailureUrl);
    }

    public AuthenticationDetailsSource getAuthenticationDetailsSource() {
		// Required due to SEC-310
		return authenticationDetailsSource;
	}

	public void setUseRelativeContext(boolean useRelativeContext) {
		this.useRelativeContext = useRelativeContext;
	}

}
