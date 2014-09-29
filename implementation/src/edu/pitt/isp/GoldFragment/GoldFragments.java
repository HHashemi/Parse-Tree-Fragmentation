package edu.pitt.isp.GoldFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.pitt.isp.treeFragments.Ref_fragmentWithRef;
import edu.pitt.isp.treeProcessing.matchLabels;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;

public class GoldFragments {

	private String origSent;
	private Tree origTree;
	private List<Tree> fragments = new ArrayList<Tree>(); 
	CollinsHeadFinder hf = new CollinsHeadFinder();
	
	public GoldFragments(String origSent, String origTree){
		//convert to stanford tree format
		String treeRef = Tree.valueOf(origTree).toString();
		
		//unify labels
		matchLabels matchLabelsObj = new matchLabels();
		treeRef = matchLabelsObj.unifyTreeLabels(treeRef);
					
		//convert to stanford tree format
		treeRef = Tree.valueOf(treeRef).toString();
		
		this.origSent = origSent;
		this.origTree = Tree.valueOf(treeRef);
		this.fragments.add(addNodeNumberToLeaves());
		
	}
	
	public List<Tree> getOrigTreeList(){
		List<Tree> temp = new ArrayList<Tree>();
		temp.add(this.origTree);
		return temp;
	}
	
	private Tree addNodeNumberToLeaves() {
		Tree fragment = origTree;
		int j=0;
		String nodeValue="";
		List<Tree> postOrder = fragment.postOrderNodeList();
		int postID = 1;
		for(Tree node: postOrder){
			if(node.isLeaf()){
				j++;
				nodeValue = node.value() + "__" + j;
				node.setValue(nodeValue);
			} else { //add post-ordering number to nodes
				nodeValue = node.value() + "_" + postID;
				node.setValue(nodeValue);
			}
			postID++;
		}	
		return fragment;
	}

	
	/**
	 * main function
	 * @param errorType
	 * @param errorPosition
	 * @param errorWord
	 * @param errorPOS
	 * @param errorSent
	 * @return
	 */
	public List<Tree> makeGoldFragments(String errorType, String errorPosition, String errorWord, String errorPOS , String errorSent){
		if(errorType.startsWith("miss_") || errorType.endsWith("_del")){ //deletion error
			fragmentOriginalTreeMissingWord(errorPosition, errorWord); 
		} else if (errorType.endsWith("a_conf") || errorType.endsWith("_repl")){ //replacement error
			fragmentOriginalTreeReplacement(errorPosition, errorWord, errorPOS);
		} else { //insertion error
			fragmentOriginalTreeExtraWord(errorPosition, errorWord, errorPOS);
		}
		
		//post process fragments by removing fragments that do not have any leaf and separating fragments that do not have their head.
		//1) remove fragments that do not have leaf
		fragments = removeFragmentsWithoutLeaves(fragments);
		
		//2) separate fragments that do not have heads, it seems that there is no need for this function!!!
		//fragments = separateFragmentswithoutHead(fragments);
		
		return fragments;
	}


