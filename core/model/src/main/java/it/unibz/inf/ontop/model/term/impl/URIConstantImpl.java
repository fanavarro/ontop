package it.unibz.inf.ontop.model.term.impl;

/*
 * #%L
 * ontop-obdalib-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
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
 * #L%
 */

import it.unibz.inf.ontop.model.term.functionsymbol.Predicate.COL_TYPE;
import it.unibz.inf.ontop.model.term.URIConstant;
import it.unibz.inf.ontop.model.term.Variable;

import java.util.stream.Stream;


/**
 * Provides a storage to put the URI constant.
 */
public class URIConstantImpl implements URIConstant {

	private static final long serialVersionUID = -1263974895010238519L;
	
	private final int identifier;
	private final String iristr;

	public URIConstantImpl(String iri) {
		this.iristr = iri;
		this.identifier = iri.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof URIConstantImpl)) {
			return false;
		}
		URIConstantImpl uri2 = (URIConstantImpl) obj;
		return this.identifier == uri2.identifier;
	}

	@Override
	public int hashCode() {
		return identifier;
	}

	@Override
	public String getURI() {
		return iristr;
	}

	@Override
	public URIConstant clone() {
		return this;
	}

	@Override
	public boolean isGround() {
		return true;
	}

	@Override
	public Stream<Variable> getVariableStream() {
		return Stream.of();
	}

	@Override
	public String toString() {
		return "<" + iristr + ">";
	}

	@Override
	public COL_TYPE getType() {
		return COL_TYPE.OBJECT;
	}


	public String getName() {
		return iristr;
	}

	@Deprecated
	public String getValue() {
		return iristr;
	}
}
