package edu.pitt.isp.treeFragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import TED.TED1_1.distance.InfoTree;
import TED.TED1_1.util.LabelDictionary;
import TED.TED1_1.util.LblTree;

public class CompareFragmentSets {
	
	private static final byte POST2_PARENT = 5;
	private static final byte POST2_LABEL = 6;
	private static final byte POST2_LLD = 8;
	private double precision;
	private double recall;
	private double exactMatch;

	HashMap<String, Integer> nodesFragment1 = new HashMap<String, Integer>();
	HashMap<String, Integer> nodesFragment2 = new HashMap<String, Integer>();
	HashMap<String, Integer> nodesErrorTree = new HashMap<String, Integer>();
	HashMap<String, String> fragmentEdges1 = new HashMap<String, String>();
	HashMap<String, String> fragmentEdges2 = new HashMap<String, String>();
	HashMap<String, String> fragmentEdgesErrorTree = new HashMap<String, String>();
	
	public CompareFragmentSets() {
	}
	
	public CompareFragmentSets(List<String> fragments1, List<String> fragments2, List<String> errorTree) {
		fragmentEdges1.clear();
		fragmentEdges2.clear();
		nodesFragment1.clear();
		nodesFragment2.clear();
		fragmentEdgesErrorTree.clear();
		nodesErrorTree.clear();
		
		fragmentEdges1 = extractAllEdgesOfFragments(fragments1, nodesFragment1); 
		fragmentEdges2 = extractAllEdgesOfFragments(fragments2, nodesFragment2);
		fragmentEdgesErrorTree = extractAllEdgesOfFragments(errorTree, nodesErrorTree);
	}
	
