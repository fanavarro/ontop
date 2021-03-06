package it.unibz.inf.ontop.spec.mapping.parser.impl;

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

import com.google.common.collect.ImmutableList;
import eu.optique.r2rml.api.binding.rdf4j.RDF4JR2RMLMappingManager;
import eu.optique.r2rml.api.model.*;
import eu.optique.r2rml.api.model.impl.InvalidR2RMLMappingException;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.Term;
import it.unibz.inf.ontop.model.term.ValueConstant;
import it.unibz.inf.ontop.model.term.functionsymbol.DatatypePredicate;
import it.unibz.inf.ontop.model.term.functionsymbol.ExpressionOperation;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate.COL_TYPE;
import it.unibz.inf.ontop.model.term.impl.DatatypePredicateImpl;
import org.apache.commons.rdf.api.*;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TERM_FACTORY;
import static it.unibz.inf.ontop.model.OntopModelSingletons.TYPE_FACTORY;

public class R2RMLParser {

	List<Predicate> classPredicates;
	List<Resource> joinPredObjNodes;


    RDF4JR2RMLMappingManager mapManager;
	Logger logger = LoggerFactory.getLogger(R2RMLParser.class);

	/**
	 * empty constructor
	 */
	public R2RMLParser() {
        mapManager = RDF4JR2RMLMappingManager.getInstance();
		classPredicates = new ArrayList<Predicate>();
		joinPredObjNodes = new ArrayList<Resource>();
	}

	/**
	 * method to get the TriplesMaps from the given Graph
	 * @param myGraph - the Graph to process
	 * @return Collection<TriplesMap> - the collection of mappings
	 */
	public Collection<TriplesMap> getMappingNodes(Graph myGraph) throws InvalidR2RMLMappingException {
		return mapManager.importMappings(myGraph);
	}

	/**
	 * Get SQL query of the TriplesMap
	 * @param tm
	 * @return
	 */
	public String getSQLQuery(TriplesMap tm) {
		return tm.getLogicalTable().getSQLQuery();
	}

	/**
	 * Get classes
     * They can be retrieved only once, after retrieving everything is cleared.
	 * @return
	 */
	public List<Predicate> getClassPredicates() {
		List<Predicate> classes = new ArrayList<Predicate>();
		for (Predicate p : classPredicates)
			classes.add(p);
		classPredicates.clear();
		return classes;
	}

	/**
	 * Get predicates
	 * @param tm
	 * @return
	 */
	public Set<BlankNodeOrIRI> getPredicateObjects(TriplesMap tm) {
		Set<BlankNodeOrIRI> predobjs = new HashSet<>();
		for (PredicateObjectMap pobj : tm.getPredicateObjectMaps()) {
			for (PredicateMap pm : pobj.getPredicateMaps()) {
				BlankNodeOrIRI r = pm.getNode();
				predobjs.add(r);
			}
		}
		return predobjs;
	}

	public ImmutableTerm getSubjectAtom(TriplesMap tm) throws Exception {
		return getSubjectAtom(tm, "");
	}

