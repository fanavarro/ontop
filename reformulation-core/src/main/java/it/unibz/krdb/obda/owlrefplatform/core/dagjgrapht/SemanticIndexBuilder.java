package it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht;

import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;


/** 
 * 
 * Build the indexes for the DAG
 * create a map with the index and the intervals for each node in the graph
 * 
 * 
 */
public class SemanticIndexBuilder  {

	private final TBoxReasoner reasoner;
	private final Map<ClassExpression, SemanticIndexRange> classRanges;
	private final Map<ObjectPropertyExpression, SemanticIndexRange> opRanges;
	private final Map<DataPropertyExpression, SemanticIndexRange> dpRanges;
	
	private int index_counter = 1;

	/**
	 * Listener that creates the index for each node visited in depth first search.
	 * extends TraversalListenerAdapter from JGrapht
	 *
	 */
	private final class SemanticIndexer<T> extends TraversalListenerAdapter<T, DefaultEdge> {

		private T reference; 		//last root node
		private boolean newComponent = true;

		private final DirectedGraph <T,DefaultEdge> namedDAG;
		private final Map<T, SemanticIndexRange> ranges;
		
		public SemanticIndexer(DirectedGraph<T,DefaultEdge> namedDAG, Map<T, SemanticIndexRange> ranges) {
			this.namedDAG = namedDAG;
			this.ranges = ranges;
		}
		
		//search for the new root in the graph
		@Override
		public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			newComponent = true;
		}

		@Override
		public void vertexTraversed(VertexTraversalEvent<T> e) {
			T vertex = e.getVertex();

			if (newComponent) {
				reference = vertex;
				newComponent = false;
			}

			ranges.put(vertex, new SemanticIndexRange(index_counter));
			index_counter++;
		}

		@Override
		public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			//merge all the interval for the current root of the graph
			mergeRangeNode(reference);
		}
		/**  
		 * Merge the indexes of the current connected component 
		 * @param d  is the root node 
		 * */
		private void mergeRangeNode(T d) {
			for (T ch : Graphs.successorListOf(namedDAG, d)) { 
				if (!ch.equals(d)) { 
					mergeRangeNode(ch);

					//merge the index of the node with the index of his child
					ranges.get(d).addRange(ranges.get(ch));
				}
			}
		}
	}

	private <T> Map<T, SemanticIndexRange> createSemanticIndex(DirectedGraph<T, DefaultEdge> dag) {
		DirectedGraph<T, DefaultEdge> reversed = new EdgeReversedGraph<>(dag);
		
		LinkedList<T> roots = new LinkedList<>();
		for (T n : reversed.vertexSet()) {
			if ((reversed.incomingEdgesOf(n)).isEmpty()) {
				roots.add(n);
			}
		}
		
		Map<T,SemanticIndexRange> ranges = new HashMap<>();
		for (T root: roots) {
			// depth-first sort 
			GraphIterator<T, DefaultEdge> orderIterator = new DepthFirstIterator<>(reversed, root);
		
			//add Listener to create the indexes and ranges
			orderIterator.addTraversalListener(new SemanticIndexer<T>(reversed, ranges));
		
			//		System.out.println("\nIndexing:");
			while (orderIterator.hasNext()) 
				orderIterator.next();
		}
		return ranges;
	}
	
	/**
	 * Constructor for the NamedDAGBuilder
	 * @param dag the DAG from which we want to maintain only the named descriptions
	 */

	public static <T> SimpleDirectedGraph <T,DefaultEdge> getNamedDAG(EquivalencesDAG<T> dag) {
		
		SimpleDirectedGraph<T,DefaultEdge> namedDAG = new SimpleDirectedGraph<>(DefaultEdge.class); 

		for (Equivalences<T> v : dag) 
			namedDAG.addVertex(v.getRepresentative());

		for (Equivalences<T> s : dag) 
			for (Equivalences<T> t : dag.getDirectSuper(s)) 
				namedDAG.addEdge(s.getRepresentative(), t.getRepresentative());

		for (Equivalences<T> v : dag) 
			if (!v.isIndexed()) {
				// eliminate node
				for (DefaultEdge incEdge : namedDAG.incomingEdgesOf(v.getRepresentative())) { 
					T source = namedDAG.getEdgeSource(incEdge);

					for (DefaultEdge outEdge : namedDAG.outgoingEdgesOf(v.getRepresentative())) {
						T target = namedDAG.getEdgeTarget(outEdge);

						namedDAG.addEdge(source, target);
					}
				}
				namedDAG.removeVertex(v.getRepresentative());		// removes all adjacent edges as well				
			}
		return namedDAG;
	}
	
	
	/**
	 * Assign indexes for the named DAG, use a depth first listener over the DAG 
	 * @param reasoner used to know ancestors and descendants of the dag
	 */
	
	public SemanticIndexBuilder(TBoxReasoner reasoner)  {
		this.reasoner = reasoner;
		
		//test with a reversed graph so that the smallest index will be given to the higher ancestor
		classRanges = createSemanticIndex(getNamedDAG(reasoner.getClassDAG()));
		opRanges = createSemanticIndex(getNamedDAG(reasoner.getObjectPropertyDAG()));
		dpRanges = createSemanticIndex(getNamedDAG(reasoner.getDataPropertyDAG()));
	}
	
	public int getIndex(OClass d) {
		SemanticIndexRange idx = classRanges.get(d); 
		if (idx != null)
			return idx.getIndex();
		return -1;
	}
	public int getIndex(ObjectPropertyExpression d) {
		SemanticIndexRange idx = opRanges.get(d); 
		if (idx != null)
			return idx.getIndex();
		return -1;
	}
	public int getIndex(DataPropertyExpression d) {
		SemanticIndexRange idx = dpRanges.get(d); 
		if (idx != null)
			return idx.getIndex();
		return -1;
	}
	
	
	public List<Interval> getIntervals(OClass d) {

		Description node = reasoner.getClassDAG().getVertex(d).getRepresentative();
		
		SemanticIndexRange range = classRanges.get(node);
		if (range == null)
			range = new SemanticIndexRange(-1);
		return range.getIntervals();
	}

	public List<Interval> getIntervals(ObjectPropertyExpression d) {

		Description node = reasoner.getObjectPropertyDAG().getVertex(d).getRepresentative();
		
		SemanticIndexRange range = opRanges.get(node);
		if (range == null)
			range = new SemanticIndexRange(-1);
		return range.getIntervals();
	}
	
	public List<Interval> getIntervals(DataPropertyExpression d) {

		Description node = reasoner.getDataPropertyDAG().getVertex(d).getRepresentative();
		
		SemanticIndexRange range = dpRanges.get(node);
		if (range == null)
			range = new SemanticIndexRange(-1);
		return range.getIntervals();
	}
	

	public Set<ClassExpression> getIndexedClasses() {
		return classRanges.keySet();
	}
	public Set<ObjectPropertyExpression> getIndexedObjectProperties() {
		return opRanges.keySet();
	}
	public Set<DataPropertyExpression> getIndexedDataProperties() {
		return dpRanges.keySet();
	}
	
	
	// TEST ONLY
	
	public SemanticIndexRange getRange(OClass d) {
		return classRanges.get(d);
	}
	public SemanticIndexRange getRange(ObjectPropertyExpression d) {
		return opRanges.get(d);
	}
	public SemanticIndexRange getRange(DataPropertyExpression d) {
		return dpRanges.get(d);
	}
}
