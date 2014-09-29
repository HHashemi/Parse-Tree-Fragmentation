package edu.pitt.isp.treeFragments;

import java.util.HashMap;
import java.util.List;

import TED.TED1_1.distance.InfoTree;
import TED.TED1_1.util.LabelDictionary;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;

public class postProcessingFragments {
	
	 private static final byte POST2_PARENT = 5;
	 private static final byte POST2_LABEL = 6;
	 private static final byte POST2_LLD = 8;
	 private LabelDictionary ld;
	 private String ts1;
	 String treeFrag;
	 private int[] fragment;
	 private int maxFrag;
	 private int[][]  alignment;
	
	HashMap<Integer, Integer> mapID = new HashMap<Integer,Integer>(); //tree id to post-order id
	HashMap<Integer, Integer> mapIDRev = new HashMap<Integer,Integer>(); //post-order id to tree id

	HashMap<Integer, String> mapIDLabel = new HashMap<Integer,String>(); // nodeLable
	HashMap<Integer, String> mapIDHead = new HashMap<Integer,String>(); // nodeHead
	HashMap<Integer, String> mapIDHeadAligned = new HashMap<Integer,String>(); // nodeHead in the Aligned Good Tree
	
	/**
	 * We should care about the head of good tree, not Bad tree. 
	 * So, this method is added to consider head of a non-terminal in a good tree.
	 * @param ld
	 * @param it2
	 * @param treeFrag
	 * @param fragments
	 * @param alignment 
	 * @return
	 */
	public int[] checkParseTreeHeadsBasedOnReferenceTree(LabelDictionary ld, InfoTree it1, InfoTree it2, String ts1, String ts2, int[] fragments, int[][] alignment) {	
		this.ld = ld;
		this.fragment = fragments;
		this.alignment = alignment;
		this.ts1 = ts1;
		initialTreeInfo(ts2);
		
		int noFrag = findMaxFragment(fragment);
		maxFrag = noFrag;
		for(int frag=1; frag<noFrag+1; frag++){
			int[] specificFrag = new int[fragment.length];
			
			for(int i=0; i<fragment.length; i++ ){
				if(fragment[i] == frag){
					specificFrag[i] = fragment[i];
				}
			}
			
			for(int i=0; i<it2.info[POST2_PARENT].length ; i++){
				if(specificFrag[i] != frag)
					continue;
				if(isHeadAmongChildrenAlignedGoodTree(i,it2,specificFrag))
					continue;
				else{ //fragment its children
					newFragmentForChildren(i, it2, specificFrag);
					
				}
			}		
		}
		return fragment;
	}
	/**
	 * For fragmentation with Reference tree method
	 * @param ld
	 * @param it2
	 * @param treeFrag
	 * @param fragments
	 * @return
	 */
	public int[] checkParseTreeHeads(LabelDictionary ld, InfoTree it2, String ts1, String treeFrag, int[] fragments) {		
		this.ld = ld;
		this.treeFrag =treeFrag;
		this.fragment = fragments;
		this.ts1 = ts1;
		
		initialTreeInfo(treeFrag);
		
		int noFrag = findMaxFragment(fragment);
		maxFrag = noFrag;
		for(int frag=1; frag<noFrag+1; frag++){
			int[] specificFrag = new int[fragment.length];
			
			for(int i=0; i<fragment.length; i++ ){
				if(fragment[i] == frag){
					specificFrag[i] = fragment[i];
				}
			}
			
			for(int i=0; i<it2.info[POST2_PARENT].length ; i++){
				if(specificFrag[i] != frag)
					continue;
				if(isIHeadAmongChildren(i,it2,specificFrag))
					continue;
				else{ //fragment its children
					isIHeadAmongChildren(i,it2,specificFrag);
					newFragmentForChildren(i, it2, specificFrag);
					
				}
			}		
		}
		return fragment;
	}
	

