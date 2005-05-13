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

package net.sf.acegisecurity.domain.hibernate;

import net.sf.acegisecurity.domain.validation.IntrospectionManager;
import net.sf.acegisecurity.domain.validation.ValidationRegistryManager;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

import org.hibernate.metadata.ClassMetadata;

import org.hibernate.type.Type;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.orm.hibernate3.HibernateSystemException;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * {@link IntrospectionManager} that uses Hibernate metadata to locate
 * children.
 * 
 * <p>
 * Add children objects are added to the <code>List</code> of children objects
 * to validate, irrespective of whether a save/update/delete operation will
 * cascade to them. This is not a perfect solution, but addresses most
 * real-world validation requirements (you can always implement your own
 * <code>IntrospectionManager</code> if you prefer).
 * </p>
 * 
 * <p>
 * This implementation only adds properties of a parent object that have a
 * Hibernate {@link net.sf.hibernate.type.Type} that indicates it is an object
 * type (ie {@link net.sf.hibernate.type.Type#isObjectType()}).
 * </p>
 *
 * @author Matthew Porter
 * @author Ben Alex
 */
public class IntrospectionManagerHibernate implements IntrospectionManager,
    InitializingBean {
    //~ Instance fields ========================================================

    private SessionFactory sessionFactory;
    private ValidationRegistryManager validationRegistryManager;

    //~ Methods ================================================================

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    public void setValidationRegistryManager(
        ValidationRegistryManager validationRegistryManager) {
        this.validationRegistryManager = validationRegistryManager;
    }

    public ValidationRegistryManager getValidationRegistryManager() {
        return validationRegistryManager;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(validationRegistryManager,
            "ValidationRegistryManager is required");
        Assert.notNull(sessionFactory, "SessionFactory is required");

        // Eagerly pre-register Validators for all Hibernate metadata-defined classes
		Map<String,ClassMetadata> metadataMap = this.sessionFactory.getAllClassMetadata();
        Collection<String> mappedClasses = metadataMap.keySet();

        for (Iterator<String> iter = mappedClasses.iterator(); iter.hasNext();) {
            String className = iter.next();
            this.validationRegistryManager.findValidator(Class.forName(
                    className));
        }
    }

    public void obtainImmediateChildren(Object parentObject, List<Object> allObjects) {
        Assert.notNull(parentObject,
            "Violation of interface contract: parentObject null");
        Assert.notNull(allObjects,
            "Violation of interface contract: allObjects null");

        ClassMetadata classMetadata = null;

        try {
            classMetadata = sessionFactory.getClassMetadata(parentObject
                    .getClass());

            if (classMetadata != null) {
                String[] propertyNames = classMetadata.getPropertyNames();

                for (int i = 0; i < propertyNames.length; i++) {
                    Type propertyType = classMetadata.getPropertyType(propertyNames[i]);

                    // Add this property to the List of Objects to validate
                    // only if a Validator is registered for that Object AND
					// the object is initialized (ie not lazy loaded)
                    if (this.validationRegistryManager.findValidator(
                            propertyType.getReturnedClass()) != null) {
                        Object childObject = classMetadata.getPropertyValue(parentObject, propertyNames[i], EntityMode.POJO);
                        if (childObject != null && Hibernate.isInitialized(childObject)) {
                            allObjects.add(childObject);
                        }
                    }
                }
            }
        } catch (HibernateException he) {
            throw new HibernateSystemException(he);
        }
    }
}