	/**
	 * Get subject
	 *
	 * @param tm
	 * @param joinCond
	 * @return
	 * @throws Exception
	 */
	public ImmutableTerm getSubjectAtom(TriplesMap tm, String joinCond) throws Exception {
		ImmutableTerm subjectAtom = null;
		String subj = "";
		classPredicates.clear();

		// SUBJECT
		SubjectMap sMap = tm.getSubjectMap();

		// process template declaration
		IRI termType = sMap.getTermType();

		TermMap.TermMapType subjectTermType = sMap.getTermMapType();

		// WORKAROUND for:
		// SubjectMap.getTemplateString() throws NullPointerException when
		// template == null
		//
		Template template = sMap.getTemplate();
		if (template == null) {
			subj = null;
		} else {
			subj = sMap.getTemplateString();
		}

		if (subj != null) {
			// create uri("...",var)
			subjectAtom = getTermTypeAtom(subj, termType, joinCond);
		}

		// process column declaration
		subj = sMap.getColumn();
		if (subj != null) {
			if(template == null && (termType.equals(R2RMLVocabulary.iri))){

				subjectAtom = TERM_FACTORY.getImmutableUriTemplate(TERM_FACTORY.getVariable(subj));

			}
			else {
				// create uri("...",var)
				subjectAtom = getTermTypeAtom(subj, termType, joinCond);
			}
		}

		// process constant declaration
        // TODO(xiao): toString() is suspicious
        RDFTerm subjConstant = sMap.getConstant();
		if (subjConstant != null) {
			// create uri("...",var)
            subj = subjConstant.toString();
			subjectAtom = getURIFunction(subj, joinCond);
		}

		// process termType declaration
		// subj = sMap.getTermMapType().toString();
		// sMap.getTermType(Object.class);
		// if (subj != null) {
		//
		//
		// }

		// process class declaration
		List<IRI> classes = sMap.getClasses();
		for (Object o : classes) {
            // TODO(xiao): toString() is suspicious
            classPredicates.add(TERM_FACTORY.getClassPredicate(o.toString()));
		}

		if (subjectAtom == null)
			throw new Exception("Error in parsing the subjectMap in node "
					+ tm.toString());

		return subjectAtom;

	}

	/**
	 * Get body predicates
	 * @param pom
	 * @return
	 * @throws Exception
	 */
	public List<Predicate> getBodyPredicates(PredicateObjectMap pom) {
		List<Predicate> bodyPredicates = new ArrayList<Predicate>();

		// process PREDICATEs
		for (PredicateMap pm : pom.getPredicateMaps()) {
		    //
			String pmConstant = pm.getConstant().toString();
			if (pmConstant != null) {
				Predicate bodyPredicate = TERM_FACTORY.getPredicate(pmConstant, 2);
				bodyPredicates.add(bodyPredicate);
			}
		}
		return bodyPredicates;
	}

	/**
	 * Get body predicates with templates
	 * @param pom
	 * @return
	 * @throws Exception
	 */
	public List<ImmutableFunctionalTerm> getBodyURIPredicates(PredicateObjectMap pom) {
		List<ImmutableFunctionalTerm> predicateAtoms = new ArrayList<>();

		// process PREDICATEMAP
		for (PredicateMap pm : pom.getPredicateMaps()) {
			Template t = pm.getTemplate();
			if (t != null) {
				// create uri("...",var)
				ImmutableFunctionalTerm predicateAtom = getURIFunction(t.toString());
				predicateAtoms.add(predicateAtom);
			}

			// process column declaration
			String c = pm.getColumn();
			if (c != null) {
				ImmutableFunctionalTerm predicateAtom = getURIFunction(c);
				predicateAtoms.add(predicateAtom);
			}
		}
		return predicateAtoms;

	}

	public ImmutableTerm getObjectAtom(PredicateObjectMap pom) {
		return getObjectAtom(pom, "");
	}