	/**
	 * Gold Fragments for Replaced word
	 * This method just replace the new word with the old one
	 * @param errorPosition
	 * @param errorWord
	 */
	private void fragmentOriginalTreeReplacement(String errorPosition, String errorWord, String errorPOS) {
		for( int i=0; i< fragments.size();i++){
			for(int j=0; j< fragments.get(i).getLeaves().size(); j++){
				Tree leaf = fragments.get(i).getLeaves().get(j);
				if(leaf.value().endsWith("__" + errorPosition)){
					// it finds the replaced token
					int nodeNo = leaf.nodeNumber(fragments.get(i));
					int nodeNoP = leaf.parent(fragments.get(i)).nodeNumber(fragments.get(i));
								
					Tree parent = leaf.parent(fragments.get(i));
					String parentPOS = parent.value().substring(0,parent.value().indexOf("_"));
					
					// check whether the POS tag of error word is the same as the previous (correct) word
					// if POS tags are not the same, split the fragment
					if(errorPOS.equals(parentPOS)){
						fragments.get(i).getNodeNumber(nodeNo).setValue(errorWord + "__" + errorPosition);				
						return;
						
					} else { // put the word and its POS tag as a new fragment
						fragments.get(i).getNodeNumber(nodeNo).setValue(errorWord + "__" + errorPosition);
						fragments.get(i).getNodeNumber(nodeNoP).setValue(errorPOS + parent.value().substring(parent.value().indexOf("_"), parent.value().length()));
	
						//if leaf does not have a grandparent, just replace the POS tag and the word
						if (leaf.parent(fragments.get(i)) == null || leaf.parent(fragments.get(i)).parent(fragments.get(i)) == null){						
							return;
						}
						Tree grandParent = leaf.parent(fragments.get(i)).parent(fragments.get(i));
						
						//if the replaced word (with different POS tag) is the head of the its grandparent, separate grandparent's children.
						if(fragments.get(i).size()>3 && grandParent.headTerminal(hf).equals(parent.headTerminal(hf))){
							//separate all children of the grandParent into different fragments
							int childrenNo = grandParent.getChildrenAsList().size();
							for(int k=0; k<childrenNo ; k++){
								Tree separatedSibiling = grandParent.getChild(k);
								fragments.add(separatedSibiling);
							}
							for(int k=0; k<childrenNo ; k++){
								grandParent.removeChild(0);
							}

						} else {
							int childrenNo = grandParent.getChildrenAsList().size();
							for(int k=0; k<childrenNo ; k++){
								Tree separatedSibiling = grandParent.getChild(k);
								if(grandParent.getChild(k).toString().contains(errorWord + "__" + errorPosition))
									fragments.add(separatedSibiling);
							}
							for(int k=0; k<childrenNo ; k++){
								if(grandParent.getChild(k).toString().contains(errorWord + "__" + errorPosition)){
									grandParent.removeChild(k);
									break;
								}
							}
						}
						
						return;
					}
				}
			}	
		}
	}


	/**
	 * Gold Fragments for Missing Word
	 * This method, empty the deleted word and fragment the tree if the deleted node contained the head 
	 * @param errorPosition
	 * @param errorWord 
	 */
	private void fragmentOriginalTreeMissingWord(String errorPosition, String errorWord) {
 		for( int i=0; i< fragments.size();i++){
			for(int j=0; j< fragments.get(i).getLeaves().size(); j++){
				Tree leaf = fragments.get(i).getLeaves().get(j);
				if(leaf.value().endsWith("__" + errorPosition)){
										
					// it finds the replaced token
					int nodeNo = leaf.nodeNumber(fragments.get(i));
					int nodeNoP = leaf.parent(fragments.get(i)).nodeNumber(fragments.get(i));
					
					Tree grandParent = leaf.parent(fragments.get(i)).parent(fragments.get(i));
					Tree parent = leaf.parent(fragments.get(i));
												
					//if the head of the parent of deleted word is the same as head of the deleted word, separate parent's children.
					if(fragments.get(i).size()>3 && grandParent.headTerminal(hf).equals(parent.headTerminal(hf))){
						fragments.get(i).getNodeNumber(nodeNo).setValue("-0-" + "__" + errorPosition);	
						fragments.get(i).getNodeNumber(nodeNoP).setValue("-NONE-");
						//separate all children of the grandParent into different fragments
						int childrenNo = grandParent.getChildrenAsList().size();
						for(int k=0; k<childrenNo ; k++){
							Tree separatedSibiling = grandParent.getChild(k);
							fragments.add(separatedSibiling);
						}
						for(int k=0; k<childrenNo ; k++){
							grandParent.removeChild(0);
						}
						//newFragmentForChildren(fragments, i, leaf.parent(fragments.get(i)));
					} else {
						fragments.get(i).getNodeNumber(nodeNo).setValue("-0-" + "__" + errorPosition);	
						fragments.get(i).getNodeNumber(nodeNoP).setValue("-NONE-");
					}
					
					return;
				}
			}	
		}
	}

