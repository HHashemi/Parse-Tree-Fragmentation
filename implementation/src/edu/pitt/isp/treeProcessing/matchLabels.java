package edu.pitt.isp.treeProcessing;

import edu.stanford.nlp.trees.Tree;

public class matchLabels {

	/**
	 * POS (#36): http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
	 * All tags (#82): http://bulba.sdsu.edu/jeanette/thesis/PennTags.html
	 * 
	 * S = S, SBAR, SBARQ ,  SINV , SQ 
	 * JJ = JJ, JJR, JJS
	 * NN = NN, NNS, NNP, NNPS 
	 * PRP = PRP, PRP$
	 * RB = RB, RBR, RBS
	 * VB = VB, VBD,  VBG , VBN , VBP, VBZ
	 * WP = WP , WP$
	 */
	
	
	
    public boolean areLabelsMatched(String label1, String label2) {
		if(label1.contains(label2) || label2.contains(label1))
			return true;
		return false;
	}
    
    public String unifyTreeLabels(String treeStr){
    	treeStr = treeStr.replace("(SBAR ", "(S ");   	    	
    	treeStr = treeStr.replace("(SBARQ ", "(S ");
    	treeStr = treeStr.replace("(SINV ", "(S ");
    	treeStr = treeStr.replace("(SQ ", "(S ");
    	
    	treeStr = treeStr.replace("(JJR ", "(JJ "); 
    	treeStr = treeStr.replace("(JJS ", "(JJ ");
    	
    	treeStr = treeStr.replace("(NNS ", "(NN "); 
    	treeStr = treeStr.replace("(NNP ", "(NN "); 
    	treeStr = treeStr.replace("(NNPS ", "(NN "); 
    	
    	treeStr = treeStr.replace("(PRP$ ", "(PRP "); 
    	
    	treeStr = treeStr.replace("(RBR ", "(RB "); 
    	treeStr = treeStr.replace("(RBS ", "(RB "); 
    	
    	treeStr = treeStr.replace("(VBD ", "(VB ");
    	treeStr = treeStr.replace("(VBG ", "(VB ");
    	treeStr = treeStr.replace("(VBN ", "(VB ");
    	treeStr = treeStr.replace("(VBP ", "(VB ");
    	treeStr = treeStr.replace("(VBZ ", "(VB ");
    	
    	treeStr = treeStr.replace("(WP$ ", "(WP ");
    	
		return treeStr;	
    }
    
    public String unifyOneLabel(String treeStr){  // only call this method on addwordcategories class to find word pos 
    	treeStr = treeStr.replace("SBAR", "S");   	    	
    	treeStr = treeStr.replace("SBARQ", "S");
    	treeStr = treeStr.replace("SINV", "S");
    	treeStr = treeStr.replace("SQ", "S");
    	
    	treeStr = treeStr.replace("JJR", "JJ"); 
    	treeStr = treeStr.replace("JJS", "JJ");
    	
    	treeStr = treeStr.replace("NNS", "NN"); 
    	treeStr = treeStr.replace("NNP", "NN"); 
    	treeStr = treeStr.replace("NNPS", "NN"); 
    	
    	treeStr = treeStr.replace("PRP$", "PRP"); 
    	
    	treeStr = treeStr.replace("RBR", "RB"); 
    	treeStr = treeStr.replace("RBS", "RB"); 
    	
    	treeStr = treeStr.replace("VBD", "VB");
    	treeStr = treeStr.replace("VBG", "VB");
    	treeStr = treeStr.replace("VBN", "VB");
    	treeStr = treeStr.replace("VBP", "VB");
    	treeStr = treeStr.replace("VBZ", "VB");
    	
    	treeStr = treeStr.replace("WP$", "WP");

		return treeStr;	
    }
    
    public String unifyTreeLabelsTB(String treeStr){
    	treeStr = treeStr.replace("(SBAR", "(S");   	    	
    	treeStr = treeStr.replace("(SBARQ", "(S");
    	treeStr = treeStr.replace("(SINV", "(S");
    	treeStr = treeStr.replace("(SQ", "(S");
    	
    	treeStr = treeStr.replace("(JJR", "(JJ"); 
    	treeStr = treeStr.replace("(JJS", "(JJ");
    	
    	treeStr = treeStr.replace("(NNS", "(NN"); 
    	treeStr = treeStr.replace("(NNP", "(NN"); 
    	treeStr = treeStr.replace("(NNPS", "(NN"); 
    	
    	treeStr = treeStr.replace("(PRP$", "(PRP"); 
    	
    	treeStr = treeStr.replace("(RBR", "(RB"); 
    	treeStr = treeStr.replace("(RBS", "(RB"); 
    	
    	treeStr = treeStr.replace("(VBD", "(VB");
    	treeStr = treeStr.replace("(VBG", "(VB");
    	treeStr = treeStr.replace("(VBN", "(VB");
    	treeStr = treeStr.replace("(VBP", "(VB");
    	treeStr = treeStr.replace("(VBZ", "(VB");
    	
    	treeStr = treeStr.replace("(WP$", "(WP");
    	
		return treeStr;	
    }
    
}
