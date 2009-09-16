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
package org.springframework.security.taglibs.authz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.ExpressionEvaluationUtils;


/**
 * An implementation of {@link Tag} that allows its body through if some authorizations are granted to the request's
 * principal.
 * <p>
 * One or more comma separate numeric are specified via the <tt>hasPermission</tt> attribute.
 * These permissions are then converted into {@link Permission} instances. These instances are then presented as an
 * array to the {@link Acl#isGranted(Permission[], org.springframework.security.acls.model.Sid[], boolean)} method.
 * The {@link Sid} presented is determined by the {@link SidRetrievalStrategy}.
 * <p>
 * For this class to operate it must be able to access the application context via the
 * <code>WebApplicationContextUtils</code> and attempt to locate an {@link AclService} and {@link SidRetrievalStrategy}.
 * There cannot be more than one of these present. The <tt>AclService</tt> must be provided, but a
 * {@link SidRetrievalStrategyImpl} instance will be created as the default retrieval strategy if no implementation
 * is supplied by the application context.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class AccessControlListTag extends TagSupport {
    //~ Static fields/initializers =====================================================================================

    protected static final Log logger = LogFactory.getLog(AccessControlListTag.class);

    //~ Instance fields ================================================================================================

    private AclService aclService;
    private ApplicationContext applicationContext;
    private Object domainObject;
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;
    private SidRetrievalStrategy sidRetrievalStrategy;
    private PermissionFactory permissionFactory;
    private String hasPermission = "";

    //~ Methods ========================================================================================================

    public int doStartTag() throws JspException {
        if ((null == hasPermission) || "".equals(hasPermission)) {
            return Tag.SKIP_BODY;
        }

        initializeIfRequired();

        final String evaledPermissionsString = ExpressionEvaluationUtils.evaluateString("hasPermission", hasPermission,
                pageContext);

        List<Permission> requiredPermissions = null;

        try {
            requiredPermissions = parsePermissionsString(evaledPermissionsString);
        } catch (NumberFormatException nfe) {
            throw new JspException(nfe);
        }

        Object resolvedDomainObject = null;

        if (domainObject instanceof String) {
            resolvedDomainObject = ExpressionEvaluationUtils.evaluate("domainObject", (String) domainObject,
                    Object.class, pageContext);
        } else {
            resolvedDomainObject = domainObject;
        }

        if (resolvedDomainObject == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("domainObject resolved to null, so including tag body");
            }

            // Of course they have access to a null object!
            return Tag.EVAL_BODY_INCLUDE;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "SecurityContextHolder did not return a non-null Authentication object, so skipping tag body");
            }

            return Tag.SKIP_BODY;
        }

        List<Sid> sids = sidRetrievalStrategy.getSids(SecurityContextHolder.getContext().getAuthentication());
        ObjectIdentity oid = objectIdentityRetrievalStrategy.getObjectIdentity(resolvedDomainObject);

        // Obtain aclEntrys applying to the current Authentication object
        try {
            Acl acl = aclService.readAclById(oid, sids);

            if (acl.isGranted(requiredPermissions, sids, false)) {
                return Tag.EVAL_BODY_INCLUDE;
            } else {
                return Tag.SKIP_BODY;
            }
        } catch (NotFoundException nfe) {
            return Tag.SKIP_BODY;
        }
    }

    /**
     * Allows test cases to override where application context obtained from.
     *
     * @param pageContext so the <code>ServletContext</code> can be accessed as required by Spring's
     *        <code>WebApplicationContextUtils</code>
     *
     * @return the Spring application context (never <code>null</code>)
     */
    protected ApplicationContext getContext(PageContext pageContext) {
        ServletContext servletContext = pageContext.getServletContext();

        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    public Object getDomainObject() {
        return domainObject;
    }

    public String getHasPermission() {
        return hasPermission;
    }

    private void initializeIfRequired() throws JspException {
        if (applicationContext != null) {
            return;
        }

        this.applicationContext = getContext(pageContext);

        aclService = getBeanOfType(AclService.class);

        sidRetrievalStrategy = getBeanOfType(SidRetrievalStrategy.class);

        if (sidRetrievalStrategy == null) {
            sidRetrievalStrategy = new SidRetrievalStrategyImpl();
        }

        objectIdentityRetrievalStrategy = getBeanOfType(ObjectIdentityRetrievalStrategy.class);

        if (objectIdentityRetrievalStrategy == null) {
            objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();
        }

        permissionFactory = getBeanOfType(PermissionFactory.class);

        if (permissionFactory == null) {
            permissionFactory = new DefaultPermissionFactory();
        }
    }

    private <T> T getBeanOfType(Class<T> type) throws JspException {
        Map<String, T> map = applicationContext.getBeansOfType(type);

        for (ApplicationContext context = applicationContext.getParent();
            context != null; context = context.getParent()) {
            map.putAll(context.getBeansOfType(type));
        }

        if (map.size() == 0) {
            return null;
        } else if (map.size() == 1) {
            return map.values().iterator().next();
        }

        throw new JspException("Found incorrect number of " + type.getSimpleName() +" instances in "
                    + "application context - you must have only have one!");
    }

    private List<Permission> parsePermissionsString(String integersString)
        throws NumberFormatException {
        final Set<Permission> permissions = new HashSet<Permission>();
        final StringTokenizer tokenizer;
        tokenizer = new StringTokenizer(integersString, ",", false);

        while (tokenizer.hasMoreTokens()) {
            String integer = tokenizer.nextToken();
            permissions.add(permissionFactory.buildFromMask(new Integer(integer)));
        }

        return new ArrayList<Permission>(permissions);
    }

    public void setDomainObject(Object domainObject) {
        this.domainObject = domainObject;
    }

    public void setHasPermission(String hasPermission) {
        this.hasPermission = hasPermission;
    }
}
