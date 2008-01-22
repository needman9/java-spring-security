package org.springframework.security.ui.preauth;

import org.springframework.security.AuthenticationException;
import org.springframework.security.ui.AuthenticationEntryPoint;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;

/**
 * <p>
 * In the pre-authenticated authentication case (unlike CAS, for example) the
 * user will already have been identified through some external mechanism and a
 * secure context established by the time the security-enforcement filter is
 * invoked.
 * </p>
 * <p>
 * Therefore this class isn't actually responsible for the commencement of
 * authentication, as it is in the case of other providers. It will be called if
 * the user is rejected by the AbstractPreAuthenticatedProcessingFilter,
 * resulting in a null authentication.
 * </p>
 * <p>
 * The <code>commence</code> method will always return an
 * <code>HttpServletResponse.SC_FORBIDDEN</code> (403 error).
 * </p>
 * <p>
 * This code is based on
 * {@link org.springframework.security.ui.x509.X509ProcessingFilterEntryPoint}.
 * </p>
 * 
 * @see org.springframework.security.ui.ExceptionTranslationFilter
 */
public class PreAuthenticatedProcesingFilterEntryPoint implements AuthenticationEntryPoint, Ordered {
	private static final Log LOG = LogFactory.getLog(PreAuthenticatedProcesingFilterEntryPoint.class);

	private int order = Integer.MAX_VALUE;

	/**
	 * Always returns a 403 error code to the client.
	 */
	public void commence(ServletRequest request, ServletResponse response, AuthenticationException arg2) throws IOException,
			ServletException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("J2EE entry point called. Rejecting access");
		}
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int i) {
		order = i;
	}

}
