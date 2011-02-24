/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.document.mongodb.repository;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.data.document.mongodb.MongoPropertyDescriptors.MongoPropertyDescriptor;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.support.EntityMetadata;
import org.springframework.data.repository.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.RepositoryMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create {@link MongoRepository} instances.
 * 
 * @author Oliver Gierke
 */
public class MongoRepositoryFactoryBean extends RepositoryFactoryBeanSupport<MongoRepository<?, ?>> {

	private MongoTemplate template;

	/**
	 * Configures the {@link MongoTemplate} to be used.
	 * 
	 * @param template the template to set
	 */
	public void setTemplate(MongoTemplate template) {

		this.template = template;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.support.RepositoryFactoryBeanSupport #createRepositoryFactory()
	 */
	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {

		return new MongoRepositoryFactory(template);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.support.RepositoryFactoryBeanSupport #afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		super.afterPropertiesSet();
		Assert.notNull(template, "MongoTemplate must not be null!");
	}

	/**
	 * Repository to create {@link MongoRepository} instances.
	 * 
	 * @author Oliver Gierke
	 */
	public static class MongoRepositoryFactory extends RepositoryFactorySupport {

		private final MongoTemplate template;

		/**
		 * Creates a new {@link MongoRepositoryFactory} fwith the given {@link MongoTemplate}.
		 * 
		 * @param template
		 */
		public MongoRepositoryFactory(MongoTemplate template) {

			this.template = template;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Object getTargetRepository(RepositoryMetadata metadata) {

			EntityMetadata<Object> info = new MongoEntityMetadata<Object>((Class<Object>) metadata.getDomainClass());
			return new SimpleMongoRepository<Object, Serializable>(info, template);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.repository.support.RepositoryFactorySupport#getRepositoryBaseClass()
		 */
		@Override
		protected Class<?> getRepositoryBaseClass(Class<?> repositoryInterface) {
			return SimpleMongoRepository.class;
		}

		@Override
		protected QueryLookupStrategy getQueryLookupStrategy(Key key) {

			return new MongoQueryLookupStrategy();
		}

		/**
		 * {@link QueryLookupStrategy} to create {@link PartTreeMongoQuery} instances.
		 * 
		 * @author Oliver Gierke
		 */
		private class MongoQueryLookupStrategy implements QueryLookupStrategy {

			public RepositoryQuery resolveQuery(Method method) {

				MongoQueryMethod queryMethod = new MongoQueryMethod(method);

				if (queryMethod.hasAnnotatedQuery()) {
					return new StringBasedMongoQuery(queryMethod, template);
				} else {
					return new PartTreeMongoQuery(queryMethod, template);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.repository.support.RepositoryFactorySupport#validate(java.lang.Class,
		 * java.lang.Object)
		 */
		@Override
		protected void validate(RepositoryMetadata metadata, Object customImplementation) {

			Class<?> idClass = metadata.getIdClass();
			if (!MongoPropertyDescriptor.SUPPORTED_ID_CLASSES.contains(idClass)) {
				throw new IllegalArgumentException(String.format("Unsupported id class! Only %s are supported!",
						StringUtils.collectionToCommaDelimitedString(MongoPropertyDescriptor.SUPPORTED_ID_CLASSES)));
			}

			super.validate(metadata, customImplementation);
		}
	}
}