	/**
	 * Gold Fragment for inserted word
	 * This method only puts the new inserted word to a new fragment
	 * It also separate the neighbors of inserted word into 3 fragments
	 * if the new word is inserted at the beginning of a group of nodes, the neighbors should be connected in a same fragment. 
	 * @param errorPosition
	 * @param errorWord
	 */
	private void fragmentOriginalTreeExtraWord(String errorPosition, String errorWord, String errorPOS) {
		int errorPos = Integer.parseInt(errorPosition);
		System.out.println("extra word before separate fragment " + errorPosition);
		//separate the main fragment where the new word is added into two fragments
		OUTERMOST: for( int i=0; i< fragments.size();i++){			
			for(int j=0; j< fragments.get(i).getLeaves().size(); j++){
				Tree leaf = fragments.get(i).getLeaves().get(j);
				if(leaf.value().contains("__")){
					int leafPos = Integer.parseInt(leaf.value().split("__")[1]);
					//check to find only the next immediate sibling to the inserted word, in order to find the best fragment.
					if(leafPos == (errorPos + 1)){
						//it finds the separated point					
						//if leaf does not have a grandparent, so we do not need to separate its siblings
						if (leaf.parent(fragments.get(i)) == null || leaf.parent(fragments.get(i)).parent(fragments.get(i)) == null){						
							break OUTERMOST;
						}
						
						Tree grandParent = leaf.parent(fragments.get(i)).parent(fragments.get(i));
	
						//if the new node is inserted at the beginning of a group of nodes, the right-hand side siblings should remain together as a separate fragment
						int childrenNo = grandParent.getChildrenAsList().size();
						
						System.out.println("grandParent: " + grandParent );
						if(j==0 || ( fragments.get(i).getLeaves().get(j-1).parent(fragments.get(i)) != null &&  
								     fragments.get(i).getLeaves().get(j-1).parent(fragments.get(i)).parent(fragments.get(i)) != null && 
								    !fragments.get(i).getLeaves().get(j-1).parent(fragments.get(i)).parent(fragments.get(i)).equals(grandParent) )){
							Tree temp = grandParent.deepCopy();
							fragments.add(temp);			
						}else{
							//if the new node is inserted between siblings, separate all children of the grandParent into different fragments
							for(int k=0; k<childrenNo ; k++){
								Tree separatedSibiling = grandParent.getChild(k);
								fragments.add(separatedSibiling);
							}
						}
						for(int k=0; k<childrenNo ; k++){
							grandParent.removeChild(0);
						}
						break OUTERMOST;
					}
				}
			}	
		}
		System.out.println("extra word separate main fragments done");
		//change the label (number) of words
		for( int i=0; i< fragments.size();i++){
			for(int j=0; j< fragments.get(i).getLeaves().size(); j++){
				Tree leaf = fragments.get(i).getLeaves().get(j);
				if (leaf.value().contains("__")) {
					int leafPos = Integer.parseInt(leaf.value().split("__")[1]);
					if(leafPos> errorPos){
						int nodeNo = leaf.nodeNumber(fragments.get(i));
						fragments.get(i).getNodeNumber(nodeNo).setValue(leaf.value().split("__")[0] + "__" + (leafPos+1));
					}
				}
			}	
		}
		System.out.println("extra word change labels done");
		//add the inserted fragment
		String newFragment = "(?" +" (" + errorPOS + "_0" + " ("+ errorWord + "__" + (errorPos+1)  +")))";
		Tree extraFragment = Tree.valueOf(newFragment); 
		extraFragment.getChild(0).setValue(errorPOS + "_0");
		extraFragment.getLeaves().get(0).setValue( errorWord + "__" +  (errorPos+1));
		fragments.add(extraFragment);

	}

	
	/**
	 * post processing 1 - remove fragments without leaves
	 * @param fragments
	 * @return
	 */
	private List<Tree> removeFragmentsWithoutLeaves(List<Tree> fragments) {	
		List<Tree> temp = new ArrayList<Tree>();
		for(int i=0; i<fragments.size(); i++){
			if(fragments.get(i).toString().contains("__")){
				temp.add(fragments.get(i));
			} else
				System.out.println(fragments.get(i));
		}
		return temp;
	}
	
	/**
	 * post processing 2 - separate fragments without heads
	 * @param fragments2
	 * @return
	 */
	private List<Tree> separateFragmentswithoutHead(List<Tree> fragments2) {
		List<Tree> temp = new ArrayList<Tree>();
		for(int i=0; i<fragments.size(); i++){
			if(isHeadAmongChildren(fragments.get(i))){
				temp.add(fragments.get(i));
				continue;
			}else{ //fragment its children
				newFragmentForChildren(temp, fragments.get(i));		
			}
		}
		return temp;
	}

	private boolean isHeadAmongChildren(Tree tree) {
		Tree head = tree.headTerminal(hf);
		for(int j=0; j< tree.getLeaves().size(); j++){
			Tree leafHead = tree.getLeaves().get(j).headTerminal(hf);
			if(leafHead.equals(head))
				return true;
		}
		return false;
	}
	
	private void newFragmentForChildren(List<Tree> temp, Tree tree) {
		
		
	}

}
