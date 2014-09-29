package edu.pitt.isp.treeAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.isp.treeFragments.postProcessingFragments;
import edu.stanford.nlp.ie.pascal.Alignment;
import edu.stanford.nlp.trees.Tree;
import TED.TED1_1.convenience.RTED;
import TED.TED1_1.distance.InfoTree;
import TED.TED1_1.util.LabelDictionary;
import TED.TED1_1.util.LblTree;

/**
 * This is a wrapper class for RTED package (TED.TED1_1)
 * downloaded from : http://www.inf.unibz.it/dis/projects/tree-edit-distance/tree-edit-distance.php
 * @author hashemi
 *
 */
public class RTEDwrapper {

	// constants
    private static final byte POST2_PARENT = 5;
    private static final byte POST2_LABEL = 6;
    private static final byte POST2_LLD = 8; // left-most leaf descendants
    
    // trees
    private InfoTree it1;
    private InfoTree it2;
    private String ts1;
    private String ts2;
    private int size1;
    private int size2;
    private LabelDictionary ld;
    
    public boolean hasSingleWord; //is added in order to count the number of sentences that has the problem of single words (they are synonym but are not aligned)
    
	public List<String> fragmentTrees = new ArrayList(); // Huma: to return fragment results
    private int[][]  alignment; /*  contains numeric information on the alignment:
									alignment[0][p], alignment[1][p] are aligned postions.
									INDELs have one 0.
									alignment[0][0] contains the length of the alignment. */
    
	/**
	 * Extract RTED mapping then fragments
	 * @param tree1
	 * @param tree2
	 * @return
	 */
    public List<String> extractFragmentsFromMapping(String tree1, String tree2){
		hasSingleWord = false; 
		
		//convert to RTED format ( ) to { }
		String tree1RTED = convertToRTEDFormat(tree1);
		String tree2RTED = convertToRTEDFormat(tree2);
		
		RTED objRTED = new RTED();
		double RTED = objRTED.computeDistance(tree1RTED, tree2RTED);
		LinkedList<int[]> editMapping = objRTED.computeMapping(tree1RTED, tree2RTED);
		
        intialize(tree1, tree2, editMapping);
        printAlignment();
        String out = fragmentTree(tree1, tree2, editMapping);
        
        fragmentTrees.add("TED: " + RTED + " " + out);      		
		return fragmentTrees;		
	}

	private void intialize(String tree1, String tree2,LinkedList<int[]> editMapping) {
		ld = new LabelDictionary();
		it1 = new InfoTree(LblTree.fromString(convertToRTEDFormat(tree1)), ld);
		it2 = new InfoTree(LblTree.fromString(convertToRTEDFormat(tree2)), ld);	
        size1 = it1.getSize();
        size2 = it2.getSize();
        ts1 = Tree.valueOf(tree1).toString();
        ts2 = Tree.valueOf(tree2).toString();
		fillAlignment(editMapping);		
	}
	
	/**
	 * This method is added to keep the leaves (words) in the sentences
	 * @param t
	 * @return
	 */
	private static String convertToRTEDFormat(String t) {
		t = t.replace(")", " )");
		t = t.replace(":", ";");
		String[] words = t.split(" ");
		String allTree = "";
		for(int i=0; i<words.length ;i++){
			if(!words[i].equals(")") && !words[i].startsWith("(")){
				words[i] = "(" + words[i] + ")";
			}
			allTree = allTree + words[i];
		}
		allTree = allTree.replace("(", "{");
		allTree = allTree.replace(")", "}");
		return allTree;
	}
	
