package it.unibz.krdb.obda.owlapi3;

/*
 * #%L
 * ontop-obdalib-owlapi3
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

import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
import it.unibz.krdb.obda.model.URIConstant;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.Assertion;
import it.unibz.krdb.obda.ontology.Axiom;
import it.unibz.krdb.obda.ontology.BasicClassDescription;
import it.unibz.krdb.obda.ontology.DataType;
import it.unibz.krdb.obda.ontology.DisjointClassesAxiom;
import it.unibz.krdb.obda.ontology.DisjointPropertiesAxiom;
import it.unibz.krdb.obda.ontology.LanguageProfile;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyFactory;
import it.unibz.krdb.obda.ontology.Property;
import it.unibz.krdb.obda.ontology.FunctionalPropertyAxiom;
import it.unibz.krdb.obda.ontology.PropertySomeRestriction;
import it.unibz.krdb.obda.ontology.SubClassOfAxiom;
import it.unibz.krdb.obda.ontology.SubPropertyOfAxiom;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import it.unibz.krdb.obda.ontology.impl.OntologyImpl;
import it.unibz.krdb.obda.ontology.impl.PunningException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Translates an OWLOntology into ontops internal ontology representation. It
 * will ignore all ABox assertions and does a syntactic approximation of the
 * ontology, dropping anything not supported by Quest during inference.
 * 
 * @author Mariano Rodriguez Muro <mariano.muro@gmail.com>
 * 
 */
public class OWLAPI3Translator {

	private final LanguageProfile profile = LanguageProfile.DLLITEA;

	private static final OBDADataFactory dfac = OBDADataFactoryImpl.getInstance();
	private static final OntologyFactory ofac = OntologyFactoryImpl.getInstance();

	private static final Logger log = LoggerFactory.getLogger(OWLAPI3Translator.class);

	/*
	 * If we need to construct auxiliary subclass axioms for A ISA exists R.C we
	 * put them in this map to avoid generating too many auxiliary
	 * roles/classes.
	 */
	private final Map<OWLObjectSomeValuesFrom, PropertySomeRestriction> auxiliaryClassProperties 
							= new HashMap<OWLObjectSomeValuesFrom, PropertySomeRestriction>();
	
	private final Map<OWLDataSomeValuesFrom, PropertySomeRestriction> auxiliaryDatatypeProperties 
							= new HashMap<OWLDataSomeValuesFrom, PropertySomeRestriction>();

	private int auxRoleCounter = 0;

	public OWLAPI3Translator() {

	}

	/***
	 * Load all the ontologies into a single translated merge.
	 * 
	 * @param ontologies
	 * @return
	 * @throws Exception
	 */
	public Ontology mergeTranslateOntologies(Set<OWLOntology> ontologies) throws Exception {
		/*
		 * We will keep track of the loaded ontologies and tranlsate the TBox
		 * part of them into our internal represntation
		 */
		String uri = "http://it.unibz.krdb.obda/Quest/auxiliaryontology";

		Ontology translatedOntologyMerge = ofac.createOntology(uri);

		log.debug("Load ontologies called. Translating ontologies.");
		OWLAPI3Translator translator = new OWLAPI3Translator();
		// Set<URI> uris = new HashSet<URI>();

		Ontology translation = ofac.createOntology(uri);
		for (OWLOntology onto : ontologies) {
			// uris.add(onto.getIRI().toURI());
			Ontology aux = translator.translate(onto);
			// R: cannot just add all referenced entities
			for (Predicate p : aux.getConcepts())
				translation.addConcept(p);
			for (Predicate p : aux.getRoles())
				translation.addRole(p);
			
			for (Axiom ax : aux.getAssertions())
				translation.addAssertion(ax);
			translation.getClassAssertions().addAll(aux.getClassAssertions());
			translation.getPropertyAssertions().addAll(aux.getPropertyAssertions());
			translation.getDisjointPropertiesAxioms().addAll(aux.getDisjointPropertiesAxioms());
			translation.getDisjointClassesAxioms().addAll(aux.getDisjointClassesAxioms());
			translation.getFunctionalPropertyAxioms().addAll(aux.getFunctionalPropertyAxioms());
		}
		/* we translated successfully, now we append the new assertions */

		translatedOntologyMerge = translation;

		// translatedOntologyMerge.addAssertions(translation.getAssertions());
		// translatedOntologyMerge.addConcepts(new
		// ArrayList<ClassDescription>(translation.getConcepts()));
		// translatedOntologyMerge.addRoles(new
		// ArrayList<Property>(translation.getRoles()));
		// translatedOntologyMerge.saturate();

		log.debug("Ontology loaded: {}", translatedOntologyMerge);

		return translatedOntologyMerge;

	}

	public Predicate getPredicate(OWLEntity entity) {
		Predicate p = null;
		if (entity instanceof OWLClass) {
			/* We ignore TOP and BOTTOM (Thing and Nothing) */
			if (((OWLClass) entity).isOWLThing() || ((OWLClass) entity).isOWLNothing()) {
				return null;
			}
			String uri = entity.getIRI().toString();

			p = dfac.getClassPredicate(uri);
		} else if (entity instanceof OWLObjectProperty) {
			String uri = entity.getIRI().toString();

			p = dfac.getObjectPropertyPredicate(uri);
		} else if (entity instanceof OWLDataProperty) {
			String uri = entity.getIRI().toString();

			p = dfac.getDataPropertyPredicate(uri);
		}
		return p;
	}

