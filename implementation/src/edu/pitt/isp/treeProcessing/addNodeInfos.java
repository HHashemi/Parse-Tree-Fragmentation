package edu.pitt.isp.treeProcessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;

public class addNodeInfos {

	CollinsHeadFinder hf = new CollinsHeadFinder();
	addWordCategoriesToTree objWordCat = new addWordCategoriesToTree();
	matchLabels matchLabelsObj = new matchLabels();
	
	public List<String> addNodeNumbers(List<Tree> fragments){
		List<String> output = new ArrayList<String>();
		for(int i=0; i< fragments.size(); i++){
			Tree tree = fragments.get(i);
			List<Tree> postOrder = tree.postOrderNodeList();
			int postID = 0;
			for(Tree node: postOrder){
				postID++;
				String newNodeLabel;
				if(node.isLeaf()){
					newNodeLabel = node.value() + "__" + postID ;
				}else
					newNodeLabel = node.value() + "_" + postID ;
				
				node.setValue(newNodeLabel);
			}
			output.add(printFormatFragment(tree.toString()));
		}
		return output;
	}
	
	/**
	 * for each fragment node add its head and parent to the node
	 * @param fragments
	 * @return
	 */
	public List<String> addHeadAndParentsToTree(List<Tree> fragments, Tree totalTree) {
		objWordCat.loadWordCategories();
	
		HashMap<String, String> headMap = addNodeHeads(totalTree);
		HashMap<Integer, String> temp = new HashMap<Integer, String>();
		List<String> output = new ArrayList<String>();
		for(int i=0; i< fragments.size(); i++){
			Tree tree = fragments.get(i);
			List<Tree> postOrder = tree.postOrderNodeList();
			for(Tree node: postOrder){
				if(node.value().contains("?") || node.value().equals("-0-") || node.value().equals("NONE")) //TODO: it does not have any affect
					continue;
				String parent;
				if(node.parent(tree) == null)
					parent = "null";
				else
					parent = node.parent(tree).value().split("_")[0];
				String nodeNumber = node.value().split("_")[node.value().split("_").length-1];
				String head = node.headTerminal(hf).value().split("_")[0];
				String nodeValue = node.value().split("_")[0];
				//12102013: in order to not considering leaves of tree
				if(node.isLeaf() && node.value().contains("__"))
					continue;
				
				//unify the node label
				if(node.isLeaf() && node.value().contains("__")){
					nodeValue = objWordCat.getWordCategory(nodeValue);
				}else{
					nodeValue = matchLabelsObj.unifyOneLabel(nodeValue);
					//find the head based on the node number
					head = headMap.get(nodeNumber);				
				}

				head = objWordCat.getWordCategory(head);
				parent = matchLabelsObj.unifyOneLabel(parent);
				//TODO:if _ is used, the comparison only considers head, but if - is used, it also considers parents
				// I just added another nodeValue to the head, so it would be considered as a unique edge when extracting fragment edges in comparison measures
				String newNodeLabel = nodeValue + "_" + parent + "_" + head + "-" + nodeValue;
				temp.put(node.nodeNumber(tree), newNodeLabel);
			}
			for(Tree node: postOrder){
				if(temp.containsKey(node.nodeNumber(tree)))
					node.setValue(temp.get(node.nodeNumber(tree)));
			}
			temp.clear();
			output.add(printFormatFragment(tree.toString()));
		}
		return output;
	}

	private HashMap<String, String> addNodeHeads(Tree tree) {
		HashMap<String, String> temp = new HashMap<String, String>();
		List<Tree> postOrder = tree.postOrderNodeList();
		int postID = 1;
		for(Tree node: postOrder){
			String head = node.headTerminal(hf).value();
			temp.put(Integer.toString(postID), head);
			postID++;
		}
		return temp;
	}

	public static String printFormatFragment(String tree) {
		tree = tree.replace("(", "[");
		tree = tree.replace(")", "]");
		tree = tree.replace("]", " ]");
		String[] words = tree.split(" ");
		String treeCat = "";
		for(int i=0; i<words.length ;i++){
			if(!words[i].equals("]") && !words[i].startsWith("[") && !words[i].startsWith("][")){
				words[i] = "[" + words[i] + "]";
			}
			treeCat = treeCat + words[i];
		}
		return treeCat;		
	}
}