	/**
	 * use TED to find the maximum  match between two sets of fragments
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public Double compareFragmentsTED(List<String> fragments1, List<String> fragments2){
		return 0.0;
	}
	
	/**
	 * using the following link to implement it:
	 * http://nlp.stanford.edu/IR-book/html/htmledition/evaluation-of-clustering-1.html#tab:clmeascomp
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public Double compareFragmentsNormalizedMutualInformation(List<String> fragments1, List<String> fragments2) {
		HashMap<String, List<Double>> allPrecisionRecalls = new HashMap<String, List<Double>>();
		List<Double> sharedEdgesList = new ArrayList<Double>();
		List<Double> PrecisionRecall; 
		double sharedEdges = 0;
		double MI = 0.0; 
		double MItemp = 0.0;
		exactMatch = 0; 
		//TODO: I can change this parameter to the size of gold standard fragments for example: fragmentEdges2.size()
		int N = Math.max(fragmentEdgesErrorTree.size(), fragmentEdges2.size() );
		for(int i=0; i<fragments1.size(); i++){
			HashMap<String, String> fragmentEdges1i = extractAllEdgesOfFragments(Arrays.asList(fragments1.get(i)), new HashMap<String, Integer>() );
			for(int j=0; j<fragments2.size(); j++){
				HashMap<String, String> fragmentEdges2j = extractAllEdgesOfFragments(Arrays.asList(fragments2.get(j)), new HashMap<String, Integer>());
				
				PrecisionRecall = findSharedEdges(Arrays.asList(fragments1.get(i)), Arrays.asList(fragments2.get(j)));
				if (PrecisionRecall.get(0) == 0 || PrecisionRecall.get(1) == 0 || PrecisionRecall.get(2) == 0)
					continue;
				allPrecisionRecalls.put(i + "-" + j, PrecisionRecall);
				sharedEdgesList.add(PrecisionRecall.get(3)); //only shared edges
				if(allPrecisionRecalls.get(i + "-" + j).get(2) == 1)
					exactMatch++;
				
				if(PrecisionRecall.get(3) == 0 || fragmentEdges1i.size() == 0 || fragmentEdges2j.size() == 0)
					MItemp = 0.0;
				else
					MItemp = (double) ((PrecisionRecall.get(3)/N) * 
						(Math.log((N*PrecisionRecall.get(3)) / (fragmentEdges1i.size()*fragmentEdges2j.size()) )));
				 
				MI = MI + MItemp; 
				
			}
			//for each fragment of set1, find a fragment from the second set that have the maximum shared edge
			if (sharedEdgesList.size()<1)
				continue;
			double max = Collections.max(sharedEdgesList);
			sharedEdges = sharedEdges + max; // findMax(sharedEdgesList);
			sharedEdgesList.clear();
		} 
		double H1 = calculateEntropy(fragments1, fragmentEdges1, N);
		double H2 = calculateEntropy(fragments2, fragmentEdges2, N);
		
		double NMI = (MI / ((H1+H2)/2));
		if(MI == 0.0)
			NMI = 0.0;
		else if (H1 == 0.0 && H2 == 0.0)
			NMI = 0.0;
		else if(Double.isNaN(NMI)){
			System.out.println(NMI);
			System.out.println(MI+ "\t"+ H1 + "\t" + H2);
			System.exit(0);
		}
		return NMI;	
	}
	
	private double calculateEntropy(List<String> fragments, HashMap<String, String> fragmentEdges, int N) {
		double H = 0.0;  
		N = fragmentEdges.size();
		for(int i=0; i<fragments.size(); i++ ){
			HashMap<String, String> fragmentEdges1i = extractAllEdgesOfFragments(Arrays.asList(fragments.get(i)), new HashMap<String, Integer>());
			if(fragmentEdges1i.size() == 0)
				continue;
			H = H + (((double) fragmentEdges1i.size()/N) * (Math.log((double) fragmentEdges1i.size()/N)) );
		} 	
		if (Double.isNaN(H))
			H = 0.0 ;
		if(H == 0.0)
			return H;
		return - H;
	}
 
	/**
	 * calculate Adjusted Rand Index
	 * I use shared edges between each two fragments for n_ij similar to F-measure
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public Double compareFragmentsAdjustedRandIndexSharedEdges(List<String> fragments1, List<String> fragments2){
		HashMap<String, Double> allPrecisionRecalls = new HashMap<String, Double>();
		List<Double> PrecisionRecall; 
		int sum_n_ij = 0, sum_a_i = 0, sum_b_j = 0;
		for(int i=0; i<fragments1.size(); i++){
			for(int j=0; j<fragments2.size(); j++){
				PrecisionRecall = findSharedEdges(Arrays.asList(fragments1.get(i)), Arrays.asList(fragments2.get(j)));
	
				//fill the contingency table and its sums
				allPrecisionRecalls.put(i + "-" + j, PrecisionRecall.get(3));
				if (!allPrecisionRecalls.containsKey("a-" + i)){
					allPrecisionRecalls.put("a-" + i, PrecisionRecall.get(3));
				} else {
					allPrecisionRecalls.put("a-" + i, allPrecisionRecalls.get("a-" + i) + PrecisionRecall.get(3));
				}
				
				if(!allPrecisionRecalls.containsKey("b-" + j)){
					allPrecisionRecalls.put("b-" + j, PrecisionRecall.get(3));
				} else {
					allPrecisionRecalls.put("b-" + j, allPrecisionRecalls.get("b-" + j) + PrecisionRecall.get(3));
				}		
				
				if(allPrecisionRecalls.get(i + "-" + j) == 0.0)
					continue;
				sum_n_ij = sum_n_ij + combination((int) Math.round(allPrecisionRecalls.get(i + "-" + j)), 2);
			}
		}
		//calculate ARI
		int N = Math.max(fragmentEdgesErrorTree.size(), fragmentEdges2.size() );	
		//int N = fragmentEdges2.size();
				
		for(int i=0; i<fragments1.size(); i++){
			sum_a_i = sum_a_i + combination((int) Math.round(allPrecisionRecalls.get("a-" + i)), 2);
		}
		for(int j=0; j<fragments2.size(); j++){
			sum_b_j = sum_b_j + combination((int) Math.round(allPrecisionRecalls.get("b-" + j)), 2);
		}
		double nom = sum_n_ij - ( (sum_a_i * sum_b_j ) / combination(N, 2) ) ;
		double denom = ((sum_a_i + sum_b_j ) /2) -  ( (sum_a_i * sum_b_j ) / combination(N, 2) )  ;
		double ARI = (double) nom/denom;
		if(nom == 0 || denom == 0 )
			return 0.0;
		return ARI;
	}
	
	/** 
	 * using shared nodes between fragments
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public Double compareFragmentsAdjustedRandIndexSharedNodes(List<String> fragments1, List<String> fragments2){
		HashMap<String, Double> allPrecisionRecalls = new HashMap<String, Double>();
		List<Double> PrecisionRecall; 
		int sum_n_ij = 0, sum_a_i = 0, sum_b_j = 0;
		for(int i=0; i<fragments1.size(); i++){
			for(int j=0; j<fragments2.size(); j++){
				PrecisionRecall = findSharedNodes(Arrays.asList(fragments1.get(i)), Arrays.asList(fragments2.get(j)));
	
				//fill the contingency table and its sums
				allPrecisionRecalls.put(i + "-" + j, PrecisionRecall.get(3));
				if (!allPrecisionRecalls.containsKey("a-" + i)){
					allPrecisionRecalls.put("a-" + i, PrecisionRecall.get(3));
				} else {
					allPrecisionRecalls.put("a-" + i, allPrecisionRecalls.get("a-" + i) + PrecisionRecall.get(3));
				}
				
				if(!allPrecisionRecalls.containsKey("b-" + j)){
					allPrecisionRecalls.put("b-" + j, PrecisionRecall.get(3));
				} else {
					allPrecisionRecalls.put("b-" + j, allPrecisionRecalls.get("b-" + j) + PrecisionRecall.get(3));
				}		
				
				if(allPrecisionRecalls.get(i + "-" + j) == 0.0)
					continue;
				sum_n_ij = sum_n_ij + combination((int) Math.round(allPrecisionRecalls.get(i + "-" + j)), 2);
			}
		}
		//calculate ARI
		int N = Math.max(nodesErrorTree.size(), nodesFragment2.size() );	
				
		for(int i=0; i<fragments1.size(); i++){
			sum_a_i = sum_a_i + combination((int) Math.round(allPrecisionRecalls.get("a-" + i)), 2);
		}
		for(int j=0; j<fragments2.size(); j++){
			sum_b_j = sum_b_j + combination((int) Math.round(allPrecisionRecalls.get("b-" + j)), 2);
		}
		double nom = sum_n_ij - ( (sum_a_i * sum_b_j ) / combination(N, 2) ) ;
		double denom = ((sum_a_i + sum_b_j ) /2) -  ( (sum_a_i * sum_b_j ) / combination(N, 2) )  ;
		double ARI = (double) nom/denom;
		if(nom == 0 || denom == 0 )
			return 0.0;
		return ARI;
	}
	/**
	 * Rand-Index which is a Pairwise-counting , Cluster measure
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public Double compareFragmentsRandIndex(List<String> fragments1, List<String> fragments2) {
		double countSameCluster = 0;
		for(String node11 : nodesFragment1.keySet()){
			for(String node12: nodesFragment1.keySet()){
				if(node11.equals(node12)) continue;
				if(nodesFragment2.containsKey(node11) && nodesFragment2.containsKey(node12) && twoNodesHaveSameClusterInBothSets(node11, node12)){
					countSameCluster++;
				}
			}
		}
		countSameCluster = countSameCluster/2;
		int maxSize = nodesFragment2.size();
		int allPairs = combination(maxSize, 2);
		double similarity = countSameCluster/allPairs;
		return similarity;
	}
	
	private boolean twoNodesHaveSameClusterInBothSets(String node11, String node12) {
		if(nodesFragment1.get(node11) == nodesFragment1.get(node12) && nodesFragment2.get(node11) == nodesFragment2.get(node12))
			return true;
		else if(nodesFragment1.get(node11) != nodesFragment1.get(node12) && nodesFragment2.get(node11) != nodesFragment2.get(node12))
			return true;
		return false;
	}

	/**
	 * Compare two sets by finding shared edges
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public double compareFragmentsFmeasureSharedEdges(List<String> fragments1, List<String> fragments2) {
		HashMap<String, List<Double>> allPrecisionRecalls = new HashMap<String, List<Double>>();
		List<Double> sharedEdgesList = new ArrayList<Double>();
		List<Double> sharedNodesList = new ArrayList<Double>();
		List<Double> PrecisionRecall;  
		double sharedEdges = 0; 
		double sumSharedNodes = 0.0;
		exactMatch = 0;
		for(int i=0; i<fragments1.size(); i++){
			for(int j=0; j<fragments2.size(); j++){
				PrecisionRecall = findSharedEdges(Arrays.asList(fragments1.get(i)), Arrays.asList(fragments2.get(j)));
				if (PrecisionRecall.get(0) == 0 || PrecisionRecall.get(1) == 0)
					continue;
				allPrecisionRecalls.put(i + "-" + j, PrecisionRecall);
				sharedEdgesList.add(PrecisionRecall.get(3)); //only shared edges
				sharedNodesList.add(PrecisionRecall.get(4)); //because there are fragments with only one node.
				if(allPrecisionRecalls.get(i + "-" + j).get(2) == 1)
					exactMatch++;
			}
			//for each fragment of set1, find a fragment from the second set that have the maximum shared edge
			if (sharedEdgesList.size()<1)
				continue;
			double max = Collections.max(sharedEdgesList);
			sharedEdges = sharedEdges + max;
			sumSharedNodes = sumSharedNodes + sharedNodesList.get(sharedEdgesList.indexOf(max));
			sharedEdgesList.clear();
			sharedNodesList.clear();

		}
		if (fragmentEdges1.size() == 0)
			precision = 0.0;
		else
			precision = sharedEdges/( fragmentEdges1.size() + sumSharedNodes ) ;
		if(fragmentEdges2.size() == 0)
			recall = 0.0;
		else
			recall = sharedEdges/ ( fragmentEdges2.size() + sumSharedNodes ) ;		
		double totalFMeasure = 2*(precision*recall)/(precision+recall);
		if(sharedEdges == 0 || Double.isNaN(totalFMeasure))
			totalFMeasure = 0;
		if(totalFMeasure > 1.0)   
			totalFMeasure = 1.0;
		return totalFMeasure;
	} 

	/**
	 * calculate F-measure by counting shared nodes between fragments
	 * @param fragments1
	 * @param fragments2
	 * @return
	 */
	public double compareFragmentsFmeasureSharedNodes(List<String> fragments1, List<String> fragments2) {
		HashMap<String, List<Double>> allPrecisionRecalls = new HashMap<String, List<Double>>();
		List<Double> sharedEdgesList = new ArrayList<Double>();
		List<Double> PrecisionRecall; 
		double sharedEdges = 0;
		exactMatch = 0;
		for(int i=0; i<fragments1.size(); i++){
			for(int j=0; j<fragments2.size(); j++){
				PrecisionRecall = findSharedNodes(Arrays.asList(fragments1.get(i)), Arrays.asList(fragments2.get(j)));
				if (PrecisionRecall.get(0) == 0 || PrecisionRecall.get(1) == 0)
					continue;
				allPrecisionRecalls.put(i + "-" + j, PrecisionRecall);
				sharedEdgesList.add(PrecisionRecall.get(3)); //only shared edges
				if(allPrecisionRecalls.get(i + "-" + j).get(2) == 1)
					exactMatch++;
			}
			//for each fragment of set1, find a fragment from the second set that have the maximum shared edge
			if (sharedEdgesList.size()<1)
				continue;
			double max = Collections.max(sharedEdgesList);
			sharedEdges = sharedEdges + max; // findMax(sharedEdgesList);
			sharedEdgesList.clear();

		}
		if (nodesFragment1.size() == 0)
			precision = 0.0;
		else
			precision = sharedEdges/nodesFragment1.size();
		if(nodesFragment2.size() == 0)
			recall = 0.0;
		else
			recall = sharedEdges/nodesFragment2.size();	
		double totalFMeasure = 2*(precision*recall)/(precision+recall);
		if(sharedEdges == 0)
			totalFMeasure = 0;
		if(recall == 0.0 && precision == 0.0)
			totalFMeasure = 0;
		return totalFMeasure;
	}
	public double getPrecision(){
		return precision;
	}
	