	public boolean isConcat(String st) {
		int i, j;
		if ((i = st.indexOf("{")) > -1) {
			if ((j = st.lastIndexOf("{")) > i) {
				return true;
			} else if ((i > 0) || ((j > 0) && (j < (st.length() - 1)))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the object atom, it can be a constant, a column or a template
	 * 
	 * @param pom
	 * @param joinCond
	 * @return
	 * @throws Exception
	 */
	public ImmutableTerm getObjectAtom(PredicateObjectMap pom, String joinCond) {
		ImmutableTerm objectAtom = null;
		if (pom.getObjectMaps().isEmpty()) {
			return null;
		}
		ObjectMap om = pom.getObjectMap(0);

		String lan = om.getLanguageTag();
		Object datatype = om.getDatatype();

		// we check if the object map is a constant (can be a iri or a literal)
        // TODO(xiao): toString() is suspicious
        RDFTerm constantObj = om.getConstant();
		if (constantObj != null) {
			// boolean isURI = false;
			// try {
			// java.net.URI.create(obj);
			// isURI = true;
			// } catch (IllegalArgumentException e){
			//
			// }

			// if the literal has a language property or a datatype property we
			// create the function object later

			if (lan != null || datatype != null) {
				ValueConstant constantLiteral = TERM_FACTORY.getConstantLiteral(((Literal) constantObj).getLexicalForm());
				objectAtom = constantLiteral;

			} else {

				if (constantObj instanceof Literal){

					ValueConstant constantLiteral = TERM_FACTORY.getConstantLiteral(((Literal) constantObj).getLexicalForm());
					Literal constantLit1 = (Literal) constantObj;

					String lanConstant = om.getLanguageTag();
					IRI datatypeConstant = constantLit1.getDatatype();

					// we check if it is a literal with language tag

					if (lanConstant != null) {
						objectAtom = TERM_FACTORY.getImmutableTypedTerm(constantLiteral, lanConstant);
					}

					// we check if it is a typed literal
					else if (datatypeConstant != null) {
						Optional<COL_TYPE> type = TYPE_FACTORY.getDatatype(datatypeConstant.getIRIString());
						if (!type.isPresent()) {
							// throw new RuntimeException("Unsupported datatype: " +
							// datatype.toString());
							logger.warn("Unsupported datatype will not be converted: "
									+ datatypeConstant.toString());
						} else {
							objectAtom = TERM_FACTORY.getImmutableTypedTerm(constantLiteral, type.get());
						}
					}
					else {

						objectAtom = constantLiteral;
								 // .RDFS_LITERAL;
					}
                } else if (constantObj instanceof IRI){
                    objectAtom = TERM_FACTORY.getImmutableUriTemplate(TERM_FACTORY.getConstantLiteral( ((IRI) constantObj).getIRIString()));

                }
			}
		}

		// we check if the object map is a column
		// if it has a datatype or language property or its a iri we check it later
		String col = om.getColumn();
		if (col != null) {
			col = trim(col);

			if (!joinCond.isEmpty()) {
				col = joinCond + col;
			}

			objectAtom = TERM_FACTORY.getVariable(col);

		}

		// we check if the object map is a template (can be a iri, a literal or
		// a blank node)
		Template t = om.getTemplate();
		IRI typ = om.getTermType();
		boolean concat = false;
		if (t != null) {
			//we check if the template is a literal
			//then we check if the template includes concat 
			concat = isConcat(t.toString());
			if (typ.equals(R2RMLVocabulary.literal) && (concat)){
				objectAtom = getTypedFunction(t.toString(), 4, joinCond);
			}else {

				// a template can be a rr:IRI, a
				// rr:Literal or rr:BlankNode

				// if the literal has a language property or a datatype property
				// we
				// create the function object later
				if (lan != null || datatype != null) {
					String value = t.getColumnName(0);
					if (!joinCond.isEmpty()) {
						value = joinCond + value;

					}
					objectAtom = TERM_FACTORY.getVariable(value);
				} else {
					IRI type = om.getTermType();

					// we check if the template is a IRI a simple literal or a
					// blank
					// node and create the function object
					objectAtom = getTermTypeAtom(t.toString(), type, joinCond);
				}
			}
		}
		else{
			//assign iri template
			TermMap.TermMapType termMapType = om.getTermMapType();
			if(termMapType.equals(TermMap.TermMapType.CONSTANT_VALUED)){

			} else if(termMapType.equals(TermMap.TermMapType.COLUMN_VALUED)){
				if(typ.equals(R2RMLVocabulary.iri)) {
					objectAtom = TERM_FACTORY.getImmutableUriTemplate(objectAtom);
				}
			}

		}

		// we check if it is a literal with language tag

		if (lan != null) {
			objectAtom = TERM_FACTORY.getImmutableTypedTerm(objectAtom, lan);
		}

		// we check if it is a typed literal
		if (datatype != null) {
			Optional<COL_TYPE> type = TYPE_FACTORY.getDatatype(datatype.toString());
			if (!type.isPresent()) {
				// throw new RuntimeException("Unsupported datatype: " +
				// datatype.toString());
				logger.warn("Unsupported datatype will not be converted: "
						+ datatype.toString());
			} else {
				objectAtom = TERM_FACTORY.getImmutableTypedTerm(objectAtom, type.get());
			}
		}

		return objectAtom;
	}

	@Deprecated
	private Term getExplicitTypedObject(String string) {

		Term typedObject = null;
		String[] strings = string.split("<");
		if (strings.length > 1) {
			String consts = strings[0];
			consts = consts.substring(0, consts.length() - 2);
			consts = trim(consts);
			String type = strings[1];
			if (type.endsWith(">"))
				type = type.substring(0, type.length() - 1);

			DatatypePredicate predicate = new DatatypePredicateImpl(type, COL_TYPE.OBJECT);
			Term constant = TERM_FACTORY.getConstantLiteral(consts);
			typedObject = TERM_FACTORY.getFunction(predicate, constant);
		}
		return typedObject;
	}

	@Deprecated
	public List<BlankNodeOrIRI> getJoinNodes(TriplesMap tm) {
		List<BlankNodeOrIRI> joinPredObjNodes = new ArrayList<BlankNodeOrIRI>();
		// get predicate-object nodes
		Set<BlankNodeOrIRI> predicateObjectNodes = getPredicateObjects(tm);
		return joinPredObjNodes;
	}

	/**
	 * get a typed atom of a specific type
	 * 
	 * @param type
	 *            - iri, blanknode or literal
	 * @param string
	 *            - the atom as string
	 * @return the contructed Function atom
	 */
	private ImmutableFunctionalTerm getTermTypeAtom(String string, Object type, String joinCond) {

		if (type.equals(R2RMLVocabulary.iri)) {

			return getURIFunction(string, joinCond);

		} else if (type.equals(R2RMLVocabulary.blankNode)) {

			return getTypedFunction(string, 2, joinCond);

		} else if (type.equals(R2RMLVocabulary.literal)) {

			return getTypedFunction(trim(string), 3, joinCond);
		}
		return null;
	}

	private ImmutableFunctionalTerm getURIFunction(String string, String joinCond) {
		return getTypedFunction(string, 1, joinCond);
	}

	private ImmutableFunctionalTerm getURIFunction(String string) {
		return getTypedFunction(string, 1);
	}

	public ImmutableFunctionalTerm getTypedFunction(String parsedString, int type) {
		return getTypedFunction(parsedString, type, "");
	}
	
	
	//this function distinguishes curly bracket with back slash "\{" from curly bracket "{" 
	private int getIndexOfCurlyB(String str){
		int i;
		int j;
		i = str.indexOf("{");
		j = str.indexOf("\\{");
		
		while((i-1 == j) && (j != -1)){		
			i = str.indexOf("{",i+1);
			j = str.indexOf("\\{",j+1);		
		}	
		return i;
	}

	/**
	 * get a typed atom
	 * 
	 * @param parsedString
	 *            - the content of atom
	 * @param type
	 *            - 0=constant uri, 1=uri or iri, 2=bnode, 3=literal 4=concat
	 * @param joinCond
	 *            - CHILD_ or PARENT_ prefix for variables
	 * @return the constructed Function atom
	 */
	public ImmutableFunctionalTerm getTypedFunction(String parsedString, int type,
			String joinCond) {

		List<ImmutableTerm> terms = new ArrayList<>();
		String string = (parsedString);
		if (!string.contains("{")) {
			if (type < 3) {
    				if (!R2RMLVocabulary.isResourceString(string)) {
						string = R2RMLVocabulary.prefixUri("{" + string + "}");
					if (type == 2) {
						string = "\"" + string + "\"";
					}
				} else {
					type = 0;
				}
			}
		}
		if (type == 1) {
			string = R2RMLVocabulary.prefixUri(string);
		}

		String str = string; //str for concat of constant literal
		
		string = string.replace("\\{", "[");
		string = string.replace("\\}", "]");
		
		String cons;
		int i;
		while (string.contains("{")) {
			int end = string.indexOf("}");
			int begin = string.lastIndexOf("{", end);
			
			// (Concat) if there is constant literal in template, adds it to terms list 
			if (type == 4){
				if ((i = getIndexOfCurlyB(str)) > 0){
					cons = str.substring(0, i);
					str = str.substring(str.indexOf("}", i)+1, str.length());
					terms.add(TERM_FACTORY.getConstantLiteral(cons));
				}else{
					str = str.substring(str.indexOf("}")+1);
				}
			}

			String var = trim(string.substring(begin + 1, end));

			// trim for making variable
			terms.add(TERM_FACTORY.getVariable(joinCond + (var)));

			string = string.replace("{\"" + var + "\"}", "[]");
			string = string.replace("{" + var + "}", "[]");
			
		}
		if(type == 4){
			if (!str.equals("")){
				cons = str;
				terms.add(TERM_FACTORY.getConstantLiteral(cons));
			}
		}
	
		string = string.replace("[", "{");
		string = string.replace("]", "}");

		ImmutableTerm uriTemplate = null;
		switch (type) {
		// constant uri
		case 0:
			uriTemplate = TERM_FACTORY.getConstantLiteral(string);
			terms.add(0, uriTemplate); // the URI template is always on the
										// first position in the term list
			return TERM_FACTORY.getImmutableUriTemplate(ImmutableList.copyOf(terms));
			// URI or IRI
		case 1:
			uriTemplate = TERM_FACTORY.getConstantLiteral(string);
			terms.add(0, uriTemplate); // the URI template is always on the
										// first position in the term list
			return TERM_FACTORY.getImmutableUriTemplate(ImmutableList.copyOf(terms));
			// BNODE
		case 2:
			uriTemplate = TERM_FACTORY.getConstantBNode(string);
			terms.add(0, uriTemplate); // the URI template is always on the
										// first position in the term list
			return TERM_FACTORY.getImmutableBNodeTemplate(ImmutableList.copyOf(terms));
			// simple LITERAL
		case 3:
			uriTemplate = terms.remove(0);
			// pred = TYPE_FACTORY.getTypePredicate(); //
			// the URI template is always on the first position in the term list
			// terms.add(0, uriTemplate);
			return TERM_FACTORY.getImmutableTypedTerm(uriTemplate, COL_TYPE.STRING);
		case 4://concat
			ImmutableFunctionalTerm f = TERM_FACTORY.getImmutableFunctionalTerm(ExpressionOperation.CONCAT, terms.get(0), terms.get(1));
            for(int j=2;j<terms.size();j++){
                f = TERM_FACTORY.getImmutableFunctionalTerm(ExpressionOperation.CONCAT, f, terms.get(j));
            }
            return f;
		}
		return null;
	}

	/**
	 * method that trims a string of all its double apostrophes from beginning
	 * and end
	 * 
	 * @param string
	 *            - to be trimmed
	 * @return the string without any quotes
	 */
	private String trim(String string) {

		while (string.startsWith("\"") && string.endsWith("\"")) {

			string = string.substring(1, string.length() - 1);
		}
		return string;
	}

	/**
	 * method to trim a string of its leading or trailing quotes but one
	 * 
	 * @param string
	 *            - to be trimmed
	 * @return the string left with one leading and trailing quote
	 */
	private String trimTo1(String string) {

		while (string.startsWith("\"\"") && string.endsWith("\"\"")) {

			string = string.substring(1, string.length() - 1);
		}
		return string;
	}


}
