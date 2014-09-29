package edu.pitt.isp.treeFragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.pitt.isp.treeAlignment.RTEDwrapper;
import edu.pitt.isp.treeProcessing.addWordCategoriesToTree;
import edu.pitt.isp.treeProcessing.matchLabels;
import edu.stanford.nlp.trees.Tree;

public class Ref_fragmentWithRef {
	
	public boolean hasSingleWord;
	private addWordCategoriesToTree objWordCat;
	
	public Ref_fragmentWithRef(){
		objWordCat = new addWordCategoriesToTree();
		objWordCat.loadWordCategories();
	}
	/**
	 * main function to fragment with Ref 
	 * @param treeRef
	 * @param tree2
	 * @return
	 */
	public List<String> fragmentwithRef(String treeRef, String tree2){
				
		//convert to stanford tree format
		treeRef = Tree.valueOf(unconvertToDrawTable(treeRef)).toString();
		tree2 = Tree.valueOf(unconvertToDrawTable(tree2)).toString();
		
		//unify labels
		matchLabels matchLabelsObj = new matchLabels();
		treeRef = matchLabelsObj.unifyTreeLabels(treeRef);
		tree2 = matchLabelsObj.unifyTreeLabels(tree2);
					
		//convert to stanford tree format
		treeRef = Tree.valueOf(treeRef).toString();
		tree2 = Tree.valueOf(tree2).toString();
		
		//keep words and then add word categories to leaves
		treeRef = objWordCat.addWordCategoriesToParseTree(treeRef); 
		tree2 = objWordCat.addWordCategoriesToParseTree(tree2);
		
		//replace [ ] to ( )
		treeRef = unconvertToDrawTable(treeRef);
		tree2 = unconvertToDrawTable(tree2);
		
		//convert to stanford tree format
		treeRef = Tree.valueOf(treeRef).toString();
		tree2 = Tree.valueOf(tree2).toString();

		//use RTED wrapper to get the mapping and extract the fragments given mapping and tree
		RTEDwrapper objRTEDWrapper = new RTEDwrapper();
		List<String> fragmentTrees = objRTEDWrapper.extractFragmentsFromMapping(treeRef, tree2);
		this.hasSingleWord = objRTEDWrapper.hasSingleWord;
		String RTED = findTED(fragmentTrees);
				
	    return fragmentTrees;
	}

	/**
	 * find TED by refining the output strings
	 * @param refFragments
	 * @return
	 */
	public String findTED(List<String> refFragments) {
		String last = refFragments.get(refFragments.size()-1);
		String[] words = last.split(" ");
		return words[1]; 
	}

	/**
	 * find fragments
	 * @param refFragments
	 * @return
	 */
	public String findFragments(List<String> refFragments) {
		String fragment = "";
		for (int i=0; i<refFragments.size()-1; i++) {
			fragment = fragment + "\t" +  refFragments.get(i);
		}
		return fragment.trim();
	}
	
	private static String unconvertToDrawTable(String tree) {
		tree = tree.replace("[", "(");
		tree = tree.replace("]", ")");
		return tree;
	}

	public List<Integer> calcNumberOfEdges10(List<String> fragments, String parse_MT) {
		//calculate number of edges with 1 and 0 label			
		HashMap<String, String> treeEdges = (new MLbuildTrainFile()).extractAllEdgesOfTree(unconvertToDrawTable(parse_MT));
		HashMap<String, String> fragmentEdges = (new MLbuildTrainFile()).extractAllEdgesOfFragments(fragments);
		List<Integer> out = new ArrayList();;
		int out1 = 0, out0=0;
		for (Entry<String, String> edge : treeEdges.entrySet()) {
    		if(fragmentEdges.containsKey(edge.getKey()))
    			out1++;
    		else
    			out0++;	
		}	
		out.add(out1);
		out.add(out0);
		return out;
	}
}
