package net.sf.acegisecurity.providers.dao.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import junit.framework.TestCase;

/** Important note: this class merely defines certain 
 *  base properties needed by all LDAP unit tests.
 */
public class BaseLdapTestCase extends TestCase {

    // static finalizers, they'd be nice, as LdapTestHelper 
    // never seems to get the chance to cleanup after itself
	protected static LdapTestHelper ldapTestHelper = new LdapTestHelper();
    
    static {
        //InputStream in = BaseLdapTestCase.class.getResourceAsStream("net/sf/acegisecurity/providers/dao/ldap/test-data.ldif");
        /* InputStream in = ldapTestHelper.getClass().getResourceAsStream("test-data.ldif");
        try {
            ldapTestHelper.importLDIF(in);
        } catch (Exception x) {
            x.printStackTrace();
            ldapTestHelper.shutdownServer();
            ldapTestHelper = null;
            throw new RuntimeException("Server initialization failed.");
        } */
        DirContentsInitializer.initialize( ldapTestHelper.getServerContext() );
    }
	
	protected DirContext getClientContext() throws NamingException {
		Hashtable env = new Hashtable();
		env.put( Context.PROVIDER_URL, "ldap://localhost:389/ou=system" );
		env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
		env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
		env.put( Context.SECURITY_CREDENTIALS, "secret" );
		return new InitialDirContext( env );
	}
	
	/** @return The server context for LDAP ops. used for things like addding/removing users. */
	protected DirContext getServerContext() {
		return ldapTestHelper.getServerContext();
	}
	
}