	/**
	 * main part of this class...
	 * separate the children that are not associated in the parent's head tag
	 * @param i
	 * @param it2
	 * @param specificFrag
	 * @param maxFrag 
	 */
	private void newFragmentForChildren(int node, InfoTree it2, int[] specificFrag) {
		String label = ld.read(it2.info[POST2_LABEL][node]);
		for(int i=it2.info[POST2_PARENT].length-1 ; i >= 0; i--){
			if (it2.info[POST2_PARENT][i] == node && fragment[i] == fragment[node]){
				maxFrag++;
				fragment[i] = maxFrag;
				for(int j=it2.info[POST2_PARENT].length-2 ; j >= 0; j--){
					if (fragment[it2.info[POST2_PARENT][j]] == maxFrag && fragment[j] == fragment[node]){
						fragment[j] = maxFrag;
					}
				}		
			}
		}		
	}

	/**
	 * Check the head of i with its children
	 * @param i
	 * @param it2
	 * @param fragment
	 * @return
	 */
	private boolean isIHeadAmongChildren(int node, InfoTree it2, int[] fragment) {
		String head = mapIDHead.get(node+1);
		String label = ld.read(it2.info[POST2_LABEL][node]);
		int childrenNo = getNumberOfChildren(it2, node);
		if(childrenNo == 0)
			return true;
		if(head == null)
			return true;
		for(int j=0; j<it2.info[POST2_PARENT].length ; j++){
			if (it2.info[POST2_PARENT][j] == node && head.equals(mapIDHead.get(j)) && fragment[j] != 0){
				return true;
			}
		}
		return false;
	}

	private boolean isHeadAmongChildrenAlignedGoodTree(int node, InfoTree it2, int[] fragment) {
		String head2 = mapIDHead.get(node+1);
		String head1 = mapIDHeadAligned.get(node+1);
		String label = ld.read(it2.info[POST2_LABEL][node]);
		int childrenNo = getNumberOfChildren(it2, node);
		if(childrenNo == 0)
			return true;
		if(head2 == null)
			return true;
		if(head1.equals(head2) && label.equals(head1))
			return true;
		
		//if the head of aligned node is among the children of the bad tree, it is OK
		for(int j=0; j<it2.info[POST2_PARENT].length ; j++){
			if (it2.info[POST2_PARENT][j] == node && head1.equals(mapIDHead.get(j+1)) && fragment[j] != 0){
				return true;
			}
		}		
		return false;
	}
	
	/**
	 * Initialize tree string
	 * @param treeStr
	 */
	private void initialTreeInfo(String ts2) {		
		mapID.clear();
		mapIDRev.clear();
		mapIDLabel.clear();
		mapIDHead.clear();
		
		CollinsHeadFinder hf = new CollinsHeadFinder();
		
		ts1 = unconvertToDrawTable(ts1);
		ts2 = unconvertToDrawTable(ts2);
		Tree tree1 = Tree.valueOf(ts1);
		Tree tree2 = Tree.valueOf(ts2);
		List<Tree> postOrder1 = tree1.postOrderNodeList();
		int postID = 1;
		HashMap<Integer, Tree> mapIDRev1 = new HashMap<Integer,Tree>(); //post-order id to tree id
		for(Tree node: postOrder1){
				mapIDRev1.put(postID, node);
				postID++;
		}
		List<Tree> postOrder2 = tree2.postOrderNodeList();
		postID = 1;
		for(Tree node: postOrder2){
			mapID.put(node.nodeNumber(tree2), postID);
			mapIDRev.put(postID, node.nodeNumber(tree2));
			postID++;
		}
		
		String label2, label1;
		Tree node2;
		int count = 1;
		String head2, head2preTerminal;
		for(int i=1; i<=tree2.size(); i++){
			node2 = tree2.getNodeNumber(mapIDRev.get(i));
			label2 = node2.value();
			head2 = node2.headTerminal(hf).value();
			if(!node2.isLeaf()){
				head2 = node2.headPreTerminal(hf).value();
			}
			mapIDLabel.put(count, label2);
			mapIDHead.put(count, head2);
				
			// new part to add head of aligned node in reference tree as the head of bad tree
			String head1 = head2;
			if(alignment[1][count] != 0){					
					int aligned = alignment[1][count];
					Tree node1 = mapIDRev1.get(aligned);
					label1 = node1.value();
					head1 = node1.headTerminal(hf).value();
					if(!node1.isLeaf())
						head1 = node1.headPreTerminal(hf).value();
			}
			mapIDHeadAligned.put(count, head1);
				
			count++;
		}
		
	}

