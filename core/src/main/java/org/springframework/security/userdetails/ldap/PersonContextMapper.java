package org.springframework.security.userdetails.ldap;

import java.util.List;

import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.GrantedAuthority;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.util.Assert;

/**
 * @author Luke Taylor
 * @version $Id$
 */
public class PersonContextMapper implements UserDetailsContextMapper {

    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, List<GrantedAuthority> authorities) {
        Person.Essence p = new Person.Essence(ctx);

        p.setUsername(username);
        p.setAuthorities(authorities);

        return p.createUserDetails();

    }

    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        Assert.isInstanceOf(Person.class, user, "UserDetails must be a Person instance");

        Person p = (Person) user;
        p.populateContext(ctx);
    }
}
