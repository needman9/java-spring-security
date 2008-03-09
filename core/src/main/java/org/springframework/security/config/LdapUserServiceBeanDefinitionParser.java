package org.springframework.security.config;

import org.springframework.security.userdetails.ldap.LdapUserDetailsService;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;

/**
 * @author Luke Taylor
 * @version $Id$
 * @since 2.0
 */
public class LdapUserServiceBeanDefinitionParser extends AbstractUserDetailsServiceBeanDefinitionParser {
    public static final String ATT_SERVER = "server-ref";
    public static final String ATT_USER_SEARCH_FILTER = "user-search-filter";
    public static final String ATT_USER_SEARCH_BASE = "user-search-base";
    public static final String DEF_USER_SEARCH_BASE = "";

    public static final String ATT_GROUP_SEARCH_FILTER = "group-search-filter";
    public static final String ATT_GROUP_SEARCH_BASE = "group-search-base";
    public static final String ATT_GROUP_ROLE_ATTRIBUTE = "group-role-attribute";    
    public static final String DEF_GROUP_SEARCH_FILTER = "(uniqueMember={0})";
    public static final String DEF_GROUP_SEARCH_BASE = "ou=groups";

    protected Class getBeanClass(Element element) {
        return LdapUserDetailsService.class;
    }

    protected void doParse(Element elt, ParserContext parserContext, BeanDefinitionBuilder builder) {

        if (!StringUtils.hasText(elt.getAttribute(ATT_USER_SEARCH_FILTER))) {
            parserContext.getReaderContext().error("User search filter must be supplied", elt);
        }
        
        builder.addConstructorArg(parseSearchBean(elt, parserContext));
        builder.addConstructorArg(parseAuthoritiesPopulator(elt, parserContext));

        LdapConfigUtils.registerPostProcessorIfNecessary(parserContext.getRegistry());
    }
    
    static RootBeanDefinition parseSearchBean(Element elt, ParserContext parserContext) {
        String userSearchFilter = elt.getAttribute(ATT_USER_SEARCH_FILTER);
        String userSearchBase = elt.getAttribute(ATT_USER_SEARCH_BASE);
        Object source = parserContext.extractSource(elt);
        
        if (StringUtils.hasText(userSearchBase)) {
            if(!StringUtils.hasText(userSearchFilter)) {
                parserContext.getReaderContext().error(ATT_USER_SEARCH_BASE + " cannot be used without a " + ATT_USER_SEARCH_FILTER, source);
            }
        } else {
            userSearchBase = DEF_USER_SEARCH_BASE;
        }        
        
        if (!StringUtils.hasText(userSearchFilter)) {
            return null;
        }
        
        RootBeanDefinition search = new RootBeanDefinition(FilterBasedLdapUserSearch.class);
        search.setSource(source);
        search.getConstructorArgumentValues().addIndexedArgumentValue(0, userSearchBase);
        search.getConstructorArgumentValues().addIndexedArgumentValue(1, userSearchFilter);
        search.getConstructorArgumentValues().addIndexedArgumentValue(2, parseServerReference(elt, parserContext));
        
        return search;
    }
    
    static RuntimeBeanReference parseServerReference(Element elt, ParserContext parserContext) {
        String server = elt.getAttribute(ATT_SERVER);

        if (!StringUtils.hasText(server)) {
            server = BeanIds.CONTEXT_SOURCE;
        }

        RuntimeBeanReference contextSource = new RuntimeBeanReference(server);
        contextSource.setSource(parserContext.extractSource(elt));
        
        return contextSource;
    }
    
    static RootBeanDefinition parseAuthoritiesPopulator(Element elt, ParserContext parserContext) {
        String groupSearchFilter = elt.getAttribute(ATT_GROUP_SEARCH_FILTER);
        String groupSearchBase = elt.getAttribute(ATT_GROUP_SEARCH_BASE);
        String groupRoleAttribute = elt.getAttribute(ATT_GROUP_ROLE_ATTRIBUTE);

        if (!StringUtils.hasText(groupSearchFilter)) {
            groupSearchFilter = DEF_GROUP_SEARCH_FILTER;
        }

        if (!StringUtils.hasText(groupSearchBase)) {
            groupSearchBase = DEF_GROUP_SEARCH_BASE;
        }
        
        RootBeanDefinition populator = new RootBeanDefinition(DefaultLdapAuthoritiesPopulator.class);
        populator.setSource(parserContext.extractSource(elt));
        populator.getConstructorArgumentValues().addIndexedArgumentValue(0, parseServerReference(elt, parserContext));
        populator.getConstructorArgumentValues().addIndexedArgumentValue(1, groupSearchBase);
        populator.getPropertyValues().addPropertyValue("groupSearchFilter", groupSearchFilter);
        
        if (StringUtils.hasLength(groupRoleAttribute)) {
            populator.getPropertyValues().addPropertyValue("groupRoleAttribute", groupRoleAttribute);
        }
        
        return populator;
    }
}
