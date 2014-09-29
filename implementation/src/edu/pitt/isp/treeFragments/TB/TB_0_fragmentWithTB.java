package edu.pitt.isp.treeFragments.TB;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import TED.TED1_1.distance.InfoTree;
import TED.TED1_1.util.LabelDictionary;
import TED.TED1_1.util.LblTree;
import edu.pitt.isp.treeFragments.postProcessingFragments;
import edu.pitt.isp.treeProcessing.addWordCategoriesToTree;
import edu.pitt.isp.treeProcessing.matchLabels;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.CoordinationTransformer;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.Treebank;

public class TB_0_fragmentWithTB {
 
	 String treeFile = "../results/TB_sentences_all.ParseTree";
	 HashMap<String, Integer> TBrules=new HashMap<String, Integer>();
	 HashMap<String, Integer> TBsiblings=new HashMap<String, Integer>();
	 HashMap<Integer, Integer> mapID = new HashMap<Integer,Integer>(); //tree id to post-order id
	 HashMap<Integer, Integer> mapIDRev = new HashMap<Integer,Integer>(); //post-order id to tree id
	 public int[] checked ;
	 int[] fragment;
	 String[] treeLabels;
	 private int threshold_Sibling_freq = 5000;
	 private int threshold_rule_freq = 5000;
	 public List<String> fragmentTrees = new ArrayList();
	 private int[][]  alignment; /* contains numeric information on the alignment:
									alignment[0][p], alignment[1][p] are aligned postions.
									INDELs have one 0.
									alignment[0][0] contains the length of the alignment. */
	 private String treeStrOrig;
	 private String treeStrConverted;
	 private matchLabels matchLabelsObj = new matchLabels();
	 
	 public boolean hasSingleWord;
	 private addWordCategoriesToTree objWordCat;
	 
	 // added to fragment tree by its connected edges from TB. Because there was a bug in fragmenting by only checked nodes, we should also consider good edges.
	 private HashMap<String, Integer> connectedEdges=new HashMap<String, Integer>(); //integer is 0 or 1: 0 means the edge is not a real edge and just connect two nodes to a same parent
	 
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
		
	 public TB_0_fragmentWithTB(){
			objWordCat = new addWordCategoriesToTree();
			objWordCat.loadWordCategories();
	 }
		
	 public List<String> fragmentwithTB0(String treeSt){
			//keep words and then add word categories to leaves
		 	treeSt = objWordCat.addWordCategoriesToParseTree(treeSt);
			
			//replace [ ] to ( )
		 	treeSt = unconvertToDrawTable(treeSt);
			
			//convert to stanford tree format
		 	treeSt = Tree.valueOf(treeSt).toString();
			
			//unify labels
			matchLabels matchLabelsObj = new matchLabels();
			treeSt = matchLabelsObj.unifyTreeLabels(treeSt);
						
			//convert to stanford tree format
			treeSt = Tree.valueOf(treeSt).toString();
			
		 	this.treeStrOrig = treeSt;
		 	this.treeStrConverted = convertToFragmentFormat(treeStrOrig);
		 	
		 	//check the nodes that are in good rules and also check the connected edges
			checkTree(treeStrOrig);		
			printCheckedTree(treeStrOrig);
			
			intialize(treeSt);
			//String out = fragmentTree(treeStrOrig);
			
			//make fragments by connected edges
			String out = fragmentTreeByConnectedEdges(treeStrOrig);
			
	        fragmentTrees.add( out);
	        return fragmentTrees;

	 }
	 

	private void intialize(String tree) {	
		//convert to RTED format ( ) to { }
		String treeRTED = convertToRTEDFormat(tree);
		
		LblTree lt1 = LblTree.fromString(treeRTED.toString());
		ld = new LabelDictionary();
		it1 = new InfoTree(lt1, ld);
		it2 = new InfoTree(lt1, ld);	
	    size1 = it1.getSize();
	    size2 = it2.getSize();
	    ts1 = Tree.valueOf(tree).toString();
	    ts2 = Tree.valueOf(tree).toString();
	    fillAlignment();
	}	 
	 
	public void setThreshold(int teta){
		threshold_Sibling_freq = teta;
		threshold_rule_freq = teta;
	}
	 
	
	/**
	 * I have first found the check array
	 * Then convert that mapping to alignment[][] 
	 * in order to be compatible with my previous implementations
	 * @param 
	 */
	private void fillAlignment() {
		alignment = new int[2][];
        alignment[0] = checked; //this is fake
        alignment[1] = checked;
	}
	
