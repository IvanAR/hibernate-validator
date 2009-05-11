// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validation.engine.resolver;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import org.hibernate.validation.engine.HibernateValidatorConfiguration;

/**
 * @author Emmanuel Bernard
 */
//this test is specific to Hibernate Validator
public class CachedTraversableResolverTest {
	@Test
	public void testCache() {
		TraversableResolver resolver = new AskOnceTR();
		ValidatorFactory factory = Validation.byProvider( HibernateValidatorConfiguration.class )
				.configure().traversableResolver( resolver )
				.buildValidatorFactory();
		Suit suit = new Suit();
		suit.setTrousers( new Trousers() );
		suit.setJacket( new Jacket() );
		suit.setSize( 3333 );
		suit.getTrousers().setLength( 32321 );
		suit.getJacket().setWidth( 432432 );
		Validator v = factory.getValidator();
		try {
			v.validate( suit, Default.class, Cloth.class );
		}
		catch ( IllegalStateException e ) {
			fail("Traversable Called several times for a given object");
		}

		v = factory.usingContext().traversableResolver( new AskOnceTR() ).getValidator();
		try {
			v.validateProperty( suit, "size", Default.class, Cloth.class );
		}
		catch ( IllegalStateException e ) {
			fail("Traversable Called several times for a given object");
		}

		v = factory.usingContext().traversableResolver( new AskOnceTR() ).getValidator();
		try {
			v.validateValue( Suit.class, "size", 2, Default.class, Cloth.class );
		}
		catch ( IllegalStateException e ) {
			fail("Traversable Called several times for a given object");
		}
	}

	private static class AskOnceTR implements TraversableResolver {
		private Set<Holder> askedReach = new HashSet<Holder>();
		private Set<Holder> askedCascade = new HashSet<Holder>();

		private boolean isTraversable(Set<Holder> asked, Object traversableObject, String traversableProperty, Class<?> rootBeanType, String pathToTraversableObject, ElementType elementType) {
			Holder h = new Holder(traversableObject, traversableProperty);
			if ( asked.contains( h )) throw new IllegalStateException( "Called twice" );
			asked.add( h );
			return true;
		}

		public boolean isReachable(Object traversableObject, String traversableProperty, Class<?> rootBeanType, String pathToTraversableObject, ElementType elementType) {
			return isTraversable( askedReach, traversableObject, traversableProperty, rootBeanType, pathToTraversableObject,elementType );
		}

		public boolean isCascadable(Object traversableObject, String traversableProperty, Class<?> rootBeanType, String pathToTraversableObject, ElementType elementType) {
			return isTraversable( askedCascade, traversableObject, traversableProperty, rootBeanType, pathToTraversableObject,elementType );
		}

		public static class Holder {
			Object NULL = new Object();
			Object to;
			String tp;

			public Holder(Object traversableObject, String traversableProperty) {
				to = traversableObject == null ? NULL : traversableObject;
				tp = traversableProperty;
			}

			@Override
			public int hashCode() {
				return to.hashCode() + tp.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if ( ! ( obj instanceof Holder ) ) return false;
				Holder that = (Holder) obj;

				return to != NULL && to == that.to && tp.equals( that.tp );
			}
		}
	}
}