	public Ontology translate(OWLOntology owl) throws PunningException {
		// ManchesterOWLSyntaxOWLObjectRendererImpl rend = new
		// ManchesterOWLSyntaxOWLObjectRendererImpl();

		OWL2QLProfile owlprofile = new OWL2QLProfile();

		OWLProfileReport report = owlprofile.checkOntology(owl);
		Set<OWLAxiom> axiomIgnoresOWL2QL = new HashSet<OWLAxiom>();

		if (!report.isInProfile()) {
			log.warn("WARNING. The current ontology is not in the OWL 2 QL profile.");
			try {
				File profileReport = new File("quest-profile-report.log");
				if (profileReport.canWrite()) {
					BufferedWriter bf = new BufferedWriter(new FileWriter(profileReport));
					bf.write(report.toString());
					bf.flush();
					bf.close();
				}
			} catch (Exception e) {

			}
			// log.warn(report.toString());
			// for (OWLProfileViolation violation : report.getViolations())
			// axiomIgnoresOWL2QL.add(violation.getAxiom());
		}

		// Ontology dl_onto =
		// ofac.createOntology((owl.getOntologyID().getOntologyIRI().toString()));
		Ontology dl_onto = ofac.createOntology("http://www.unibz.it/ontology");

		HashSet<String> objectproperties = new HashSet<String>();
		HashSet<String> dataproperties = new HashSet<String>();
		//HashSet<String> classes = new HashSet<String>();

		/*
		 * First we add all definitions for classes and roles
		 */
		Set<OWLEntity> entities = owl.getSignature();
		Iterator<OWLEntity> eit = entities.iterator();

		HashSet<String> punnedPredicates = new HashSet<String>();

		while (eit.hasNext()) {
			OWLEntity entity = eit.next();
			Predicate p = getPredicate(entity);
			if (p == null)
				continue;

			/*
			 * When we register predicates punning is not allowed between data
			 * and object properties
			 */

			if (p.isClass()) {
				dl_onto.addConcept(p);

			} else {
				if (p.isObjectProperty()) {
					if (dataproperties.contains(p.getName().toString())) {
						punnedPredicates.add(p.getName().toString());
					} else {
						objectproperties.add(p.getName().toString());
						dl_onto.addRole(p);
					}
				} else {
					if (objectproperties.contains(p.getName().toString())) {
						punnedPredicates.add(p.getName().toString());
					} else {
						dataproperties.add(p.getName().toString());
						dl_onto.addRole(p);
					}
				}
			}
		}

		/*
		 * Generating a WARNING about all punned predicates which have been
		 * ignored
		 */
		if (!punnedPredicates.isEmpty()) {
			log.warn("Quest can become unstable with properties declared as both, data and object property. Offending properties: ");
			for (String predicates : punnedPredicates) {
				log.warn("  " + predicates);
			}
		}

		Set<OWLAxiom> axioms = owl.getAxioms();
		Iterator<OWLAxiom> it = axioms.iterator();
		while (it.hasNext()) {

			OWLAxiom axiom = it.next();

			if (axiomIgnoresOWL2QL.contains(axiom)) {
				/*
				 * This axiom is not part of OWL 2 QL according to the OWLAPI,
				 * we need to ignore it
				 */
				continue;
			}

			/***
			 * Important to use the negated normal form of the axioms, and not
			 * the simple ones.
			 */
			axiom = axiom.getNNF();

			try {
				if (axiom instanceof OWLEquivalentClassesAxiom) {
					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();

					OWLEquivalentClassesAxiom aux = (OWLEquivalentClassesAxiom) axiom;
					Set<OWLClassExpression> equivalents = aux.getClassExpressions();
					List<BasicClassDescription> vec = getSubclassExpressions(equivalents);

					addConceptEquivalences(dl_onto, vec);

				} else if (axiom instanceof OWLSubClassOfAxiom) {

					OWLSubClassOfAxiom aux = (OWLSubClassOfAxiom) axiom;
					BasicClassDescription subDescription = getSubclassExpression(aux.getSubClass());
					List<BasicClassDescription> superDescriptions = getSuperclassExpressions(aux.getSuperClass(), dl_onto);

					addSubclassAxioms(dl_onto, subDescription, superDescriptions);

				} else if (axiom instanceof OWLDataPropertyDomainAxiom) {

					OWLDataPropertyDomainAxiom aux = (OWLDataPropertyDomainAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());

					BasicClassDescription subclass = ofac.createPropertySomeRestriction(role);
					List<BasicClassDescription> superDescriptions = getSuperclassExpressions(aux.getDomain(), dl_onto);

					addSubclassAxioms(dl_onto, subclass, superDescriptions);

				} else if (axiom instanceof OWLDataPropertyRangeAxiom) {

					OWLDataPropertyRangeAxiom aux = (OWLDataPropertyRangeAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());

					BasicClassDescription subclass = ofac.createPropertySomeRestriction(role.getPredicate(), true);

					if (aux.getRange().isDatatype()) {
						OWLDatatype rangeDatatype = aux.getRange().asOWLDatatype();

						if (rangeDatatype.isBuiltIn()) {

							Predicate.COL_TYPE columnType = getColumnType(rangeDatatype);
							DataType datatype = ofac.createDataType(dfac.getTypePredicate(columnType));
							addSubclassAxiom(dl_onto, subclass, datatype);
						} else {
							log.warn("Ignoring range axiom since it refers to a non-supported datatype: " + axiom.toString());
						}
					} else {
						log.warn("Ignoring range axiom since it is not a datatype: " + axiom.toString());
					}

				} else if (axiom instanceof OWLSubDataPropertyOfAxiom) {

					OWLSubDataPropertyOfAxiom aux = (OWLSubDataPropertyOfAxiom) axiom;
					Property subrole = getRoleExpression(aux.getSubProperty());
					Property superrole = getRoleExpression(aux.getSuperProperty());

					SubPropertyOfAxiom roleinc = ofac.createSubPropertyAxiom(subrole, superrole);
					dl_onto.addAssertion(roleinc);

				} else if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {

					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();

					OWLEquivalentDataPropertiesAxiom aux = (OWLEquivalentDataPropertiesAxiom) axiom;
					List<Property> vec = getDataRoleExpressions(aux.getProperties());
					addRoleEquivalences(dl_onto, vec);

				} else if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) {

					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();

					OWLEquivalentObjectPropertiesAxiom aux = (OWLEquivalentObjectPropertiesAxiom) axiom;
					List<Property> vec = getObjectRoleExpressions(aux.getProperties());
					addRoleEquivalences(dl_onto, vec);

				} else if (axiom instanceof OWLFunctionalDataPropertyAxiom) {
					if (profile.order() < LanguageProfile.DLLITEA.order())
						throw new TranslationException();
					OWLFunctionalDataPropertyAxiom aux = (OWLFunctionalDataPropertyAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());
					FunctionalPropertyAxiom funct = ofac.createPropertyFunctionalAxiom(role);

					dl_onto.addAssertion(funct);

				} else if (axiom instanceof OWLInverseObjectPropertiesAxiom) {
					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();

					OWLInverseObjectPropertiesAxiom aux = (OWLInverseObjectPropertiesAxiom) axiom;
					OWLObjectPropertyExpression exp1 = aux.getFirstProperty();
					OWLObjectPropertyExpression exp2 = aux.getSecondProperty();
					Property role1 = getRoleExpression(exp1);
					Property role2 = getRoleExpression(exp2);

					Property invrole1 = ofac.createProperty(role1.getPredicate(), !role1.isInverse());
					Property invrole2 = ofac.createProperty(role2.getPredicate(), !role2.isInverse());

					SubPropertyOfAxiom inc1 = ofac.createSubPropertyAxiom(role1, invrole2);
					SubPropertyOfAxiom inc2 = ofac.createSubPropertyAxiom(role2, invrole1);

					dl_onto.addAssertion(inc1);
					dl_onto.addAssertion(inc2);

				} else if (axiom instanceof OWLSymmetricObjectPropertyAxiom) {
					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();
					OWLSymmetricObjectPropertyAxiom aux = (OWLSymmetricObjectPropertyAxiom) axiom;
					OWLObjectPropertyExpression exp1 = aux.getProperty();
					Property role = getRoleExpression(exp1);
					Property invrole = ofac.createProperty(role.getPredicate(), !role.isInverse());

					SubPropertyOfAxiom symm = ofac.createSubPropertyAxiom(invrole, role);

					dl_onto.addAssertion(symm);

				} else if (axiom instanceof OWLObjectPropertyDomainAxiom) {

					OWLObjectPropertyDomainAxiom aux = (OWLObjectPropertyDomainAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());

					BasicClassDescription subclass = ofac.createPropertySomeRestriction(role);
					List<BasicClassDescription> superDescriptions = getSuperclassExpressions(aux.getDomain(), dl_onto);

					addSubclassAxioms(dl_onto, subclass, superDescriptions);

				} else if (axiom instanceof OWLObjectPropertyRangeAxiom) {

					OWLObjectPropertyRangeAxiom aux = (OWLObjectPropertyRangeAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());

					BasicClassDescription subclass = ofac.createPropertySomeRestriction(role.getPredicate(), !role.isInverse());
					List<BasicClassDescription> superDescriptions = getSuperclassExpressions(aux.getRange(), dl_onto);

					addSubclassAxioms(dl_onto, subclass, superDescriptions);

				} else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {

					OWLSubObjectPropertyOfAxiom aux = (OWLSubObjectPropertyOfAxiom) axiom;
					Property subrole = getRoleExpression(aux.getSubProperty());
					Property superrole = getRoleExpression(aux.getSuperProperty());

					SubPropertyOfAxiom roleinc = ofac.createSubPropertyAxiom(subrole, superrole);

					dl_onto.addAssertion(roleinc);

				} else if (axiom instanceof OWLFunctionalObjectPropertyAxiom) {
					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();
					OWLFunctionalObjectPropertyAxiom aux = (OWLFunctionalObjectPropertyAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());
					FunctionalPropertyAxiom funct = ofac.createPropertyFunctionalAxiom(role);

					dl_onto.addAssertion(funct);
					
				} else if (axiom instanceof OWLInverseFunctionalObjectPropertyAxiom) {
					if (profile.order() < LanguageProfile.OWL2QL.order())
						throw new TranslationException();
					OWLInverseFunctionalObjectPropertyAxiom aux = (OWLInverseFunctionalObjectPropertyAxiom) axiom;
					Property role = getRoleExpression(aux.getProperty());
					Property invrole = ofac.createProperty(role.getPredicate(), !role.isInverse());
					FunctionalPropertyAxiom funct = ofac.createPropertyFunctionalAxiom(invrole);

					dl_onto.addAssertion(funct);

				} else if (axiom instanceof OWLDisjointClassesAxiom) {
					OWLDisjointClassesAxiom aux = (OWLDisjointClassesAxiom) axiom;
					for (OWLClassExpression disjClass : aux.getClassExpressionsAsList()) {
						if (!(disjClass instanceof OWLClass))
						throw new TranslationException("Invalid class expression in disjoint class axiom: "+disjClass.toString());
					}
					Set<OWLClass> disjointClasses = aux.getClassesInSignature();
					Iterator<OWLClass> iter = disjointClasses.iterator();
					if (!iter.hasNext())
						throw new TranslationException();
					OClass c1 = ofac.createClass(iter.next().toStringID());
					OClass c2 = ofac.createClass(iter.next().toStringID());
					DisjointClassesAxiom disj = ofac.createDisjointClassAxiom(c1, c2);
					
					dl_onto.addAssertion(disj);
							
				} else if (axiom instanceof OWLDisjointDataPropertiesAxiom) {
					OWLDisjointDataPropertiesAxiom aux = (OWLDisjointDataPropertiesAxiom) axiom;
					Set<OWLDataProperty> disjointProps = aux.getDataPropertiesInSignature();
					Iterator<OWLDataProperty> iter = disjointProps.iterator();
					if (!iter.hasNext())
						throw new TranslationException();
					Property p1 = ofac.createDataProperty(iter.next().toStringID());
					Property p2 = ofac.createDataProperty(iter.next().toStringID());
					DisjointPropertiesAxiom disj = ofac.createDisjointPropertiesAxiom(p1, p2);
					
					dl_onto.addAssertion(disj);
					
				} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
					OWLDisjointObjectPropertiesAxiom aux = (OWLDisjointObjectPropertiesAxiom) axiom;
					Set<OWLObjectProperty> disjointProps = aux.getObjectPropertiesInSignature();
					Iterator<OWLObjectProperty> iter = disjointProps.iterator();
					if (!iter.hasNext())
						throw new TranslationException();
					// TODO: handle inverses
					Property p1 = ofac.createObjectProperty(iter.next().toStringID(), false);
					Property p2 = ofac.createObjectProperty(iter.next().toStringID(), false);
					DisjointPropertiesAxiom disj = ofac.createDisjointPropertiesAxiom(p1, p2);
					
					dl_onto.addAssertion(disj);
				
				
				} else if (axiom instanceof OWLIndividualAxiom) {
					Axiom translatedAxiom = translate((OWLIndividualAxiom)axiom);
					if (translatedAxiom != null)
						dl_onto.addAssertion(translatedAxiom);
					
				} else if (axiom instanceof OWLAnnotationAxiom) {
					/*
					 * Annotations axioms are intentionally ignored by the
					 * translator
					 */
				} else if (axiom instanceof OWLDeclarationAxiom) {
					OWLDeclarationAxiom owld = (OWLDeclarationAxiom) axiom;
					OWLEntity entity = owld.getEntity();
					if (entity instanceof OWLClass) {
						if (!entity.asOWLClass().isOWLThing()) {

							String uri = entity.asOWLClass().getIRI().toString();
							dl_onto.addConcept(dfac.getClassPredicate(uri));
						}
					} else if (entity instanceof OWLObjectProperty) {
						String uri = entity.asOWLObjectProperty().getIRI().toString();
						dl_onto.addRole(dfac.getObjectPropertyPredicate(uri));
					} else if (entity instanceof OWLDataProperty) {
						String uri = entity.asOWLDataProperty().getIRI().toString();
						dl_onto.addRole(dfac.getDataPropertyPredicate(uri));
					} else if (entity instanceof OWLIndividual) {
						/*
						 * NO OP, individual declarations are ignored silently
						 * during TBox translation
						 */
					} else {
						log.info("Ignoring declaration axiom: {}", axiom);
					}

					/*
					 * Annotations axioms are intentionally ignored by the
					 * translator
					 */
				}
				// else if (axiom instanceof OWLImportsDeclaration) {
				// /*
				// * Imports
				// */
				// }
				else {
					log.warn("Axiom not yet supported by Quest: {}", axiom.toString());
				}
			} catch (TranslationException e) {
				log.warn("Axiom not yet supported by Quest: {}", axiom.toString());
			}
		}
		return dl_onto;
	}

	private void addSubclassAxiom(Ontology dl_onto, BasicClassDescription subDescription, BasicClassDescription superDescription) {
		if (superDescription == null || subDescription == null) {
			log.warn("NULL: {} {}", subDescription, superDescription);
		}

			/* We ignore TOP and BOTTOM (Thing and Nothing) */
			if (superDescription instanceof OClass) {
				OClass classDescription = (OClass) superDescription;
				if (classDescription.toString().equals("http://www.w3.org/2002/07/owl#Thing")) {
					return;
				}
			}
			SubClassOfAxiom inc = ofac.createSubClassAxiom(subDescription, superDescription);
			dl_onto.addAssertion(inc);
	}

	private void addSubclassAxioms(Ontology dl_onto, BasicClassDescription subDescription, List<BasicClassDescription> superDescriptions) {
		for (BasicClassDescription superDescription : superDescriptions) {
			addSubclassAxiom(dl_onto, subDescription, superDescription);
		}
	}

	private List<BasicClassDescription> getSubclassExpressions(Collection<OWLClassExpression> owlExpressions) throws TranslationException {
		List<BasicClassDescription> descriptions = new LinkedList<BasicClassDescription>();
		for (OWLClassExpression OWLClassExpression : owlExpressions) {
			descriptions.add(getSubclassExpression(OWLClassExpression));
		}
		return descriptions;
	}

	private Property getRoleExpression(OWLObjectPropertyExpression rolExpression) throws TranslationException {
		Property role = null;

		if (rolExpression instanceof OWLObjectProperty) {
			role = ofac.createObjectProperty(rolExpression.asOWLObjectProperty().getIRI().toString(), false);
		} else if (rolExpression instanceof OWLObjectInverseOf) {
			if (profile.order() < LanguageProfile.OWL2QL.order())
				throw new TranslationException();
			OWLObjectInverseOf aux = (OWLObjectInverseOf) rolExpression;
			role = ofac.createProperty(dfac.getObjectPropertyPredicate((aux.getInverse().asOWLObjectProperty().getIRI().toString())), true);
		} else {
			throw new TranslationException();
		}
		return role;

	}

	private void addConceptEquivalences(Ontology ontology, List<BasicClassDescription> roles) {
		for (int i = 0; i < roles.size(); i++) {
			for (int j = i + 1; j < roles.size(); j++) {
				BasicClassDescription subclass = roles.get(i);
				BasicClassDescription superclass = roles.get(j);
				SubClassOfAxiom inclusion1 = ofac.createSubClassAxiom(subclass, superclass);
				ontology.addAssertion(inclusion1);
				SubClassOfAxiom inclusion2 = ofac.createSubClassAxiom(superclass, subclass);
				ontology.addAssertion(inclusion2);
			}
		}
	}

	private void addRoleEquivalences(Ontology ontology, List<Property> roles) {
		for (int i = 0; i < roles.size(); i++) {
			for (int j = i + 1; j < roles.size(); j++) {
				Property subrole = roles.get(i);
				Property superole = roles.get(j);
				SubPropertyOfAxiom inclusion1 = ofac.createSubPropertyAxiom(subrole, superole);
				ontology.addAssertion(inclusion1);
				SubPropertyOfAxiom inclusion2 = ofac.createSubPropertyAxiom(superole, subrole);
				ontology.addAssertion(inclusion2);
			}
		}
	}

	private List<Property> getObjectRoleExpressions(Collection<OWLObjectPropertyExpression> rolExpressions) throws TranslationException {
		List<Property> result = new LinkedList<Property>();
		for (OWLObjectPropertyExpression rolExpression : rolExpressions) {
			result.add(getRoleExpression(rolExpression));
		}
		return result;
	}

	private List<Property> getDataRoleExpressions(Collection<OWLDataPropertyExpression> rolExpressions) throws TranslationException {
		List<Property> result = new LinkedList<Property>();
		for (OWLDataPropertyExpression rolExpression : rolExpressions) {
			result.add(getRoleExpression(rolExpression));
		}
		return result;
	}

	private Property getRoleExpression(OWLDataPropertyExpression rolExpression) throws TranslationException {
		Property role = null;

		if (rolExpression instanceof OWLDataProperty) {
			role = ofac.createDataProperty((rolExpression.asOWLDataProperty().getIRI().toString()));
		} else {
			throw new TranslationException();
		}
		return role;

	}

	private List<BasicClassDescription> getSuperclassExpressions(OWLClassExpression owlExpression, Ontology dl_onto) throws TranslationException {
		List<BasicClassDescription> result = new LinkedList<BasicClassDescription>();
		if (owlExpression instanceof OWLObjectIntersectionOf) {
			if (profile.order() < LanguageProfile.OWL2QL.order()) {
				throw new TranslationException();
			}
			OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) owlExpression;
			Set<OWLClassExpression> operands = intersection.getOperands();
			for (OWLClassExpression operand : operands) {
				result.addAll(getSuperclassExpressions(operand, dl_onto));
			}
		} else if (owlExpression instanceof OWLObjectSomeValuesFrom) {
			if (profile.order() < LanguageProfile.OWL2QL.order()) {
				throw new TranslationException();
			}
			OWLObjectSomeValuesFrom someexp = (OWLObjectSomeValuesFrom) owlExpression;
			OWLClassExpression filler = someexp.getFiller();
			if (!(filler instanceof OWLClass)) {
				throw new TranslationException();
			}
			if (filler.isOWLThing()) {
				BasicClassDescription cd = getSubclassExpression(owlExpression);
				result.add(cd);
			} else {
				BasicClassDescription cd = getPropertySomeClassRestriction(someexp, dl_onto);
				result.add(cd);
			}
		} else if (owlExpression instanceof OWLDataSomeValuesFrom) {
			if (profile.order() < LanguageProfile.OWL2QL.order()) {
				throw new TranslationException();
			}
			OWLDataSomeValuesFrom someexp = (OWLDataSomeValuesFrom) owlExpression;
			OWLDataRange filler = someexp.getFiller();

			if (filler.isTopDatatype()) {
				OWLDataPropertyExpression property = someexp.getProperty();
				Property role = getRoleExpression(property);
				BasicClassDescription cd = ofac.createPropertySomeRestriction(role);
				result.add(cd);
			} else if (filler instanceof OWLDatatype) {
				BasicClassDescription cd = this.getPropertySomeDatatypeRestriction(someexp, dl_onto);
				result.add(cd);
			}
		} else {
			result.add(getSubclassExpression(owlExpression));
		}
		return result;
	}

	private BasicClassDescription getPropertySomeClassRestriction(OWLObjectSomeValuesFrom someexp, Ontology dl_onto) throws TranslationException {
		
		PropertySomeRestriction auxclass = auxiliaryClassProperties.get(someexp);
		if (auxclass == null) {
			/*
			 * no auxiliary subclass assertions found for this exists R.A,
			 * creating a new one
			 */
			
			OWLObjectPropertyExpression owlProperty = someexp.getProperty();
			OWLClassExpression owlFiller = someexp.getFiller();
			
			Property role = getRoleExpression(owlProperty);
			BasicClassDescription filler = getSubclassExpression(owlFiller);

			Property auxRole = ofac.createProperty(dfac.getObjectPropertyPredicate((OntologyImpl.AUXROLEURI + auxRoleCounter)), false);
			auxRoleCounter += 1;

			auxclass = ofac.createPropertySomeRestriction(auxRole.getPredicate(), role.isInverse());
			auxiliaryClassProperties.put(someexp, auxclass);

			/* Creating the new subrole assertions */
			SubPropertyOfAxiom subrole = ofac.createSubPropertyAxiom(auxRole, ofac.createProperty(role.getPredicate(), false));
			dl_onto.addAssertion(subrole);
			
			/* Creating the range assertion */
			PropertySomeRestriction propertySomeRestrictionInv = ofac.createPropertySomeRestriction(auxRole.getPredicate(), !role.isInverse());
			SubClassOfAxiom subclass = ofac.createSubClassAxiom(propertySomeRestrictionInv, filler);
			dl_onto.addAssertion(subclass);
		}

		return auxclass;
	}

	private BasicClassDescription getPropertySomeDatatypeRestriction(OWLDataSomeValuesFrom someexp, Ontology dl_onto) throws TranslationException {
		
		PropertySomeRestriction auxclass = auxiliaryDatatypeProperties.get(someexp);
		if (auxclass == null) {
			/*
			 * no auxiliary subclass assertions found for this exists R.A,
			 * creating a new one
			 */
			
			OWLDataPropertyExpression owlProperty = someexp.getProperty();
			Property role = getRoleExpression(owlProperty);

			OWLDataRange owlFiller = someexp.getFiller();		
			BasicClassDescription filler = getDataTypeExpression(owlFiller);

			Property auxRole = ofac.createProperty(dfac.getObjectPropertyPredicate((OntologyImpl.AUXROLEURI + auxRoleCounter)), false);
			auxRoleCounter += 1;

			auxclass = ofac.createPropertySomeRestriction(auxRole.getPredicate(), role.isInverse());
			auxiliaryDatatypeProperties.put(someexp, auxclass);

			/* Creating the new subrole assertions */
			SubPropertyOfAxiom subrole = ofac.createSubPropertyAxiom(auxRole, ofac.createProperty(role.getPredicate(), false));
			dl_onto.addAssertion(subrole);
			
			/* Creating the range assertion */
			PropertySomeRestriction propertySomeRestrictionInv = ofac.createPropertySomeRestriction(auxRole.getPredicate(), !role.isInverse());
			SubClassOfAxiom subclass = ofac.createSubClassAxiom(propertySomeRestrictionInv, filler);
			dl_onto.addAssertion(subclass);
		}

		return auxclass;
	}
	
	private DataType getDataTypeExpression(OWLDataRange filler) throws TranslationException {
		OWLDatatype owlDatatype = (OWLDatatype) filler;
		COL_TYPE datatype = getColumnType(owlDatatype);
		return ofac.createDataType(dfac.getTypePredicate(datatype));
	}

	private BasicClassDescription getSubclassExpression(OWLClassExpression owlExpression) throws TranslationException {
		BasicClassDescription cd = null;
		if (owlExpression instanceof OWLClass) {
			String uri = ((OWLClass) owlExpression).getIRI().toString();
			Predicate p = dfac.getClassPredicate(uri);
			cd = ofac.createClass(p);

		} else if (owlExpression instanceof OWLDataMinCardinality) {
			if (profile.order() < LanguageProfile.DLLITEA.order())
				throw new TranslationException();
			OWLDataMinCardinality rest = (OWLDataMinCardinality) owlExpression;
			int cardinatlity = rest.getCardinality();
			OWLDataRange range = rest.getFiller();
			if (cardinatlity != 1 || range != null) {
				throw new TranslationException();
			}
			String uri = (rest.getProperty().asOWLDataProperty().getIRI().toString());
			Predicate attribute = dfac.getDataPropertyPredicate(uri);
			cd = ofac.createPropertySomeRestriction(attribute, false);

		} else if (owlExpression instanceof OWLObjectMinCardinality) {
			if (profile.order() < LanguageProfile.DLLITEA.order())
				throw new TranslationException();
			OWLObjectMinCardinality rest = (OWLObjectMinCardinality) owlExpression;
			int cardinatlity = rest.getCardinality();
			OWLClassExpression filler = rest.getFiller();
			if (cardinatlity != 1) {
				throw new TranslationException();
			}

			if (!filler.isOWLThing()) {
				throw new TranslationException();
			}
			OWLObjectPropertyExpression propExp = rest.getProperty();
			String uri = propExp.getNamedProperty().getIRI().toString();
			Predicate role = dfac.getObjectPropertyPredicate(uri);

			if (propExp instanceof OWLObjectInverseOf) {
				cd = ofac.createPropertySomeRestriction(role, true);
			} else {
				cd = ofac.createPropertySomeRestriction(role, false);
			}

		} else if (owlExpression instanceof OWLObjectSomeValuesFrom) {
			if (profile.order() < LanguageProfile.OWL2QL.order())
				throw new TranslationException();
			OWLObjectSomeValuesFrom rest = (OWLObjectSomeValuesFrom) owlExpression;
			OWLClassExpression filler = rest.getFiller();

			if (!filler.isOWLThing()) {
				throw new TranslationException();
			}
			OWLObjectPropertyExpression propExp = rest.getProperty();
			String uri = propExp.getNamedProperty().getIRI().toString();
			Predicate role = dfac.getObjectPropertyPredicate(uri);

			if (propExp instanceof OWLObjectInverseOf) {
				cd = ofac.createPropertySomeRestriction(role, true);
			} else {
				cd = ofac.createPropertySomeRestriction(role, false);
			}
		} else if (owlExpression instanceof OWLDataSomeValuesFrom) {
			if (profile.order() < LanguageProfile.OWL2QL.order())
				throw new TranslationException();
			OWLDataSomeValuesFrom rest = (OWLDataSomeValuesFrom) owlExpression;
			OWLDataRange filler = rest.getFiller();

			if (!filler.isTopDatatype()) {
				throw new TranslationException();
			}
			OWLDataProperty propExp = (OWLDataProperty) rest.getProperty();
			String uri = propExp.getIRI().toString();
			Predicate role = dfac.getDataPropertyPredicate(uri);
			cd = ofac.createPropertySomeRestriction(role, false);
		}

		if (cd == null) {
			throw new TranslationException();
		}
		return cd;
	}

	private class TranslationException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7917688953760608030L;

		public TranslationException() {
			super();
		}

		public TranslationException(String msg) {
			super(msg);
		}

	}


	/***
	 * This will translate an OWLABox assertion into our own abox assertions.
	 * The functioning is straight forward except for the equivalenceMap.
	 * 
	 * The equivalenceMap is used to align the ABox assertions with n
	 * alternative vocabulary. The equivalence map relates a Class or Role with
	 * another Class or Role (or inverse Role) that should be used instead of
	 * the original to create the ABox assertions.
	 * 
	 * For example, if the equivalenceMap has the mapping hasFather ->
	 * inverse(hasChild), then, if the ABox assertions is
	 * "hasFather(mariano,ramon)", the translator will return
	 * "hasChild(ramon,mariano)".
	 * 
	 * If there is no equivalence mapping for a given class or property, the
	 * translation is straight forward. If the map is empty or it is null, the
	 * translation is straight forward.
	 * 
	 * @param axiom
	 * @return
	 */
	public Assertion translate(OWLIndividualAxiom axiom) {

		if (axiom instanceof OWLClassAssertionAxiom) {

			/*
			 * Class assertions
			 */

			OWLClassAssertionAxiom assertion = (OWLClassAssertionAxiom) axiom;
			OWLClassExpression classExpression = assertion.getClassExpression();
			if (!(classExpression instanceof OWLClass) || classExpression.isOWLThing() || classExpression.isOWLNothing())
				return null;

			OWLClass namedclass = (OWLClass) classExpression;
			OWLIndividual indv = assertion.getIndividual();

			if (indv.isAnonymous()) {
				throw new RuntimeException("Found anonymous individual, this feature is not supported");
			}

			Predicate classproperty = dfac.getClassPredicate((namedclass.getIRI().toString()));
			URIConstant c = dfac.getConstantURI(indv.asOWLNamedIndividual().getIRI().toString());

			OClass concept = ofac.createClass(classproperty.getName());
			return ofac.createClassAssertion(concept, c);

		} else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {

			/*
			 * Role assertions
			 */

			OWLObjectPropertyAssertionAxiom assertion = (OWLObjectPropertyAssertionAxiom) axiom;
			OWLObjectPropertyExpression propertyExperssion = assertion.getProperty();

			String property = null;
			OWLIndividual subject = null;
			OWLIndividual object = null;

			if (propertyExperssion instanceof OWLObjectProperty) {
				OWLObjectProperty namedclass = (OWLObjectProperty) propertyExperssion;
				property = namedclass.getIRI().toString();

				subject = assertion.getSubject();
				object = assertion.getObject();

			} else if (propertyExperssion instanceof OWLObjectInverseOf) {
				OWLObjectProperty namedclass = ((OWLObjectInverseOf) propertyExperssion).getInverse().getNamedProperty();
				property = namedclass.getIRI().toString();
				subject = assertion.getObject();
				object = assertion.getSubject();
			}

			if (subject.isAnonymous()) {
				throw new RuntimeException("Found anonymous individual, this feature is not supported");
			}

			if (object.isAnonymous()) {
				throw new RuntimeException("Found anonymous individual, this feature is not supported");
			}
			Predicate p = dfac.getObjectPropertyPredicate(property);
			URIConstant c1 = dfac.getConstantURI(subject.asOWLNamedIndividual().getIRI().toString());
			URIConstant c2 = dfac.getConstantURI(object.asOWLNamedIndividual().getIRI().toString());

//			Description equivalent = null;
//			if (equivalenceMap != null)
//				equivalent = equivalenceMap.get(p);

			Property prop = ofac.createObjectProperty(p.getName(), false);
//			if (equivalent == null)
				return ofac.createPropertyAssertion(prop, c1, c2);
//			else {
//				Property equiProp = (Property) equivalent;
//				if (!equiProp.isInverse()) {
//					return ofac.createObjectPropertyAssertion(equiProp.getPredicate(), c1, c2);
//				} else {
//					return ofac.createObjectPropertyAssertion(equiProp.getPredicate(), c2, c1);
//				}
//			}

		} else if (axiom instanceof OWLDataPropertyAssertionAxiom) {

			/*
			 * Attribute assertions
			 */

			OWLDataPropertyAssertionAxiom assertion = (OWLDataPropertyAssertionAxiom) axiom;
			OWLDataProperty propertyExperssion = (OWLDataProperty) assertion.getProperty();

			String property = propertyExperssion.getIRI().toString();
			OWLIndividual subject = assertion.getSubject();
			OWLLiteral object = assertion.getObject();

			if (subject.isAnonymous()) {
				throw new RuntimeException("Found anonymous individual, this feature is not supported");
			}

			Predicate.COL_TYPE type;
			try {
				type = getColumnType(object.getDatatype());
			} catch (TranslationException e) {
				throw new RuntimeException(e.getMessage());
			}

			Predicate p = dfac.getDataPropertyPredicate(property);
			URIConstant c1 = dfac.getConstantURI(subject.asOWLNamedIndividual().getIRI().toString());
			ValueConstant c2 = dfac.getConstantLiteral(object.getLiteral(), type);

			Property prop = ofac.createDataProperty(p.getName());
			return ofac.createPropertyAssertion(prop, c1, c2);

		} else {
			return null;
		}
	}

	private Predicate.COL_TYPE getColumnType(OWLDatatype datatype) throws TranslationException {
		if (datatype == null) {
			return COL_TYPE.LITERAL;
		}
		if (datatype.isString() || datatype.getBuiltInDatatype() == OWL2Datatype.XSD_STRING) { // xsd:string
			return COL_TYPE.STRING;
		} else if (datatype.isRDFPlainLiteral() || datatype.getBuiltInDatatype() == OWL2Datatype.RDF_PLAIN_LITERAL // rdf:PlainLiteral
				|| datatype.getBuiltInDatatype() == OWL2Datatype.RDFS_LITERAL) { // rdfs:Literal
			return COL_TYPE.LITERAL;
		} else if (datatype.isInteger()
				|| datatype.getBuiltInDatatype() == OWL2Datatype.XSD_INTEGER) {
            return COL_TYPE.INTEGER;
        } else if ( datatype.getBuiltInDatatype() == OWL2Datatype.XSD_NON_NEGATIVE_INTEGER) {
            return COL_TYPE.NON_NEGATIVE_INTEGER;
        } else if (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_INT) { // xsd:int
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return COL_TYPE.INT;
        } else if  (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_POSITIVE_INTEGER){
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return COL_TYPE.POSITIVE_INTEGER;
        } else if  (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_NEGATIVE_INTEGER) {
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return COL_TYPE.NEGATIVE_INTEGER;
        } else if  (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_NON_POSITIVE_INTEGER){
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return COL_TYPE.NON_POSITIVE_INTEGER;
        } else if  (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_UNSIGNED_INT) {
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return COL_TYPE.UNSIGNED_INT;
		} else if (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_DECIMAL) { // xsd:decimal
			return Predicate.COL_TYPE.DECIMAL;
        } else if (datatype.isFloat() || datatype.isDouble() || datatype.getBuiltInDatatype() == OWL2Datatype.XSD_DOUBLE) { // xsd:double
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
			return Predicate.COL_TYPE.DOUBLE;
        } else if (datatype.isFloat() || datatype.getBuiltInDatatype() == OWL2Datatype.XSD_FLOAT) { // xsd:float
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return Predicate.COL_TYPE.FLOAT;
		} else if (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_DATE_TIME || datatype.getBuiltInDatatype() == OWL2Datatype.XSD_DATE_TIME_STAMP ) {
			return Predicate.COL_TYPE.DATETIME;
        } else if (datatype.getBuiltInDatatype() == OWL2Datatype.XSD_LONG) {
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
            return Predicate.COL_TYPE.LONG;
		} else if (datatype.isBoolean() || datatype.getBuiltInDatatype() == OWL2Datatype.XSD_BOOLEAN) { // xsd:boolean
            System.err.println(datatype.getBuiltInDatatype() + " is not in OWL2QL profile");
			return Predicate.COL_TYPE.BOOLEAN;
		} else {
			throw new TranslationException("Unsupported data range: " + datatype.toString());
		}
	}

	// public Predicate getDataTypePredicate(Predicate.COL_TYPE type) {
	// switch (type) {
	// case LITERAL:
	// return dfac.getDataTypePredicateLiteral();
	// case STRING:
	// return dfac.getDataTypePredicateString();
	// case INTEGER:
	// return dfac.getDataTypePredicateInteger();
	// case DECIMAL:
	// return dfac.getDataTypePredicateDecimal();
	// case DOUBLE:
	// return dfac.getDataTypePredicateDouble();
	// case DATETIME:
	// return dfac.getDataTypePredicateDateTime();
	// case BOOLEAN:
	// return dfac.getDataTypePredicateBoolean();
	// default:
	// return dfac.getDataTypePredicateLiteral();
	// }
	// }
}
