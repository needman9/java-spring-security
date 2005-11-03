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

package net.sf.acegisecurity.providers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import net.sf.acegisecurity.AbstractAuthenticationManager;
import net.sf.acegisecurity.AccountExpiredException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.AuthenticationServiceException;
import net.sf.acegisecurity.BadCredentialsException;
import net.sf.acegisecurity.CredentialsExpiredException;
import net.sf.acegisecurity.DisabledException;
import net.sf.acegisecurity.LockedException;
import net.sf.acegisecurity.concurrent.ConcurrentLoginException;
import net.sf.acegisecurity.concurrent.ConcurrentSessionController;
import net.sf.acegisecurity.concurrent.NullConcurrentSessionController;
import net.sf.acegisecurity.event.authentication.AbstractAuthenticationEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureBadCredentialsEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureConcurrentLoginEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureCredentialsExpiredEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureDisabledEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureExpiredEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureLockedEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureProviderNotFoundEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureProxyUntrustedEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationFailureServiceExceptionEvent;
import net.sf.acegisecurity.event.authentication.AuthenticationSuccessEvent;
import net.sf.acegisecurity.providers.cas.ProxyUntrustedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;


/**
 * Iterates an {@link Authentication} request through a list of {@link
 * AuthenticationProvider}s. Can optionally be configured with a {@link
 * ConcurrentSessionController} to limit the number of sessions a user can
 * have.
 * 
 * <p>
 * <code>AuthenticationProvider</code>s are tried in order until one provides a
 * non-null response. A non-null response indicates the provider had authority
 * to decide on the authentication request and no further providers are tried.
 * If an <code>AuthenticationException</code> is thrown by a provider, it is
 * retained until subsequent providers are tried. If a subsequent provider
 * successfully authenticates the request, the earlier authentication
 * exception is disregarded and the successful authentication will be used. If
 * no subsequent provider provides a non-null response, or a new
 * <code>AuthenticationException</code>, the last
 * <code>AuthenticationException</code> received will be used. If no provider
 * returns a non-null response, or indicates it can even process an
 * <code>Authentication</code>, the <code>ProviderManager</code> will throw a
 * <code>ProviderNotFoundException</code>.
 * </p>
 * 
 * <p>If a valid <code>Authentication</code> is returned by an <code>AuthenticationProvider</code>,
 * the <code>ProviderManager</code> will publish an
 * {@link net.sf.acegisecurity.event.authentication.AuthenticationSuccessEvent}. If an
 * <code>AuthenticationException</code> is detected, the final <code>AuthenticationException</code> thrown
 * will be used to publish an appropriate failure event. By default <code>ProviderManager</code>
 * maps common exceptions to events, but this can be fine-tuned by providing a new
 * <code>exceptionMappings</code> <code>java.util.Properties</code> object. In the
 * properties object, each of the keys represent the fully qualified classname of
 * the exception, and each of the values represent the name of an event class which subclasses
 * {@link net.sf.acegisecurity.event.authentication.AbstractAuthenticationFailureEvent} and
 * provides its constructor.
 *
 * @author Ben Alex
 * @author Wesley Hall
 * @author Ray Krueger
 * @version $Id$
 *
 * @see ConcurrentSessionController
 */
