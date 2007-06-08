/**
 * 
 */
package org.acegisecurity.config;

import java.lang.reflect.Field;
import java.util.Map;

import junit.framework.TestCase;

import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.ui.rememberme.RememberMeProcessingFilter;
import org.acegisecurity.ui.rememberme.RememberMeServices;
import org.acegisecurity.ui.rememberme.TokenBasedRememberMeServices;
import org.acegisecurity.userdetails.UserDetailsService;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Vishal Puri
 *
 */
public class RememberMeServicesBeanDefinitionParserTests extends TestCase {
	
	public void testFilterConfiguration() {
		ApplicationContext context = new ClassPathXmlApplicationContext("org/acegisecurity/config/remember-me-defaults.xml");
		ConfigurableListableBeanFactory bf = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		String[] names = bf.getBeanNamesForType(RememberMeProcessingFilter.class);
		assertEquals(1, names.length);
		RootBeanDefinition definition = (RootBeanDefinition)bf.getBeanDefinition(names[0]);
		assertEquals(definition.getBeanClass(), RememberMeProcessingFilter.class);
	}
	
	public void testRuntimeAutoDetectionOfDependencies() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("org/acegisecurity/config/remember-me-autodetect.xml");
		ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory();
		Map map = factory.getBeansOfType(RememberMeProcessingFilter.class);
		RememberMeProcessingFilter filter = (RememberMeProcessingFilter)map.values().iterator().next();
		RememberMeServices services = filter.getRememberMeServices();
		assertNotNull(services);
		TokenBasedRememberMeServices rememberMeServices = (TokenBasedRememberMeServices)services;
		UserDetailsService ud = rememberMeServices.getUserDetailsService();
		assertNotNull(ud);
		Field field = filter.getClass().getDeclaredField("authenticationManager");
		ReflectionUtils.makeAccessible(field);
		Object obj = field.get(filter);
		assertNotNull("authentication manager should not have been null", obj);
		assertTrue(obj instanceof AuthenticationManager);
	}

}
