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

package org.springframework.security.providers.rememberme;

import org.springframework.security.SpringSecurityMessageSource;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;

import org.springframework.security.providers.AuthenticationProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;

import org.springframework.util.Assert;


/**
 * An {@link AuthenticationProvider} implementation that validates {@link
 * org.springframework.security.providers.rememberme.RememberMeAuthenticationToken}s.<p>To be successfully validated, the
 * {@link org.springframework.security.providers.rememberme.RememberMeAuthenticationToken#getKeyHash()} must match this class'
 * {@link #getKey()}.</p>
 */
public class RememberMeAuthenticationProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(RememberMeAuthenticationProvider.class);

    //~ Instance fields ================================================================================================

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private String key;

    //~ Methods ========================================================================================================

	public void afterPropertiesSet() throws Exception {
        Assert.hasLength(key);
        Assert.notNull(this.messages, "A message source must be set");
    }

    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }

        if (this.key.hashCode() != ((RememberMeAuthenticationToken) authentication).getKeyHash()) {
            throw new BadCredentialsException(messages.getMessage("RememberMeAuthenticationProvider.incorrectKey",
                    "The presented RememberMeAuthenticationToken does not contain the expected key"));
        }

        return authentication;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public boolean supports(Class authentication) {
        return (RememberMeAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
