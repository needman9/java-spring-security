package org.acegisecurity.providers.ldap;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.Authentication;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.ldap.authenticator.FilterBasedLdapUserSearch;
import org.acegisecurity.providers.ldap.authenticator.BindAuthenticator;
import org.acegisecurity.providers.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.acegisecurity.userdetails.UserDetails;

/**
 * @author Luke Taylor
 * @version $Id$
 */
public class LdapAuthenticationProviderTests extends AbstractLdapServerTestCase {
    DefaultInitialDirContextFactory dirCtxFactory;


    public LdapAuthenticationProviderTests(String string) {
        super(string);
    }

    public LdapAuthenticationProviderTests() {
        super();
    }

    public void testNormalUsage() throws Exception {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider();

        ldapProvider.setAuthenticator(new MockAuthenticator());
        ldapProvider.setLdapAuthoritiesPopulator(new MockAuthoritiesPopulator());
        ldapProvider.afterPropertiesSet();

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("bob","bobspassword");
        UserDetails user = ldapProvider.retrieveUser("bob", token);
        assertEquals(1, user.getAuthorities().length);
        assertTrue(user.getAuthorities()[0].equals("ROLE_USER"));
        ldapProvider.additionalAuthenticationChecks(user, token);

    }

    public void testIntegration() throws Exception {
        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider();

        // Connection information
        DefaultInitialDirContextFactory dirCtxFactory = new DefaultInitialDirContextFactory();
        dirCtxFactory.setUrl(PROVIDER_URL);
        dirCtxFactory.setManagerDn(MANAGER_USER);
        dirCtxFactory.setInitialContextFactory(CONTEXT_FACTORY);
        dirCtxFactory.setExtraEnvVars(EXTRA_ENV);
        dirCtxFactory.setManagerPassword(MANAGER_PASSWORD);
        dirCtxFactory.afterPropertiesSet();
        BindAuthenticator authenticator = new BindAuthenticator();
        //PasswordComparisonAuthenticator authenticator = new PasswordComparisonAuthenticator();
        authenticator.setInitialDirContextFactory(dirCtxFactory);
        //authenticator.setUserDnPattern("cn={0},ou=people");

        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch();
        userSearch.setSearchBase("ou=people");
        userSearch.setSearchFilter("(cn={0})");
        userSearch.setInitialDirContextFactory(dirCtxFactory);
        userSearch.afterPropertiesSet();

        authenticator.setUserSearch(userSearch);

        authenticator.afterPropertiesSet();

        DefaultLdapAuthoritiesPopulator populator;
        populator = new DefaultLdapAuthoritiesPopulator();
        populator.setRolePrefix("ROLE_");
        populator.setInitialDirContextFactory(dirCtxFactory);
        populator.setGroupSearchBase("ou=groups");
        populator.afterPropertiesSet();

        ldapProvider.setLdapAuthoritiesPopulator(populator);
        ldapProvider.setAuthenticator(authenticator);
        Authentication auth = ldapProvider.authenticate(new UsernamePasswordAuthenticationToken("Ben Alex","benspassword"));
        assertEquals(2, auth.getAuthorities().length);
    }

    class MockAuthoritiesPopulator implements LdapAuthoritiesPopulator {

        public GrantedAuthority[] getGrantedAuthorities(String userDn, String dn, Attributes userAttributes) {
            return new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_USER") };
        }
    }

    class MockAuthenticator implements LdapAuthenticator {
        Attributes userAttributes = new BasicAttributes("cn","bob");

        public LdapUserDetails authenticate(String username, String password) {
            if(username.equals("bob") && password.equals("bobspassword")) {

                return new LdapUserDetails("cn=bob,ou=people,dc=acegisecurity,dc=org", userAttributes);
            }
            throw new BadCredentialsException("Authentication of Bob failed.");
        }
    }
}
