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

package net.sf.acegisecurity.taglibs.authz;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.context.ContextHolder;
import net.sf.acegisecurity.context.security.SecureContext;

import org.springframework.web.util.ExpressionEvaluationUtils;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;


/**
 * An implementation of {@link javax.servlet.jsp.tagext.Tag} that allows it's
 * body through if some authorizations are granted to the request's principal.
 *
 * @author Francois Beausoleil
 * @version $Id$
 */
public class AuthorizeTag extends TagSupport {
    //~ Instance fields ========================================================

    private String ifAllGranted = "";
    private String ifAnyGranted = "";
    private String ifNotGranted = "";

    //~ Methods ================================================================

    public void setIfAllGranted(String ifAllGranted) throws JspException {
        this.ifAllGranted = ifAllGranted;
    }

    public String getIfAllGranted() {
        return ifAllGranted;
    }

    public void setIfAnyGranted(String ifAnyGranted) throws JspException {
        this.ifAnyGranted = ifAnyGranted;
    }

    public String getIfAnyGranted() {
        return ifAnyGranted;
    }

    public void setIfNotGranted(String ifNotGranted) throws JspException {
        this.ifNotGranted = ifNotGranted;
    }

    public String getIfNotGranted() {
        return ifNotGranted;
    }

    public int doStartTag() throws JspException {
        if (((null == ifAllGranted) || "".equals(ifAllGranted))
            && ((null == ifAnyGranted) || "".equals(ifAnyGranted))
            && ((null == ifNotGranted) || "".equals(ifNotGranted))) {
            return Tag.SKIP_BODY;
        }

        final Collection granted = getPrincipalAuthorities();

        final String evaledIfNotGranted = ExpressionEvaluationUtils
            .evaluateString("ifNotGranted", ifNotGranted, pageContext);

        if ((null != evaledIfNotGranted) && !"".equals(evaledIfNotGranted)) {
            Set grantedCopy = retainAll(granted,
                    parseAuthoritiesString(evaledIfNotGranted));

            if (!grantedCopy.isEmpty()) {
                return Tag.SKIP_BODY;
            }
        }

        final String evaledIfAllGranted = ExpressionEvaluationUtils
            .evaluateString("ifAllGranted", ifAllGranted, pageContext);

        if ((null != evaledIfAllGranted) && !"".equals(evaledIfAllGranted)) {
            if (!granted.containsAll(parseAuthoritiesString(evaledIfAllGranted))) {
                return Tag.SKIP_BODY;
            }
        }

        final String evaledIfAnyGranted = ExpressionEvaluationUtils
            .evaluateString("ifAnyGranted", ifAnyGranted, pageContext);

        if ((null != evaledIfAnyGranted) && !"".equals(evaledIfAnyGranted)) {
            Set grantedCopy = retainAll(granted,
                    parseAuthoritiesString(evaledIfAnyGranted));

            if (grantedCopy.isEmpty()) {
                return Tag.SKIP_BODY;
            }
        }

        return Tag.EVAL_BODY_INCLUDE;
    }

    private Collection getPrincipalAuthorities() {
        SecureContext context = ((SecureContext) ContextHolder.getContext());

        if (null == context) {
            return Collections.EMPTY_LIST;
        }

        Authentication currentUser = context.getAuthentication();

        if (null == currentUser) {
            return Collections.EMPTY_LIST;
        }

        Collection granted = Arrays.asList(currentUser.getAuthorities());

        return granted;
    }

    private Set parseAuthoritiesString(String authorizationsString) {
        final Set requiredAuthorities = new HashSet();
        final StringTokenizer tokenizer;
        tokenizer = new StringTokenizer(authorizationsString, ",", false);

        while (tokenizer.hasMoreTokens()) {
            String role = tokenizer.nextToken();
            requiredAuthorities.add(new GrantedAuthorityImpl(role.trim()));
        }

        return requiredAuthorities;
    }

    private Set retainAll(final Collection granted,
        final Set requiredAuthorities) {
        Set grantedCopy = new HashSet(granted);
        grantedCopy.retainAll(requiredAuthorities);

        return grantedCopy;
    }
}