	private String unconvertToDrawTable(String tree) {
		tree = tree.replace("[", "(");
		tree = tree.replace("]", ")");
		tree = tree.replace("{", "(");
		tree = tree.replace("}", ")");
		return tree;
	}
	
	private int findMaxFragment(int[] fragment) {
	    int maxValue = fragment[0];  
	    for(int i=1;i<fragment.length;i++){  
	        if(fragment[i] > maxValue){  
	            maxValue = fragment[i];  
	        }  
	    }  
	    return maxValue;  
	}

	private int getNumberOfChildren(InfoTree it22, int node) {
		int count = 0;
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == node)
				count++;
		}	
		return count;
	}

	/**
	 * Remove fragments that do not have any leaf nodes
	 * @param ld
	 * @param it2
	 * @param it22 
	 * @param treeFrag
	 * @param ts2 
	 * @param fragments
	 * @param alignment2 
	 * @return
	 */
	public int[] checkPreTerminalsInFragments(LabelDictionary ld, InfoTree it1, InfoTree it2, String ts1, String ts2, int[] fragments, int[][] alignment) {
		this.ld = ld;
		this.fragment = fragments;
		this.alignment = alignment;
		this.ts1 = ts1;
		initialTreeInfo(ts2);
		
		int noFrag = findMaxFragment(fragment);
		maxFrag = noFrag;
		boolean flag;
		int countNodes = 0;
		String label;
		for(int frag=1; frag<noFrag+1; frag++){
			int[] specificFrag = new int[fragment.length];
			countNodes = 0;
			for(int i=0; i<fragment.length; i++ ){
				if(fragment[i] == frag){
					specificFrag[i] = fragment[i];
					countNodes++;
				}
			}
			
			flag = false;
			for(int i=0; i<it2.info[POST2_PARENT].length ; i++){
				if(specificFrag[i] != frag)
					continue;
				int childrenNo = getNumberOfChildren(it2, i);
				//if number of children is 0, it means that this node is a leaf, so this fragment has a preterminal attached to it. 
				if(childrenNo == 0){
					label = ld.read(it2.info[POST2_LABEL][i]);
					flag = true;
					break;
				}
			}	
			if(flag == false && countNodes > 0){
				fragmentStatistics.countNotConnectedFragment++;
				removeNotConnectedFragment(frag);
			}
		}
		return fragment;		
	}

	/**
	 * if there is a terminal that is not in a fragment and it is not aligned, put it in a separate fragment
	 * @param ld2
	 * @param it2
	 * @param it22 
	 * @param ts2
	 * @param ts22 
	 * @param fragment2
	 * @param alignment2
	 * @return
	 */
	public int[] checkAllTerminalsAreFragments(LabelDictionary ld, InfoTree it1, InfoTree it2, String ts1, String ts2, int[] fragments, int[][] alignment) {
		this.ld = ld;
		this.fragment = fragments;
		this.alignment = alignment;
		this.ts1 = ts1;
		initialTreeInfo(ts2);
		
		int noFrag = findMaxFragment(fragment);
		maxFrag = noFrag;
		String label;
		for(int i=0; i<alignment[1].length-1; i++){	
    		label = ld.read(it2.info[POST2_LABEL][i]);
    		if(alignment[1][i+1] == 0 && it2.info[POST2_LLD][i] == i){
    			maxFrag++;
    			fragment[i] = maxFrag;
    		}
    	}
		return fragment;
	}
	
	public int[] checkAllTerminalsAreWithPOS(LabelDictionary ld, InfoTree it1, InfoTree it2, String ts1, String ts2, int[] fragments, int[][] alignment) {
		this.ld = ld;
		this.fragment = fragments;
		this.alignment = alignment;
		this.ts1 = ts1;
		initialTreeInfo(ts2);
		
		int noFrag = findMaxFragment(fragment);
		maxFrag = noFrag;
		String label, labelparent;
		int parent;
		for(int frag=1; frag<noFrag+1; frag++){
			int[] specificFrag = new int[fragment.length];
			
			for(int i=0; i<fragment.length; i++ ){
				if(fragment[i] == frag){
					specificFrag[i] = fragment[i];
				}
			}
			
			for(int i=0; i<it2.info[POST2_PARENT].length ; i++){
				label = ld.read(it2.info[POST2_LABEL][i]);
				if(specificFrag[i] != frag)
					continue;
				int childrenNo = getNumberOfChildren(it2, i);
				//if number of children is 0, it means that this node is a leaf, so this fragment has a preterminal attached to it.
				//node i is terminal check whether it is in a same fragment with its parent otherwise put them in a same fragment
				if(childrenNo == 0){ 	
					parent = it2.info[POST2_PARENT][i];
					labelparent = ld.read(it2.info[POST2_LABEL][parent]);
					if(fragment[i] != fragment[parent]){
		    			maxFrag++;
		    			fragment[i] = maxFrag;
		    			fragment[parent] = maxFrag;
					}
				}
			}		
		}
		
		return fragment;
	}

	private void removeNotConnectedFragment(int frag) {
		for(int i=0; i<fragment.length ; i++){
			if(fragment[i] == frag)
				fragment[i] = 0;
		}		
	}


	private String printFragment(InfoTree it22, int[] fragment) {
		String[] treeArr = new String[fragment.length];
		for(int i=0; i<fragment.length; i++ ){
			if(fragment[i] != 0){
				treeArr[i] = ld.read(it22.info[POST2_LABEL][i]) + "_" + (i+1);
			}
		}
		
		for(int i=0; i<fragment.length; i++){
			// if number of its children is 0, it is a leaf
			int childrenNo = getNumberOfChildren(it22, i);
			if (childrenNo == 0 && treeArr[i] != null){
				treeArr[i] = "[" + treeArr[i] + "]";
				continue;
			} else if (childrenNo == 0 && treeArr[i] == null){
				continue;
			} else if (areAllChildrenNull(it22, i, treeArr)){   // if all the children are null, continue
				continue;
			} else {
				String treeStr;
				if(treeArr[i] == null)
					treeArr[i]="?";
				treeStr = "[" + treeArr[i] ;
				// for each of its children
				for(int t2=0; t2<it22.info[POST2_PARENT].length ; t2++){								
					if (it22.info[POST2_PARENT][t2] == i && treeArr[t2] != null){
												
						if(!treeArr[t2].startsWith("["))
							treeArr[t2] = "[" + treeArr[t2] + "]";
						treeStr = treeStr + treeArr[t2];
						
					}
				}					
				treeStr = treeStr + "]";
				treeArr[i] = treeStr;
			}
		}	
		
		for(int i=treeArr.length-1 ; i>=0 ; i--){
			if(treeArr[i] != null){
				return treeArr[i]; 
			}
		}
		return null;
	}

	private boolean areAllChildrenNull(InfoTree it22, int node, String[] treeArr) {
		int count = 0;
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == node && (treeArr[i] != null && !treeArr[i].startsWith("[?["))){
				count++;
			}
		}	
		if(count == 0)
			return true;
		else
			return false;
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


	
}