	public void printCheckedTree(String treeStr) {
		Tree tree = Tree.valueOf(treeStr);
		List<Tree> postOrder = tree.postOrderNodeList();
		int postID = 1;
		for(Tree node: postOrder){
			mapID.put(node.nodeNumber(tree), postID);
			mapIDRev.put(postID, node.nodeNumber(tree));
			postID++;
		}
		
		String label;
		Tree node;
		for(int i=1; i<checked.length; i++){
			node = tree.getNodeNumber(mapIDRev.get(i));
			label = node.value();
			System.out.println(i + " " + label + " " + checked[i]);
		}
	
	}/**
	 * extract the fragments given alignment
	 * @param tree1
	 * @return
	 */
	public String fragmentTree(String tree1) {		
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
    	
    	//2) This is added to check if there is a leaf that is only word. All words should be with their POS
    	fragment = new postProcessingFragments().checkAllTerminalsAreWithPOS(ld, it1, it2, ts1, ts2, fragment, alignment);
    	
    	//2) This is added to check the head of non-terminals with their children in each fragment
    	//fragment = postHead.checkParseTreeHeads(ld, it2 ,ts1, ts2, fragment); //care about Head in Bad Tree
    	//fragment = new postProcessingFragments().checkParseTreeHeadsBasedOnReferenceTree(ld, it1, it2 , ts1, ts2, fragment, alignment); //care about Head in Good Tree
  		  	    	
    	//3) This is added to check whether there exists any fragment that does not contain any pre-terminals?
    	fragment = new postProcessingFragments().checkPreTerminalsInFragments(ld, it1, it2, ts1, ts2, fragment, alignment);
    	    	
    	String out = printTreeFragments(it2, fragment);
        return out;
	}
	

	/**
	 * find the fragments of a tree using the edges that are labeled as connected
	 * @param treeStrOrig
	 * @return
	 */
	private String fragmentTreeByConnectedEdges(String tree) {
		fragmentTrees.clear();
		List<List<String>> fragmentsList = new ArrayList<List<String>>();
		for(String edge : connectedEdges.keySet()){
			String father = edge.split("_")[0];
			String child = edge.split("_")[1];
			if(fragmentsList.size() == 0){
				fragmentsList.add(Arrays.asList(father, child));
			}else{
				List<String> list1 = Arrays.asList(father, child);
				boolean flag = true;
				while(flag){
					flag=false;
					for (int j=0; j< fragmentsList.size(); j++){ //check with all the fragments to see frag has overlap with which one.								
						List<String> list2 = fragmentsList.get(j);
						if(isTwoNodeListOverlapped(list1, list2)){	//list1 each time is updated because of assingment by reference!						
							list1 = combineTwoFragments(tree, list1, list2);
							fragmentsList.remove(j);
							flag=true;
						} 
					} 	
				}
				fragmentsList.add(list1);
			}			
		}
		
		System.out.println("fragment TB: start drawing fragments");
		for (int i=0; i< fragmentsList.size(); i++){
			System.out.println(fragmentsList.get(i));			
			//1) This is added to check if there is a leaf that is not in a fragment
			if(checkFragmentHasTerminal(tree, fragmentsList.get(i)))
				continue;
						
			String frag = drawFragment(tree, fragmentsList.get(i));
			
			System.out.println(frag);
			fragmentTrees.add(frag);
		 }
		System.out.println("fragment TB: finish");
		int sumLen = 0, count = 0, countQ = 0;
		int noFrag = fragmentTrees.size();
		System.out.println();
		int noOneFragments = 0;
		for (String string : fragmentTrees) {
			System.out.println(convertToDrawTable(string));
			count = countOccurrences(string, ')');
			countQ = countOccurrences(string, '?');
			sumLen = sumLen + (count - countQ);
			if((count - countQ) == 1)
				noOneFragments++;
		}
		
		String out = null;
        out = "#: " + noFrag + " avgLen: " + (double) sumLen/noFrag + "\t" + noOneFragments;
		return out;
	}

	private boolean checkFragmentHasTerminal(String treeSt, List<String> listNodes) {

		Tree tree = Tree.valueOf(treeSt);
		for(String node : listNodes){
			if (node.contains("?")) continue;
			int nodeID = Integer.parseInt(node);
			if( tree.getNodeNumber(mapIDRev.get(nodeID)).isLeaf()){ 
				return false;
			} 
		}
		return true;
	}