	/**
	 * I have first calculated editMapping using RTED method
	 * Then convert that mapping to alignment[][] 
	 * in order to be compatible with my previous implementations
	 * @param editMapping
	 */
	private void fillAlignment(LinkedList<int[]> editMapping) {
		alignment = new int[2][];
        alignment[0] = new int[size1+1];
        alignment[1] = new int[size2+1];
        
        int n1, n2;
        String label1, label2, parent1, parent2;
        for (int[] nodeAlignment : editMapping) {
        	n1 = nodeAlignment[0];
        	n2 = nodeAlignment[1];
        	
        	if(n1 == 0 || n2 == 0) //it means that of the nodes is deleted or inserted
        		continue;
        	
        	
        	label1 = ld.read(it1.info[POST2_LABEL][n1-1]);
        	label2 = ld.read(it2.info[POST2_LABEL][n2-1]);
        	
        	parent1 = "";
        	parent2 = "";
        	if(it1.info[POST2_PARENT][n1-1] != -1)
        		parent1 = ld.read(it1.info[POST2_LABEL][it1.info[POST2_PARENT][n1-1]]);
        	if(it2.info[POST2_PARENT][n2-1] != -1)
        		parent2 = ld.read(it2.info[POST2_LABEL][it2.info[POST2_PARENT][n2-1]]);
        	
        	
        	// this pos tags are added, in order to consider only open class words
        	// so only function words are considered 
        	// and the other words are lemmatize
        	// so if a word is open class and it is not the same as the mapped word, we will not align it
        	List<String> openList = Arrays.asList("NN", "VB", "JJ", "RB");
        	
        	if(!label1.equals(label2) && (openList.contains(parent1) || openList.contains(parent2) )){
        		hasSingleWord = true;
        		continue;
        	}
        	
        	//if a node is not a leaf and the pos tags do not match, do not align them
        	if(!label1.equals(label2) && (getNumberOfChildren(it1, n1-1)!=0 || getNumberOfChildren(it2, n2-1)!=0 ))
        		continue;
        	
        	alignment[0][n1] = n2;
        	alignment[1][n2] = n1;
        }
	}

	/**
	 * extract the fragments given alignment
	 * @param tree1
	 * @param tree2
	 * @param editMapping
	 * @return
	 */
	private String fragmentTree(String tree1, String tree2, LinkedList<int[]> editMapping) {		
		String label = "";
		int[] fragment = new int[alignment[1].length-1];
    	int fCount = 0;    
    	int parent_i;
    	for(int i=0; i<alignment[1].length-1; i++){
    		
    		label = ld.read(it2.info[POST2_LABEL][i]);
    		parent_i= it2.info[POST2_PARENT][i];
    		
    		if(alignment[1][i+1] == 0 && it2.info[POST2_LLD][i] == i){
    			// Do nothing
    			
    		} else if (alignment[1][i+1] != 0){
    			int childFrag = childrenFragment(it2, i, fragment);
    			int parentFrag = parentFragment(it2, i, fragment);
    			if (parentFrag == -1){
    				fragment[i] = childFrag;
    				continue;
    			}
    			
    			int sibFrag = siblingFragment(it2, i, fragment);
    			int max = Math.max(childFrag, Math.max(parentFrag, sibFrag));
   			
    			if (max == 0){
        			fCount++;
        			fragment[i] = fCount; 
    			}else if(parentFrag != 0 && childFrag != 0 && parentFrag != childFrag){
    				fragment[i] = parentFrag;
    				// change its children to its parent's fragment
    				fragment = changeAllChildrenFragments(fragment, childFrag, parentFrag);
    			}else if(sibFrag == max && parentFrag == 0 && (childFrag == max || childFrag == 0) && alignment[1][parent_i+1] == 0){
    				//do nothing
    			}else if ((childFrag == max || childFrag == 0) &&
    					(parentFrag == max || parentFrag == 0) &&
    					(sibFrag == max || sibFrag == 0)){
	    				fragment[i] = max;
    			}else if ( childFrag == 0 && parentFrag != 0 && parentFrag != sibFrag)
    				fragment[i] = parentFrag;
    			
    			if( areSibilingsAligned(it2, i) && isParentAligned(it2, i) && fragment[i]!=0){
    				// set fragments of its parent and sibling to its fragment 	
    				int fragChange = fragment[it2.info[POST2_PARENT][i]];
    				fragment[it2.info[POST2_PARENT][i]] = fragment[i];
    				for(int j=0; j<it2.info[POST2_PARENT].length ; j++){			
    					if (it2.info[POST2_PARENT][j] == it2.info[POST2_PARENT][i]){
    						//if the parent of its aligned node are not the same
        					if (it1.info[POST2_PARENT][alignment[1][j+1]-1] != it1.info[POST2_PARENT][alignment[1][i+1]-1])
        						continue;
        					//if this node is not aligned, continue;
        					if(alignment[1][j+1] == 0)
        						continue;
    						fragment[j] = fragment[i];
    					}
    					
    				}
    				fragment = changeAllChildrenFragments(fragment, fragChange, fragment[i]);
    			}
    		}
    		
    	}
    	   	
    	//1) This is added to check if there is a leaf that is not in a fragment
    	fragment = new postProcessingFragments().checkAllTerminalsAreFragments(ld, it1, it2, ts1, ts2, fragment, alignment);
    	  	
    	//2) This is added to check the head of non-terminals with their children in each fragment
    	//fragment = postHead.checkParseTreeHeads(ld, it2 ,ts1, ts2, fragment); //care about Head in Bad Tree
    	fragment = new postProcessingFragments().checkParseTreeHeadsBasedOnReferenceTree(ld, it1, it2 , ts1, ts2, fragment, alignment); //care about Head in Good Tree
  		  	  
    	//2) This is added to check if there is a leaf that is only word. All words should be with their POS
    	fragment = new postProcessingFragments().checkAllTerminalsAreWithPOS(ld, it1, it2, ts1, ts2, fragment, alignment);
    	
    	//3) This is added to check whether there exists any fragment that does not contain any pre-terminals?
    	fragment = new postProcessingFragments().checkPreTerminalsInFragments(ld, it1, it2, ts1, ts2, fragment, alignment);
    	    	
    	String out = printTreeFragments(it2, fragment);
        return out;
	}
	