	public double getRecall(){
		return recall;
	}
	
	public double getExactMatch(){
		return exactMatch;
	}
	
	public List<Double> findSharedEdges(List<String> fragments1, List<String> fragments2) {
		HashMap<String, Integer> fragmentNodes1 =  new HashMap<String, Integer>();
		HashMap<String, Integer> fragmentNodes2 =  new HashMap<String, Integer>();
		HashMap<String, String> fragmentEdges1 = extractAllEdgesOfFragments(fragments1, fragmentNodes1);
		HashMap<String, String> fragmentEdges2 = extractAllEdgesOfFragments(fragments2, fragmentNodes2);
		
		int sharedEdges = 0;
		int sharedNodes = 0;
		for(String edge : fragmentEdges1.keySet()){
			if(fragmentEdges2.containsKey(edge) && fragmentEdges1.get(edge).equals(fragmentEdges2.get(edge))){
				sharedEdges++;
			}			
		}	
		// if a fragment does not have any edge match it with nodes, because there are some cases that the fragments are only pos-word and they should match with others
		if(fragmentEdges1.size() == 0 || fragmentEdges2.size() == 0){
			sharedEdges = (int) Math.round(findSharedNodes(fragments1, fragments2).get(3));
			sharedNodes = sharedEdges;
		}
		
		List<Double> PrecisionRecall = new ArrayList<Double>();
		double precision = (double) sharedEdges/fragmentEdges1.size();
		double recall = (double) sharedEdges/fragmentEdges2.size();
		double fMeasure = 2*(precision*recall)/(precision+recall);
		PrecisionRecall.add(precision);
		PrecisionRecall.add(recall);
		PrecisionRecall.add(fMeasure);
		PrecisionRecall.add((double) sharedEdges);
		PrecisionRecall.add((double) sharedNodes);
		return PrecisionRecall;
	}