public class ProviderManager extends AbstractAuthenticationManager
    implements InitializingBean, ApplicationEventPublisherAware {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(ProviderManager.class);

    //~ Instance fields ========================================================

    private ConcurrentSessionController sessionController = new NullConcurrentSessionController();
    private List providers;
    private Properties exceptionMappings;
    private ApplicationEventPublisher applicationEventPublisher;

    //~ Methods ================================================================

    /**
     * Sets the {@link AuthenticationProvider} objects to be used for
     * authentication.
     *
     * @param newList
     *
     * @throws IllegalArgumentException DOCUMENT ME!
     */
    public void setProviders(List newList) {
        checkIfValidList(newList);

        Iterator iter = newList.iterator();

        while (iter.hasNext()) {
            Object currentObject = null;

            try {
                currentObject = iter.next();

                AuthenticationProvider attemptToCast = (AuthenticationProvider) currentObject;
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("AuthenticationProvider "
                    + currentObject.getClass().getName()
                    + " must implement AuthenticationProvider");
            }
        }

        this.providers = newList;
    }

    public List getProviders() {
        return this.providers;
    }

    /**
     * Set the {@link ConcurrentSessionController} to be used for limiting
     * user's sessions.  The {@link NullConcurrentSessionController} is used
     * by default
     *
     * @param sessionController {@link ConcurrentSessionController}
     */
    public void setSessionController(
        ConcurrentSessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * The configured {@link ConcurrentSessionController} is returned or the
     * {@link NullConcurrentSessionController} if a specific one has not been
     * set.
     *
     * @return {@link ConcurrentSessionController} instance
     */
    public ConcurrentSessionController getSessionController() {
        return sessionController;
    }

    public void afterPropertiesSet() throws Exception {
        checkIfValidList(this.providers);
        if (exceptionMappings == null) {
        	exceptionMappings = new Properties();
        	exceptionMappings.put(AccountExpiredException.class.getName(), AuthenticationFailureExpiredEvent.class.getName());
        	exceptionMappings.put(AuthenticationServiceException.class.getName(), AuthenticationFailureServiceExceptionEvent.class.getName());
        	exceptionMappings.put(LockedException.class.getName(), AuthenticationFailureLockedEvent.class.getName());
        	exceptionMappings.put(CredentialsExpiredException.class.getName(), AuthenticationFailureCredentialsExpiredEvent.class.getName());
        	exceptionMappings.put(DisabledException.class.getName(), AuthenticationFailureDisabledEvent.class.getName());
        	exceptionMappings.put(BadCredentialsException.class.getName(), AuthenticationFailureBadCredentialsEvent.class.getName());
        	exceptionMappings.put(ConcurrentLoginException.class.getName(), AuthenticationFailureConcurrentLoginEvent.class.getName());
        	exceptionMappings.put(ProviderNotFoundException.class.getName(), AuthenticationFailureProviderNotFoundEvent.class.getName());
        	exceptionMappings.put(ProxyUntrustedException.class.getName(), AuthenticationFailureProxyUntrustedEvent.class.getName());
        	doAddExtraDefaultExceptionMappings(exceptionMappings);
        }
    }
    
    /**
     * Provided so subclasses can add extra exception mappings during startup if no
     * exception mappings are injected by the IoC container.
     * 
     * @param exceptionMappings the properties object, which already has entries in it
     */
    protected void doAddExtraDefaultExceptionMappings(Properties exceptionMappings) {}

    /**
     * Attempts to authenticate the passed {@link Authentication} object.
     * 
     * <p>
     * The list of {@link AuthenticationProvider}s will be successively tried
     * until an <code>AuthenticationProvider</code> indicates it is  capable
     * of authenticating the type of <code>Authentication</code> object
     * passed. Authentication will then be attempted with that
     * <code>AuthenticationProvider</code>.
     * </p>
     * 
     * <p>
     * If more than one <code>AuthenticationProvider</code> supports the passed
     * <code>Authentication</code> object, only the first
     * <code>AuthenticationProvider</code> tried will determine the result. No
     * subsequent <code>AuthenticationProvider</code>s will be tried.
     * </p>
     *
     * @param authentication the authentication request object.
     *
     * @return a fully authenticated object including credentials.
     *
     * @throws AuthenticationException if authentication fails.
     * @throws ProviderNotFoundException DOCUMENT ME!
     */
    public Authentication doAuthentication(Authentication authentication)
        throws AuthenticationException {
        Iterator iter = providers.iterator();

        Class toTest = authentication.getClass();

        sessionController.checkAuthenticationAllowed(authentication);

        AuthenticationException lastException = null;

        while (iter.hasNext()) {
            AuthenticationProvider provider = (AuthenticationProvider) iter
                .next();

            if (provider.supports(toTest)) {
                logger.debug("Authentication attempt using "
                    + provider.getClass().getName());

                Authentication result = null;

                try {
                    result = provider.authenticate(authentication);
                } catch (AuthenticationException ae) {
                    lastException = ae;
                }

                if (result != null) {
                    sessionController.registerSuccessfulAuthentication(result);
                    applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(result));
                    return result;
                }
            }
        }

        if (lastException == null) {
        	lastException = new ProviderNotFoundException("No authentication provider for " + toTest.getName());
        }

    	// Publish the event
        String className = exceptionMappings.getProperty(lastException.getClass().getName());
        AbstractAuthenticationEvent event = null;
        if (className != null) {
        	try {
        		Class clazz = getClass().getClassLoader().loadClass(className);
        		Constructor constructor = clazz.getConstructor(new Class[] {Authentication.class, AuthenticationException.class});
        		Object obj = constructor.newInstance(new Object[] {authentication, lastException});
        		Assert.isInstanceOf(AbstractAuthenticationEvent.class, obj, "Must be an AbstractAuthenticationEvent");
        		event = (AbstractAuthenticationEvent) obj;
        	} catch (ClassNotFoundException ignored) {
        	} catch (NoSuchMethodException ignored) {
        	} catch (IllegalAccessException ignored) {
        	} catch (InstantiationException ignored) {
        	} catch (InvocationTargetException ignored) {
        	}
        }
        Assert.notNull(event, "A valid event must be available for the exception " + lastException.getClass().getName());
        applicationEventPublisher.publishEvent(event);
        
        // Throw the exception
        throw lastException;
    }

    private void checkIfValidList(List listToCheck) {
        if ((listToCheck == null) || (listToCheck.size() == 0)) {
            throw new IllegalArgumentException(
                "A list of AuthenticationManagers is required");
        }
    }

	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;		
	}
}