	private String drawFragment(String treeSt, List<String> listNodes) {
		listNodes = addQuestionMarkParent(treeSt, listNodes);
		Tree tree = Tree.valueOf(treeSt);
		List<Tree> postOrder = tree.postOrderNodeList();
		String treeStr = "";
		String[] treeArr = new String[tree.size()+1];
		for(Tree node: postOrder){			
			String nodeNumberPost = Integer.toString(mapID.get(node.nodeNumber(tree)));
			if(listNodes.contains(nodeNumberPost)){
				Tree[] children = node.children();
				//if node is a leaf, add __ for its number and alos do not add () for it
				if(children.length == 0){
					treeStr = " " + node.value() + "__" + nodeNumberPost + " ";
				} else {
					int countChildrenInList=0;
					treeStr = " (" + node.value() + "_" + nodeNumberPost;
					for(Tree child: children){
						if (listNodes.contains(Integer.toString(mapID.get(child.nodeNumber(tree))))){
							treeStr = treeStr + treeArr[mapID.get(child.nodeNumber(tree))];
							countChildrenInList++;
						}
					}
					treeStr = treeStr + ")";
				}
				treeArr[mapID.get(node.nodeNumber(tree))] = treeStr;
			} else if(listNodes.contains("?" + nodeNumberPost)){
				Tree[] children = node.children();
				int countChildrenInList=0;
				treeStr = " (" + "?" + "_" + nodeNumberPost;
				for(Tree child: children){
					if (listNodes.contains(Integer.toString(mapID.get(child.nodeNumber(tree))))){
						treeStr = treeStr + treeArr[mapID.get(child.nodeNumber(tree))];
						countChildrenInList++;
					}
				}
				treeStr = treeStr + ")";
				treeArr[mapID.get(node.nodeNumber(tree))] = treeStr;
			}
		}
		int upperNode = findMax(listNodes);
		String fragment = treeArr[upperNode].trim();
		return fragment;
	}

	private List<String> addQuestionMarkParent(String treeSt, List<String> listNodes) {
		Tree tree = Tree.valueOf(treeSt);
		List<String> temp = new ArrayList<String>();
		for(String node : listNodes){
			temp.add(node);
			if(node.contains("?")){ 
				return listNodes;
			} else{
				int nodeID = Integer.parseInt(node);
				if(tree.getNodeNumber(mapIDRev.get(nodeID)).value().equals("ROOT"))
					continue;
				int parent = mapID.get(tree.getNodeNumber(mapIDRev.get(nodeID)).parent(tree).nodeNumber(tree));
				if(listNodes.contains(Integer.toString(parent)))
					continue;
				else{
					temp.add( "?" + Integer.toString(parent));
				}
			}
		}
		return temp;
	}

	private List<String> combineTwoFragments(String tree, List<String> list1, List<String> list2) {
		List<String> listNodes = new ArrayList<String>();
		listNodes.addAll(list1);
		for(int i=0; i<list2.size(); i++){
			if(!list1.contains(list2.get(i)))
				listNodes.add(list2.get(i));
		}
		return listNodes;
	}

	 /**
	  * find minimum of the nodes to find the combined fragment
	  * @param listNodes
	  * @return
	  */
	private int findMax(List<String> listNodes) {
		int max = Integer.parseInt(listNodes.get(0).replace("?", ""));
		for(int i=1; i<listNodes.size(); i++){
			if(Integer.parseInt(listNodes.get(i).replace("?", "")) >= max)
				max = Integer.parseInt(listNodes.get(i).replace("?", ""));
		}
		return max;
	}
	
