/* Copyright 2004, 2005 Acegi Technology Pty Limited
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

package org.acegisecurity.userdetails.memory;

import java.util.List;
import java.util.Vector;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;


/**
 * Used by {@link InMemoryDaoImpl} to temporarily store the attributes
 * associated with a user.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class UserAttribute {
    //~ Instance fields ========================================================

    private List authorities = new Vector();
    private String password;
    private boolean enabled = true;

    //~ Constructors ===========================================================

    public UserAttribute() {
        super();
    }

    //~ Methods ================================================================

    public GrantedAuthority[] getAuthorities() {
        GrantedAuthority[] toReturn = {new GrantedAuthorityImpl("demo")};

        return (GrantedAuthority[]) this.authorities.toArray(toReturn);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public boolean isValid() {
        if ((this.password != null) && (authorities.size() > 0)) {
            return true;
        } else {
            return false;
        }
    }

    public void addAuthority(GrantedAuthority newAuthority) {
        this.authorities.add(newAuthority);
    }
}
