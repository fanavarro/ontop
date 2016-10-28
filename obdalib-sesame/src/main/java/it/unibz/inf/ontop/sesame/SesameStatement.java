package it.unibz.inf.ontop.sesame;

/*
 * #%L
 * ontop-obdalib-sesame
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

import it.unibz.inf.ontop.model.ObjectConstant;
import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.ValueConstant;
import it.unibz.inf.ontop.model.impl.OBDAVocabulary;
import it.unibz.inf.ontop.ontology.Assertion;
import it.unibz.inf.ontop.ontology.ClassAssertion;
import it.unibz.inf.ontop.ontology.DataPropertyAssertion;
import it.unibz.inf.ontop.ontology.ObjectPropertyAssertion;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;

public class SesameStatement extends StatementImpl {

    private static final long serialVersionUID = 3398547980791013746L;
    
	private Resource subject = null;
	private URI predicate = null;
	private Value object = null;
	private Resource context = null;

	private static SesameHelper HELPER = new SesameHelper();


	public static SesameStatement getInstance(Assertion assertion) {
		if (assertion instanceof ObjectPropertyAssertion) {
			return new SesameStatement((ObjectPropertyAssertion) assertion);
		} else if (assertion instanceof DataPropertyAssertion) {
			return new SesameStatement((DataPropertyAssertion) assertion);
		} else if (assertion instanceof ClassAssertion) {
			return new SesameStatement((ClassAssertion) assertion);
		} else {
			throw new RuntimeException("");
		}
	}

	private SesameStatement(ObjectPropertyAssertion assertion) {
		super(HELPER.getResource(assertion.getSubject()),
				HELPER.createURI(assertion.getProperty().getPredicate().getName().toString()),
				HELPER.getResource(assertion.getObject()));
	}

	private SesameStatement(DataPropertyAssertion assertion) {
		super(HELPER.getResource(assertion.getSubject()),
				HELPER.createURI(assertion.getProperty().getPredicate().getName().toString()),
				HELPER.getLiteral(assertion.getValue())
				);

		if (!(assertion.getValue() instanceof ValueConstant)) {
			throw new RuntimeException("Invalid constant as object!" + assertion.getValue());
		}
	}

	private SesameStatement(ClassAssertion assertion) {
		super(HELPER.getResource(assertion.getIndividual()),
				HELPER.createURI(OBDAVocabulary.RDF_TYPE),
				HELPER.createURI(assertion.getConcept().getPredicate().getName().toString()));
	}


	/*
	public SesameStatement(Assertion assertion) {
		if (assertion instanceof ObjectPropertyAssertion) {
			//object or data property assertion
			ObjectPropertyAssertion ba = (ObjectPropertyAssertion) assertion;
			ObjectConstant subj = ba.getSubject();
			Predicate pred = ba.getProperty().getPredicate();
			ObjectConstant obj = ba.getObject();
			
			// convert string into respective type

			subject = HELPER.getResource(subj);
			predicate = HELPER.createURI(pred.getName().toString()); // URI
			object = HELPER.getResource(obj);
		} 
		else if (assertion instanceof DataPropertyAssertion) {
			//object or data property assertion
			DataPropertyAssertion ba = (DataPropertyAssertion) assertion;
			ObjectConstant subj = ba.getSubject();
			Predicate pred = ba.getProperty().getPredicate();
			ValueConstant obj = ba.getValue();
			
			// convert string into respective type
			subject = HELPER.getResource(subj);
			predicate = HELPER.createURI(pred.getName().toString()); // URI
			
			if (obj instanceof ValueConstant) {
				object = HELPER.getLiteral((ValueConstant) obj);
			}
			else 
				throw new RuntimeException("Invalid constant as object!" + obj);
		} 
		else if (assertion instanceof ClassAssertion) { 
			//class assertion
			ClassAssertion ua = (ClassAssertion) assertion;
			ObjectConstant subj = ua.getIndividual();
			Predicate obj = ua.getConcept().getPredicate();
			
			// convert string into respective type
			subject = HELPER.getResource(subj);
			predicate = HELPER.createURI(OBDAVocabulary.RDF_TYPE); // URI
			object = HELPER.createURI(obj.getName().toString());
		}
	}
	*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Statement)) return false;

        Statement that = (Statement) o;

        Resource thatContext = that.getContext();
        if (context != null ? !context.equals(thatContext) : thatContext != null) return false;
        Value thatObject = that.getObject();
        if (object != null ? !object.equals(thatObject) : thatObject != null) return false;
        URI thatPredicate = that.getPredicate();
        if (predicate != null ? !predicate.equals(thatPredicate) : thatPredicate != null) return false;
        Resource thatSubject = that.getSubject();
        if (subject != null ? !subject.equals(thatSubject) : thatSubject != null) return false;

        return true;
    }
}