	public List<Double> findSharedNodes(List<String> fragments1, List<String> fragments2) {
		HashMap<String, Integer> fragmentNodes1 =  new HashMap<String, Integer>();
		HashMap<String, Integer> fragmentNodes2 =  new HashMap<String, Integer>();
		HashMap<String, String> fragmentEdges1 = extractAllEdgesOfFragments(fragments1, fragmentNodes1);
		HashMap<String, String> fragmentEdges2 = extractAllEdgesOfFragments(fragments2, fragmentNodes2);
		
		int sharedNodes = 0;
		for(String node : fragmentNodes1.keySet()){
			if(fragmentNodes2.containsKey(node) && fragmentNodes1.get(node).equals(fragmentNodes2.get(node))){
				sharedNodes++;
			} 
		}	
		List<Double> PrecisionRecall = new ArrayList<Double>();
		double precision = (double) sharedNodes/fragmentNodes1.size();
		double recall = (double) sharedNodes/fragmentNodes2.size();
		double fMeasure = 2*(precision*recall)/(precision+recall);
		PrecisionRecall.add(precision);
		PrecisionRecall.add(recall);
		PrecisionRecall.add(fMeasure);
		PrecisionRecall.add((double) sharedNodes);
		return PrecisionRecall;
	}
	
	/**
	 * The edges format is : (paretntID_childID , parentTag_childTag) 
	 * @param fragments
	 * @return
	 */
	public static HashMap<String, String> extractAllEdgesOfFragments( List<String> fragments, HashMap<String, Integer> nodesFragment) {		
		HashMap<String, String> edges = new HashMap<String, String>();
 
		for(int i=0; i<fragments.size() ; i++){ 
			String frag = fragments.get(i); 
			int count = countOccurrences(frag, ']');
			int countQ = countOccurrences(frag, '?');
			if (count - countQ > 0){ // the fragment has more than one node
				frag = unconvertToDrawTable(frag);
				
				String tt = convertToAlignmentFormat(frag);
				//This part is added because LblTree has problem with these two symbols 
				if(tt.contains(";") || tt.contains(":")){
					tt = tt.replace(";", ",");
					tt = tt.replace(":", ",");
				}
				LblTree lt1 = LblTree.fromString(tt);
				LabelDictionary ld = new LabelDictionary();
				InfoTree it1 = new InfoTree(lt1, ld);
				
				for(int j=0; j<it1.info[POST2_PARENT].length; j++ ){
					if(it1.info[POST2_PARENT][j] > 0){
						int pp = it1.info[POST2_PARENT][j]; 
						String parentTag = ld.read(it1.info[POST2_LABEL][pp]).split("_")[0];
						String childTag = ld.read(it1.info[POST2_LABEL][j]).split("_")[0];
						if(childTag.contains("?"))
							continue;
						String parentID = ld.read(it1.info[POST2_LABEL][pp]).split("_")[ld.read(it1.info[POST2_LABEL][pp]).split("_").length-1];
						String childID = ld.read(it1.info[POST2_LABEL][j]).split("_")[ld.read(it1.info[POST2_LABEL][j]).split("_").length-1];
						if(childTag.equals("-0-") || parentTag.equals("-NONE-") || childTag.equals("-NONE-") || childID.contains("?") || parentID.length()<1)
							continue;
						//if the child is a leaf, do not consider its ID and even do not consider that edge from POS to word
						int childrenNo = getNumberOfChildren(it1, j);
						if(childrenNo == 0 &&  ld.read(it1.info[POST2_LABEL][j]).contains("__")){
							 childID = "0";  
							 //12102013: it is not a valid edge but add the parent to the nodesFragment
							 nodesFragment.put(parentTag + "_" + parentID, i);
							 continue; 
						}
						if(parentTag.contains("?") && !parentTag.contains("_?_")){
							nodesFragment.put(childTag + "_" + childID, i);
							continue;
						}
						//if(!childID.equals("0") && edges.containsKey(parentID + "_" + childID))
							//System.out.println("contains key");
						edges.put(parentID + "_" + childID, parentTag + "_" + childTag);	
						nodesFragment.put(childTag + "_" + childID, i);
						if(!nodesFragment.containsKey(parentTag + "_" + parentID)){
							nodesFragment.put(parentTag + "_" + parentID, i);
						}
					} 
				}
			}
		}
		return edges;
	}
	
	private static int getNumberOfChildren(InfoTree it22, int node) {
		int count = 0;
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == node)
				count++;
		}	
		return count;
	}
    public static int countOccurrences(String haystack, char needle)
    {
        int count = 0;
        for (int i=0; i < haystack.length(); i++)
        {
            if (haystack.charAt(i) == needle)
            {
                 count++;
            }
        }
        return count;
    }
	private static String unconvertToDrawTable(String tree) {
		tree = tree.replace("[", "(");
		tree = tree.replace("]", ")");
		return tree;
	}
	private static String convertToAlignmentFormat(String tree) {
		tree = tree.replace("(", "{");
		tree = tree.replace(")", "}");
		return tree;
	}


	private int combination(int n, int k)
	{
	    if(n>>1 < k)
	        k = n - k;
	    double mul = 1;
	    for(int i=n+1-k;i<n+1;i++)
	        mul *= i;
	    for(int i=k;i>0;i--)
	        mul /= i;
	    return (int) Math.round(mul);
	}

}
