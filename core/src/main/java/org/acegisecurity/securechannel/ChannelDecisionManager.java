/* Copyright 2004 Acegi Technology Pty Limited
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

package net.sf.acegisecurity.securechannel;

import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.intercept.web.FilterInvocation;

import java.io.IOException;

import javax.servlet.ServletException;


/**
 * Decides whether a web channel provides sufficient security.
 * 
 * <P>
 * If necessary due to the nature of the redirection, implementations should
 * store the original destination of the request in {@link
 * net.sf.acegisecurity.ui.AbstractProcessingFilter#ACEGI_SECURITY_TARGET_URL_KEY}.
 * </p>
 *
 * @author Ben Alex
 * @version $Id$
 */
public interface ChannelDecisionManager {
    //~ Methods ================================================================

    /**
     * Decided whether the presented {@link FilterInvocation} provides the
     * appropriate level of channel security based on the requested {@link
     * ConfigAttributeDefinition}.
     */
    public void decide(FilterInvocation invocation,
        ConfigAttributeDefinition config) throws IOException, ServletException;

    /**
     * Indicates whether this <code>ChannelDecisionManager</code> is able to
     * process the passed <code>ConfigAttribute</code>.
     * 
     * <p>
     * This allows the <code>ChannelProcessingFilter</code> to check every
     * configuration attribute can be consumed by the configured
     * <code>ChannelDecisionManager</code>.
     * </p>
     *
     * @param attribute a configuration attribute that has been configured
     *        against the <code>ChannelProcessingFilter</code>
     *
     * @return true if this <code>ChannelDecisionManager</code> can support the
     *         passed configuration attribute
     */
    public boolean supports(ConfigAttribute attribute);
}
