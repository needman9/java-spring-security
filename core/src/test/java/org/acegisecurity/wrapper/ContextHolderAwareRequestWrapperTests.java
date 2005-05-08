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

package net.sf.acegisecurity.wrapper;

import junit.framework.TestCase;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.context.SecurityContextHolder;
import net.sf.acegisecurity.providers.TestingAuthenticationToken;
import net.sf.acegisecurity.providers.dao.User;
import net.sf.acegisecurity.wrapper.ContextHolderAwareRequestWrapper;

import org.springframework.mock.web.MockHttpServletRequest;


/**
 * Tests {@link ContextHolderAwareRequestWrapper}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class ContextHolderAwareRequestWrapperTests extends TestCase {
    //~ Constructors ===========================================================

    public ContextHolderAwareRequestWrapperTests() {
        super();
    }

    public ContextHolderAwareRequestWrapperTests(String arg0) {
        super(arg0);
    }

    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ContextHolderAwareRequestWrapperTests.class);
    }

    public void testCorrectOperationWithStringBasedPrincipal()
        throws Exception {
        Authentication auth = new TestingAuthenticationToken("marissa",
                "koala",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_FOO")});
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/");

        ContextHolderAwareRequestWrapper wrapper = new ContextHolderAwareRequestWrapper(request);

        assertEquals("marissa", wrapper.getRemoteUser());
        assertTrue(wrapper.isUserInRole("ROLE_FOO"));
        assertFalse(wrapper.isUserInRole("ROLE_NOT_GRANTED"));
        assertEquals(auth, wrapper.getUserPrincipal());

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void testCorrectOperationWithUserDetailsBasedPrincipal()
        throws Exception {
        Authentication auth = new TestingAuthenticationToken(new User(
                    "marissaAsUserDetails", "koala", true, true, true, true,
                    new GrantedAuthority[] {}), "koala",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_HELLO"), new GrantedAuthorityImpl(
                        "ROLE_FOOBAR")});
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/");

        ContextHolderAwareRequestWrapper wrapper = new ContextHolderAwareRequestWrapper(request);

        assertEquals("marissaAsUserDetails", wrapper.getRemoteUser());
        assertFalse(wrapper.isUserInRole("ROLE_FOO"));
        assertFalse(wrapper.isUserInRole("ROLE_NOT_GRANTED"));
        assertTrue(wrapper.isUserInRole("ROLE_FOOBAR"));
        assertTrue(wrapper.isUserInRole("ROLE_HELLO"));
        assertEquals(auth, wrapper.getUserPrincipal());

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void testNullAuthenticationHandling() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/");

        ContextHolderAwareRequestWrapper wrapper = new ContextHolderAwareRequestWrapper(request);
        assertNull(wrapper.getRemoteUser());
        assertFalse(wrapper.isUserInRole("ROLE_ANY"));
        assertNull(wrapper.getUserPrincipal());

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void testNullPrincipalHandling() throws Exception {
        Authentication auth = new TestingAuthenticationToken(null, "koala",
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_HELLO"), new GrantedAuthorityImpl(
                        "ROLE_FOOBAR")});
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/");

        ContextHolderAwareRequestWrapper wrapper = new ContextHolderAwareRequestWrapper(request);

        assertNull(wrapper.getRemoteUser());
        assertFalse(wrapper.isUserInRole("ROLE_HELLO")); // principal is null, so reject
        assertFalse(wrapper.isUserInRole("ROLE_FOOBAR")); // principal is null, so reject
        assertNull(wrapper.getUserPrincipal());

        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
