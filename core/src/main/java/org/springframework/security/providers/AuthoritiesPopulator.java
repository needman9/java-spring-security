package org.springframework.security.providers;

import org.springframework.security.AuthenticationException;
import org.springframework.security.userdetails.UserDetails;

/**
 * Populates the <code>UserDetails</code> associated with a CAS authenticated
 * user.
 *
 * <p>
 * Intended to grant authorities (roles) for providers that do not support
 * authorities/roles directly. It merely authenticates their identity.
 * As Spring Security needs to know the authorities granted to a user in
 * order to construct a valid <code>Authentication</code> object, implementations
 * of this interface will provide this information.
 * </p>
 *
 * <p>
 * A {@link UserDetails} is returned by implementations. The
 * <code>UserDetails</code> must, at minimum, contain the username and
 * <code>GrantedAuthority[]</code> objects applicable to the authenticated
 * user. Note that Spring Security ignores the password and enabled/disabled
 * status of the <code>UserDetails</code> because this is
 * authentication-related and should have been enforced by another provider server. The
 * <code>UserDetails</code> returned by implementations is stored in the
 * generated <code>AuthenticationToken</code>, so additional properties
 * such as email addresses, telephone numbers etc can easily be stored.
 * </p>
 *
 * <p>
 * Implementations should not perform any caching. They will only be called
 * when a refresh is required.
 * </p>
 *
 * @author Ben Alex
 * @author Ray Krueger
 * @version $Id$
 */
public interface AuthoritiesPopulator {
    /**
     * Obtains the granted authorities for the specified user.<P>May throw any
     * <code>AuthenticationException</code> or return <code>null</code> if the authorities are unavailable.</p>
     *
     * @param casUserId as obtained from the CAS validation service
     *
     * @return the details of the indicated user (at minimum the granted authorities and the username)
     *
     * @throws org.springframework.security.AuthenticationException DOCUMENT ME!
     */
    UserDetails getUserDetails(String casUserId)
        throws AuthenticationException;
}