	/**
	  * check overlap of two node lists
	  * @param list1
	  * @param list2
	  * @return
	  */
	private boolean isTwoNodeListOverlapped(List<String> list1, List<String> list2) {
		for(int i=0; i<list1.size(); i++){
			if(list2.contains(list1.get(i)))
				return true;
		}
		return false;
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
					treeArr[i] = ld.read(it2.info[POST2_LABEL][i]) + "_" + (i+1);
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

	
	private  String findMaxImmediateSibling(Tree tree, Tree node) {
		Tree parent = node.parent(tree);
		Tree[] children = parent.children();
		String sibling = null, maxSibling = "";
		int node_id = 0;
		int max = 0;
		String value = node.value();
		
		for(int i=0;i<children.length ; i++){
			if(children[i].nodeNumber(tree) == node.nodeNumber(tree)){
				node_id=i;
				break;
			}
		}
		
		for(int i=0;i<=children.length ; i++)
			for(int j=i+2; j<= children.length; j++) { 
				if(node_id >= i && node_id <= j){
					sibling = getSibling(children, i, j);
					if(TBsiblings.containsKey(sibling) && TBsiblings.get(sibling) > max && sibling.contains(value)){
						max = TBsiblings.get(sibling);
						maxSibling = sibling;
					}
				}
			}
		return maxSibling;
	}

	private  void markMaxSiblingsChecked(Tree tree, Tree node, String maxSibling) {
		Tree parent = node.parent(tree);
		Tree[] children = parent.children();
		String sibling;
		int child_id;
		int node_id = 0;
		String[] maxSiblings = maxSibling.split(" ");
		
		for(int i=0;i<children.length ; i++){
			if(children[i].nodeNumber(tree) == node.nodeNumber(tree)){
				node_id=i;
				break;
			}
		}
		
		for(int i=0;i<=children.length ; i++)
			for(int j=i+2; j<= children.length; j++){
				if(node_id >= i && node_id <= j){
					sibling = getSibling(children, i, j);
					if(sibling.equals(maxSibling)){
						for(int k=i; k<j; k++){
							child_id = children[k].nodeNumber(tree);
							checked[mapID.get(child_id)] = 1;
						}
					}
				}
			}
	}

	private  void markSiblingsChecked(Tree tree, Tree node) {
		Tree parent = node.parent(tree);
		Tree[] children = parent.children();
		String siblings = "";
		int child_id;
		for(Tree child: children){
			if(child.isLeaf())
				continue;
			child_id = child.nodeNumber(tree);
			checked[mapID.get(child_id)] = 1;
		}	
	}


	private  String findAllSibling(Tree tree, Tree node) {
		Tree parent = node.parent(tree);
		Tree[] children = parent.children();
		String siblings = "";
		for(Tree child: children){
			if(child.isLeaf())
				continue;
			siblings = siblings + " " + child.value();
		}
		return siblings.trim();
	}


	/**
	 * 
	 * @throws IOException
	 */
	public  void findGrammarRulesFreq() throws IOException {
		Treebank tb = new MemoryTreebank();
		CoordinationTransformer transformer = new CoordinationTransformer();
		TreeReader tr = new PennTreeReader(new BufferedReader(new InputStreamReader(new FileInputStream(treeFile))), new LabeledScoredTreeFactory());
		
	    for (Tree t ; (t = tr.readTree()) != null; ) {	
	    	String unifiedTree = matchLabelsObj.unifyTreeLabels(t.toString());
	  	    Tree tree = transformer.transformTree(t);
	  	    tree = Tree.valueOf(unifiedTree);
	  	    findGrammarFreqForOneTree(tree);
	    }
	    
	}
	
	/**
	 * 
	 * @param tree
	 */
	private  void findGrammarFreqForOneTree(Tree tree) {
		Tree node;
		int[] visited = new int[tree.size()+1];
		String rule="";
  	    for (int i = 1; i <= tree.size(); i++) {
			node = tree.getNodeNumber(i);
			if(node.isLeaf())
				continue;
			else if (visited[i] == 1)
				continue;
			Tree[] children = node.children();
			rule = node.value() + " =";
			int count = 0;
			for(Tree child: children){
				if(child.isLeaf())
					continue;
				rule = rule + " " + child.value();
				count++;
			}
			if(count == 0)
				continue;
			
			findSiblingsForOneRule(children);
			
			if(!TBrules.containsKey(rule))
				TBrules.put(rule, 1);
			else
				TBrules.put(rule, TBrules.get(rule) + 1 );
			
			visited[i] = 1;
  	    }	
	}

	/**
	 * 
	 * @param children
	 */
	private  void findSiblingsForOneRule(Tree[] children) {
		if(children.length == 1)
			return;
		String sibling = "";
		int count = 0;
		for(int i=0;i<=children.length ; i++)
			for(int j=i+2; j<= children.length; j++){
				count++;
				sibling = getSibling(children, i, j);
				if(!TBsiblings.containsKey(sibling))
					TBsiblings.put(sibling, 1);
				else
					TBsiblings.put(sibling, TBsiblings.get(sibling) + 1 );
			}
	}

	private  String getSibling(Tree[] children, int i, int j) {
		String sibling = children[i].value();
		for(int k=i+1; k<j ; k++){
			sibling = sibling + " " + children[k].value();
		}
		return sibling;
	}


	/**
	 * 
	 * @throws IOException
	 */
	private  void writeTBrules() throws IOException {
		Writer out = new BufferedWriter(new FileWriter(new File("../results/TBGrammarRules")));
		for(String rule : TBrules.keySet()){
			out.write(rule + "\t" + TBrules.get(rule) + "\n");
		}
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File("../results/TBGrammarSiblings")));
		for(String rule : TBsiblings.keySet()){
			out.write(rule + "\t" + TBsiblings.get(rule) + "\n");
		}
		out.close();	
	}
	
	private String convertToFragmentFormat(String t) {
		t = t.replace(")", " )");
		t = t.replace(":", ".");
		String[] words = t.split(" ");
		String onlyTree = "";
		for(int i=0; i<words.length ;i++){
			if(!words[i].equals(")") && !words[i].startsWith("(")){
				continue;
			}
			words[i] = words[i].replace("(", "{");
			words[i] = words[i].replace(")", "}");
			onlyTree = onlyTree + words[i];
		}
		return onlyTree;
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
	
	
	private static String unconvertToDrawTable(String tree) {
		tree = tree.replace("[", "(");
		tree = tree.replace("]", ")");
		return tree;
	}
	
	private static String convertToDrawTable(String tree) {
		tree = tree.replace("(", "[");
		tree = tree.replace(")", "]");
		return tree;
	}
}
