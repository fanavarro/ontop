package it.unibz.inf.ontop.model;

import java.util.List;

import org.eclipse.rdf4j.model.URI;

import it.unibz.inf.ontop.model.Predicate.COL_TYPE;

public interface DatatypeFactory {

	@Deprecated
	public COL_TYPE getDatatype(String uri);
	
	public COL_TYPE getDatatype(URI uri);
	
	public URI getDatatypeURI(COL_TYPE type);

	
	public DatatypePredicate getTypePredicate(COL_TYPE type);
	
		
	public boolean isBoolean(Predicate p);
	
	public boolean isInteger(Predicate p);
	
	public boolean isFloat(Predicate p);
	
	public boolean isLiteral(Predicate p);
	
	public boolean isString(Predicate p);

	
	
	public List<Predicate> getDatatypePredicates();

}
