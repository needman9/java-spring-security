package org.springframework.security.ui.preauth.j2ee;

import org.springframework.security.ui.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.springframework.security.ui.AuthenticationDetailsSourceImpl;
import org.springframework.security.providers.preauth.PreAuthenticatedGrantedAuthoritiesSetter;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.rolemapping.Roles2GrantedAuthoritiesMapper;
import org.springframework.security.rolemapping.MappableRolesRetriever;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource extends AuthenticationDetailsSourceImpl implements InitializingBean {
	private static final Log LOG = LogFactory.getLog(J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource.class);

	private String[] j2eeMappableRoles;

	private Roles2GrantedAuthoritiesMapper j2eeUserRoles2GrantedAuthoritiesMapper;

	/**
	 * Public constructor which overrides the default AuthenticationDetails
	 * class to be used.
	 */
	public J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource() {
		super.setClazz(PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails.class);
	}

	/**
	 * Check that all required properties have been set.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(j2eeMappableRoles, "J2EE defined roles not available");
		Assert.notNull(j2eeUserRoles2GrantedAuthoritiesMapper, "J2EE user roles to granted authorities mapper not set");
	}

	/**
	 * Build the authentication details object. If the speficied authentication
	 * details class implements the PreAuthenticatedGrantedAuthoritiesSetter, a
	 * list of pre-authenticated Granted Authorities will be set based on the
	 * J2EE roles for the current user.
	 * 
	 * @see org.springframework.security.ui.AuthenticationDetailsSource#buildDetails(javax.servlet.http.HttpServletRequest)
	 */
	public Object buildDetails(HttpServletRequest request) {
		Object result = super.buildDetails(request);
		if (result instanceof PreAuthenticatedGrantedAuthoritiesSetter) {
			((PreAuthenticatedGrantedAuthoritiesSetter) result)
					.setPreAuthenticatedGrantedAuthorities(getJ2eeBasedGrantedAuthorities(request));
		}
		return result;
	}

	/**
	 * Get a list of Granted Authorities based on the current user's J2EE roles.
	 * 
	 * @param request
	 *            The HttpServletRequest
	 * @return GrantedAuthority[] mapped from the user's J2EE roles.
	 */
	private GrantedAuthority[] getJ2eeBasedGrantedAuthorities(HttpServletRequest request) {
		ArrayList j2eeUserRolesList = new ArrayList();

		for (int i = 0; i < j2eeMappableRoles.length; i++) {
			if (request.isUserInRole(j2eeMappableRoles[i])) {
				j2eeUserRolesList.add(j2eeMappableRoles[i]);
			}
		}
		String[] j2eeUserRoles = new String[j2eeUserRolesList.size()];
		j2eeUserRoles = (String[]) j2eeUserRolesList.toArray(j2eeUserRoles);
		GrantedAuthority[] userGas = j2eeUserRoles2GrantedAuthoritiesMapper.getGrantedAuthorities(j2eeUserRoles);
		if (LOG.isDebugEnabled()) {
			LOG.debug("J2EE user roles [" + StringUtils.join(j2eeUserRoles) + "] mapped to Granted Authorities: ["
					+ StringUtils.join(userGas) + "]");
		}
		return userGas;
	}

	/**
	 * @param aJ2eeMappableRolesRetriever
	 *            The MappableRolesRetriever to use
	 */
	public void setJ2eeMappableRolesRetriever(MappableRolesRetriever aJ2eeMappableRolesRetriever) {
		this.j2eeMappableRoles = aJ2eeMappableRolesRetriever.getMappableRoles();
	}

	/**
	 * @param mapper
	 *            The Roles2GrantedAuthoritiesMapper to use
	 */
	public void setJ2eeUserRoles2GrantedAuthoritiesMapper(Roles2GrantedAuthoritiesMapper mapper) {
		j2eeUserRoles2GrantedAuthoritiesMapper = mapper;
	}

}