	private  int[] changeAllChildrenFragments(int[] fragment, int oldFrag, int newFrag) {
		for(int kk=0; kk< fragment.length ; kk++)
			if(fragment[kk] == oldFrag && oldFrag !=0 && oldFrag != newFrag && alignment[1][kk+1] != 0)
				fragment[kk] = newFrag;
		return fragment;
	}
	
    private boolean isParentAligned(InfoTree it22, int  node) {
    	if (alignment[1][it22.info[POST2_PARENT][node] + 1] != 0){
    		return true;
    	}
		return false;
	}

	private boolean areSibilingsAligned(InfoTree it22, int node) {
    	boolean out = true;
    	int count = 0;
    	for(int i=0; i<it2.info[POST2_PARENT].length ; i++){
    		if (it22.info[POST2_PARENT][i] == it22.info[POST2_PARENT][node]){
    			count++;
    			if (alignment[1][i+1] != 0)
    				out = out && true;
    			else
    				return false;
    		}		
    	}
    	if (count > 0)
    		return out;
		return false;
	}

	private int childrenFragment(InfoTree it22, int node, int[] fragment) {
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == node && fragment[i] != 0)
				return fragment[i];
		}	
		return 0;
	}
    
    private int parentFragment(InfoTree it22, int node, int[] fragment) {
    	if (it22.info[POST2_PARENT][node] == -1)
    		return -1;
    	else
    		return fragment[it22.info[POST2_PARENT][node]];
	}
    
    private int siblingFragment(InfoTree it22, int node, int[] fragment) {
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == it22.info[POST2_PARENT][node] && fragment[i] != 0)
				return fragment[i];
		}	
		return 0;
	}

	/**
	 * Print fragments
	 * @param it12
	 * @param it22
	 */
    private String printTreeFragments(InfoTree it22, int[] fragment) {
    	fragmentTrees.clear();
   	
		int noFrag = findMaxFragment(fragment);
		for(int frag=1; frag<noFrag+1; frag++){
			
			String[] treeArr = new String[fragment.length];
			int[] questionMarkChildren = new int[size1];
			
			for(int i=0; i<fragment.length; i++ ){
				if(fragment[i] == frag){
					//treeArr[i] = ld.read(it2.info[POST2_LABEL][i]) + "_" + (i+1);
					// 11102013: this line is added to set the node of the first tree(reference tree) as the node number, so we can compare fragments of different methods together. 
					int childrenNo = getNumberOfChildren(it2, i);
					if(childrenNo == 0 )
						treeArr[i] = ld.read(it2.info[POST2_LABEL][i]) + "__" + alignment[1][i+1];
					else
						treeArr[i] = ld.read(it2.info[POST2_LABEL][i]) + "_" + alignment[1][i+1];

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
							// to add ? for each child of ref that has not appeared		
							for(int t1=0; t1<it1.info[POST2_PARENT].length -1 ; t1++){
								if(it1.info[POST2_PARENT][t1] == alignment[1][i+1]-1 && (alignment[0][t1+1]==0 || treeArr[alignment[0][t1+1]-1] == null) && t1 < alignment[1][t2+1]-1){
									// if their parents are aligned but itself is not aligned
									if(questionMarkChildren[t1] == 1)
										continue;
									if (treeStr.startsWith("[?"))
										continue;
									//TODO: for simplicity I decided to not to add ? marks.
									treeStr = treeStr + "[" + ld.read(it1.info[POST2_LABEL][t1]) + "?]";
									questionMarkChildren[t1] = 1;
								}
							}
							
							if(!treeArr[t2].startsWith("["))
								treeArr[t2] = "[" + treeArr[t2] + "]";
							treeStr = treeStr + treeArr[t2];
							
							// to add ? for each child of ref that has not appeared	only for after children	
							for(int t1=0; t1<it1.info[POST2_PARENT].length -1 ; t1++){
								if(it22.info[POST2_PARENT][t2+1] != i){ //the last aligned child of t2
									if(it1.info[POST2_PARENT][t1] == alignment[1][i+1]-1 && (alignment[0][t1+1]==0 || treeArr[alignment[0][t1+1]-1] == null) && t1 > alignment[1][t2+1]-1){
										// if their parents are aligned but itself is not aligned
										if(questionMarkChildren[t1] == 1)
											continue;
										if (treeStr.startsWith("[?"))
											continue;
										//TODO: for simplicity I decided to not to add ? marks.
										treeStr = treeStr + "[" + ld.read(it1.info[POST2_LABEL][t1]) + "?]";
										questionMarkChildren[t1] = 1;
									}
								}
							}
						}
					}					
					treeStr = treeStr + "]";
					treeArr[i] = treeStr;
				}
			}
			for(int i=treeArr.length-1 ; i>=0 ; i--){
				if(treeArr[i] != null){
					fragmentTrees.add(treeArr[i]);
					break;
				}
			}
			
		}
		
		int sumLen = 0, count = 0, countQ = 0;
		noFrag = fragmentTrees.size();
		System.out.println();
		int noOneFragments = 0;
		for (String string : fragmentTrees) {
			System.out.println(string);
			count = countOccurrences(string, ']');
			countQ = countOccurrences(string, '?');
			sumLen = sumLen + (count - countQ);
			if((count - countQ) == 1)
				noOneFragments++;
		}
		
		String out = null;
        out = "#: " + noFrag + " avgLen: " + (double) sumLen/noFrag + "\t" + noOneFragments;
		return out;
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
    
	private String removedQuestionMark(String string) {
		Pattern MY_PATTERN = Pattern.compile("\\[(.*?)\\]");
		Matcher m = MY_PATTERN.matcher(string);
		while (m.find()) {
		    String s = m.group(1);
		    s = s ;
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

	private int getNumberOfChildren(InfoTree it22, int node) {
		int count = 0;
		for(int i=0; i<it22.info[POST2_PARENT].length ; i++){
			if (it22.info[POST2_PARENT][i] == node)
				count++;
		}	
		return count;
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

	private void printAlignment() {
    	String label1, label2;
    	for(int i=1; i<=alignment[0].length-1; i++){
    		if(alignment[0][i] == 0)
    			continue;
    		label1 = ld.read(it1.info[POST2_LABEL][i-1]);
    		label2 = ld.read(it2.info[POST2_LABEL][alignment[0][i]-1]);
    		
    		System.out.println( (i) + " (" + label1 + ") - " + (alignment[0][i]) + " (" + label2 + ")");
    	}	
	}
}

