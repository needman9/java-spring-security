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

package net.sf.acegisecurity.intercept.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;


/**
 * Outputs interceptor-related application events to Commons Logging.
 * 
 * <P>
 * All failures are logged at the warning level, with success events logged at
 * the information level, and public invocation events logged at the debug
 * level.
 * </p>
 *
 * @author Ben Alex
 * @version $Id$
 */
public class LoggerListener implements ApplicationListener {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(LoggerListener.class);

    //~ Methods ================================================================

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AuthenticationCredentialsNotFoundEvent) {
            AuthenticationCredentialsNotFoundEvent authEvent = (AuthenticationCredentialsNotFoundEvent) event;

            if (logger.isWarnEnabled()) {
                logger.warn("Security interception failed due to: "
                    + authEvent.getCredentialsNotFoundException()
                    + "; secure object: " + authEvent.getSource()
                    + "; configuration attributes: "
                    + authEvent.getConfigAttributeDefinition());
            }
        }

        if (event instanceof AuthenticationFailureEvent) {
            AuthenticationFailureEvent authEvent = (AuthenticationFailureEvent) event;

            if (logger.isWarnEnabled()) {
                logger.warn("Security authentication failed due to: "
                    + authEvent.getAuthenticationException()
                    + "; for authentication request: "
                    + authEvent.getAuthentication() + "; secure object: "
                    + authEvent.getSource() + "; configuration attributes: "
                    + authEvent.getConfigAttributeDefinition());
            }
        }

        if (event instanceof AuthorizationFailureEvent) {
            AuthorizationFailureEvent authEvent = (AuthorizationFailureEvent) event;

            if (logger.isWarnEnabled()) {
                logger.warn("Security authorization failed due to: "
                    + authEvent.getAccessDeniedException()
                    + "; authenticated principal: "
                    + authEvent.getAuthentication() + "; secure object: "
                    + authEvent.getSource() + "; configuration attributes: "
                    + authEvent.getConfigAttributeDefinition());
            }
        }

        if (event instanceof AuthorizedEvent) {
            AuthorizedEvent authEvent = (AuthorizedEvent) event;

            if (logger.isInfoEnabled()) {
                logger.info("Security authorized for authenticated principal: "
                    + authEvent.getAuthentication() + "; secure object: "
                    + authEvent.getSource() + "; configuration attributes: "
                    + authEvent.getConfigAttributeDefinition());
            }
        }

        if (event instanceof PublicInvocationEvent) {
            PublicInvocationEvent authEvent = (PublicInvocationEvent) event;

            if (logger.isInfoEnabled()) {
                logger.info(
                    "Security interception not required for public secure object: "
                    + authEvent.getSource());
            }
        }
    }
}
