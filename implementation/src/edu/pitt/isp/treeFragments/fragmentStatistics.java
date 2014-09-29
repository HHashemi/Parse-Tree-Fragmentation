package edu.pitt.isp.treeFragments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import TED.TED1_1.distance.InfoTree;
import TED.TED1_1.util.LabelDictionary;
import TED.TED1_1.util.LblTree;
import edu.pitt.isp.DB.Mysql;
import edu.pitt.isp.treeFragments.TB.TB_0_fragmentWithTB;
import edu.pitt.isp.treeFragments.TB.TB_1_fragmentWithTBDepth;
import edu.pitt.isp.treeProcessing.matchLabels;
import edu.stanford.nlp.trees.Tree;

/**
 * write some statistics over fragments
 * @author hashemi
 *
 */
public class fragmentStatistics {
	static String outFile = "../results/fragment_stats_TB"; // "../results/fragment_stats";
	static String outFile2 = "../results/fragment_stats_compareRefTB"; // "../results/fragment_stats";

	static String method = "RefTB"; //TB1Depth or Ref or TB  or RefTB or RefTBerror
	static TB_0_fragmentWithTB fragObj, fragObj2;	
	static TB_1_fragmentWithTBDepth TB1D3Obj, TB1D3Obj2;
	static Ref_fragmentWithRef RefObj;
	static List<String> goodRef, badRef, goodTB, badTB;  // list of fragments
	static double avgTEDGood=0, avgFragNoGood=0, avgFragLenGood=0, avgOneFragGood=0, avgTEDBad=0, avgFragNoBad=0, avgFragLenBad=0, avgOneFragBad=0, count=0; 
	static double avgFragNoGoodTB=0, avgFragLenGoodTB=0, avgOneFragGoodTB=0, avgFragNoBadTB=0, avgFragLenBadTB=0, avgOneFragBadTB=0, countTB=0; 

	public static int countNotConnectedFragment = 0;	
	private static matchLabels matchLabelsObj = new matchLabels();
	static List<String> uniqueSentencePairs = new ArrayList<String>();
	private static final byte POST2_PARENT = 5;
	private static final byte POST2_LABEL = 6;
	
	public static void main(String args[]) throws IOException{
		if(method.contains("TB") && !method.contains("TB1")){
			fragObj = new TB_0_fragmentWithTB();
			fragObj.findGrammarRulesFreq();	
			
			fragObj2 = new TB_0_fragmentWithTB(); 
			fragObj2.findGrammarRulesFreq();	
		} else if(method.contains("TB1Depth")){
			TB1D3Obj = new TB_1_fragmentWithTBDepth();
			TB1D3Obj.loadTBFragments();
			
			TB1D3Obj2 = new TB_1_fragmentWithTBDepth();
			TB1D3Obj2.loadTBFragments();
		}
		if(method.contains("Ref")){
			RefObj = new Ref_fragmentWithRef();
		}
		extractGoodBadSentences();
		
	}

	/**
	 * to collect some statistics for fragmentation
	 * @throws IOException 
	 */
	private static void extractGoodBadSentences() throws IOException {
		Writer out = new BufferedWriter(new FileWriter(new File(outFile)));	
		Writer out2 = new BufferedWriter(new FileWriter(new File(outFile2)));	

		try {
			Connection con = Mysql.getConn("MTdata");	
		    Statement sta = con.createStatement(); 
		    
		    HashMap<String, Integer> MT=new HashMap<String, Integer>();
		    
		    String MT_id, refSent1 = null, refSent2 = null, mtSent = null, fragmentOut = null, refTree1 = null, refTree2 = null, mtTree = null, ref_id, mtFluency = null;
		    String query = "SELECT MT_id FROM MT_Ref group by MT_id";
		    ResultSet rec = sta.executeQuery(query);
		    while(rec.next()){
		    	MT_id = rec.getString("MT_id");
		    	
		    	if(MT.containsKey(MT_id)){
		    		continue;
		    	}
		    	MT.put(MT_id, 1);
		    	
		    	query = "SELECT * FROM MT_Ref where MT_id='" + MT_id + "'";	    	
		    	Statement sta1 = con.createStatement();
		    	ResultSet rec1 = sta1.executeQuery(query);
		    	int count = 0;
		    	while(rec1.next()){
		    		ref_id = rec1.getString("ref_id");
	    			if(isTitle(con, "Ref", ref_id))  // if the reference sentence is Title, skip it
	    				continue;
	    			
		    		if (count==0){
		    			refSent1 = getSentFromDB(con, "Ref", ref_id);		    			
		    			refTree1 = getParseTreeFromDB(con, "Ref", ref_id);
		    			if(refTree1.length() < 2)
		    				continue;
		    			count++;
		    		} else if(count==1){
		    			refSent2 = getSentFromDB(con, "Ref", ref_id);
		    			refTree2 = getParseTreeFromDB(con, "Ref", ref_id);	    			
		    			mtSent = getSentFromDB(con, "MToutput", MT_id);
		    			mtTree = getParseTreeFromDB(con, "MToutput", MT_id);
		    			mtFluency = getSentenceFluency(con, "MToutput", MT_id);
		    			if(mtTree.length() < 2 || isNotValidSentece(mtSent, mtTree) || isNotValidSentece(refSent1, refTree1) || isNotValidSentece(refSent2, refTree2)){ //  if the size of leaves of tree are not equal to size of words in the sentence, then remove the sentence
		    				continue;
		    			}
		    			count++;
		    			break;
		    		}
		    	}
		    	sta1.close();
		    	if(count>1){
		    		refTree1 = matchLabelsObj.unifyTreeLabels(refTree1);
		    		refTree2 = matchLabelsObj.unifyTreeLabels(refTree2);
		    		mtTree = matchLabelsObj.unifyTreeLabels(mtTree);
		    		if(!uniqueSentencePairs.contains(mtTree + "\t" + refTree1 + "\t" + refTree2))
		    			uniqueSentencePairs.add(mtTree + "\t" + refTree1 + "\t" + refTree2);
		    		else
		    			continue;
		    		
		    		if (method == "Ref" || method == "TB" || method == "TB1Depth"){  // in order to write statistics about fragmentation for Ref method or TB method
			    		if(method == "Ref")
			    			fragmentOut = fragmentRef(refTree1, refTree2, mtTree);
			    		
			    		else if (method == "TB")
			    			fragmentOut = fragmentTB0(refTree1, mtTree);
			    		
			    		else if (method == "TB1Depth")
			    			fragmentOut = fragmentTB1_Depth(refTree1, mtTree);
			    				    			
			    		fragmentOut = fragmentOut.replace("good* TED: ", "\t");
			    		fragmentOut = fragmentOut.replace(" #: ", "\t");
			    		fragmentOut = fragmentOut.replace(" avgLen: ", "\t");
			    		fragmentOut = fragmentOut.replace(" ------- bad*  TED: ", "\t");
			    		fragmentOut = fragmentOut.replace("good*", "0");
			    		fragmentOut = fragmentOut.replace("------- bad* ", "\t0");
			    		out.write(refSent1 + "\t" + refSent2 + "\t" +  mtSent + "\t" + fragmentOut + "\t" + mtFluency + "\n");
			    		out.flush();
			    		updateStatistics(fragmentOut);
		    			
		    		} else if (method == "RefTB"){ // find fragments for both methods for each sentence to compare the results of fragmentation
		    			fragmentOut = fragmentRef(refTree1, refTree2, mtTree);		    			
		    			String goodRefFrags, badRefFrags;
		    			goodRefFrags = readFragmentList(goodRef);
		    			badRefFrags = readFragmentList(badRef);
		    				    		
			    		fragmentOut = fragmentOut.replace("good* TED: ", "\t");
			    		fragmentOut = fragmentOut.replace(" #: ", "\t");
			    		fragmentOut = fragmentOut.replace(" avgLen: ", "\t");
			    		fragmentOut = fragmentOut.replace(" ------- bad*  TED: ", "\t");
			    		fragmentOut = fragmentOut.replace("good*", "0");
			    		fragmentOut = fragmentOut.replace("------- bad* ", "\t0");
			    		updateStatistics(fragmentOut);
			    		
		    			out.write("Ref: " + refSent1 + "\n\t" + convertToDrawTable(refTree1) +
		    					"\nRef2(good): " + 	refSent2 + "\n\t" + convertToDrawTable(refTree2) + "\nRef2(Fragments):\n" +  goodRefFrags + 
		    					"\nMT:" + mtSent + "\n\t" + convertToDrawTable(mtTree) + "\nMT(Fragments):\n" + badRefFrags + 
		    					"\n" + fragmentOut + "\tFluency:" + mtFluency + "\n*****\n");
		    			out.flush();
		    			
		    			//Fragment using TB
		    			fragmentOut = fragmentTB0(refTree2, mtTree);
		    			String goodTBFrags, badTBFrags;
		    			goodTBFrags = readFragmentList(goodTB);
		    			badTBFrags = readFragmentList(badTB);
		    			
			    		fragmentOut = fragmentOut.replace("good* TED: ", "\t");
			    		fragmentOut = fragmentOut.replace(" #: ", "\t");
			    		fragmentOut = fragmentOut.replace(" avgLen: ", "\t");
			    		fragmentOut = fragmentOut.replace(" ------- bad*  TED: ", "\t");
			    		fragmentOut = fragmentOut.replace("good*", "0");
			    		fragmentOut = fragmentOut.replace("------- bad* ", "\t0");
			    		updateStatisticsTB(fragmentOut);
			    		
		    			out.write(
		    					"Ref2(good): " + 	refSent2 + "\n\t" + convertToDrawTable(refTree2) + "\nRef2(Fragments):\n" +  goodTBFrags + 
		    					"\nMT:" + mtSent + "\n\t" + convertToDrawTable(mtTree) + "\nMT(Fragments):\n" + badTBFrags + 
		    					"\n" + fragmentOut + "\tFluency:" + mtFluency + "\n\n=============================\n");
		    			out.flush();
		    			
		    			//compare TB fragments with Ref fragments	
		    			List<Double> precisionRecallGood = findSharedEdges(goodRef, goodTB);
		    			List<Double> precisionRecallBad = findSharedEdges(badRef, badTB);
		    			out2.write("Ref-Ref fragments: \t " + precisionRecallGood.get(0) + "\t" + precisionRecallGood.get(1) + "\tRef-MT fragments:\t" + precisionRecallBad.get(0) + "\t" + precisionRecallBad.get(1) + "\n");
		    			out2.flush();
		    			
		    		} else if (method == "RefTBerror"){ //only find sentences with special size of fragments for both methods 
		    			String fragmentOut1 = fragmentRef(refTree1, refTree2, mtTree);		   			
		    			String good1, bad1;
		    			good1 = readFragmentList(goodRef);
		    			bad1 = readFragmentList(badRef);
		    			String good1_no = goodRef.get(goodRef.size()-1).substring(goodRef.get(goodRef.size()-1).indexOf("#: ") + 3, goodRef.get(goodRef.size()-1).indexOf("avgLen:") - 1);
		    			String bad1_no = badRef.get(badRef.size()-1).substring(badRef.get(badRef.size()-1).indexOf("#: ") + 3, badRef.get(badRef.size()-1).indexOf("avgLen:") - 1);
	  		    			
		    			String good2, bad2;
		    			String fragmentOut2 = fragmentTB0(refTree2, mtTree);
		    			good2 = readFragmentList(goodTB);
		    			bad2 = readFragmentList(badTB);
		    			String good2_no = goodTB.get(goodTB.size()-1).substring(goodTB.get(goodTB.size()-1).indexOf("#: ") + 3, goodTB.get(goodTB.size()-1).indexOf("avgLen:") - 1);
		    			String bad2_no = badTB.get(badTB.size()-1).substring(badTB.get(badTB.size()-1).indexOf("#: ") + 3, badTB.get(badTB.size()-1).indexOf("avgLen:") - 1);		    			
		    			
		    			out.write("Ref: " + refSent1 + "\n\t" + convertToDrawTable(refTree1) +
		    					"\nRef2(good): " + 	refSent2 + "\n\t" + convertToDrawTable(refTree2) + "\nRef2(Fragments):\n" +  good1 + 
		    					"\nMT:" + mtSent + "\n\t" + convertToDrawTable(mtTree) + "\nMT(Fragments):\n" + bad1 + 
		    					"\n" + fragmentOut1 + "\tFluency:" + mtFluency + "\n*****\n");
		    			out.flush();
		    			
		    		}	    		
		    	}
		    }
		    
		    if (method == "Ref" || method == "TB" || method == "TB1Depth"){
		    	printFinalStatistics();
		    } else if (method == "RefTB"){
		    	printFinalStatisticsRefTB();
		    }
		    
		    System.out.println(" \n # Not connected Fragments: \n" + countNotConnectedFragment);
	 	    sta.close();
	 	    con.close();  
		} catch (Exception e) {
			System.err.println("Exception: "+e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
		}
		out.close();
		out2.close();
		return;	
		
	}
	
	public static List<Double> findSharedEdges(List<String> fragmentsRef,
			List<String> fragmentsTB) {
		fragmentsRef.remove(fragmentsRef.size()-1);
		HashMap<String, String> fragmentEdgesRef = extractAllEdgesOfFragments(fragmentsRef);
		fragmentsTB.remove(fragmentsTB.size()-1);
		HashMap<String, String> fragmentEdgesTB = extractAllEdgesOfFragments(fragmentsTB);
		
		int sharedEdges = 0;
		for(String edge : fragmentEdgesRef.keySet()){
			if(fragmentEdgesTB.containsKey(edge))
				sharedEdges++;
		}	
		List<Double> PrecisionRecall = new ArrayList<Double>();
		double precision = (double) sharedEdges/fragmentEdgesRef.size();
		double recall = (double) sharedEdges/fragmentEdgesTB.size();
		PrecisionRecall.add(precision);
		PrecisionRecall.add(recall);
		return PrecisionRecall;
	}

	/**
	 * The edges format is : (paretntID_childID , parentTag_childTag) 
	 * @param fragments
	 * @return
	 */
	public static HashMap<String, String> extractAllEdgesOfFragments(
			List<String> fragments) {
		HashMap<String, String> edges = new HashMap<String, String>();

		for(int i=0; i<fragments.size() ; i++){
			String frag = fragments.get(i);
			int count = countOccurrences(frag, ']');
			int countQ = countOccurrences(frag, '?');
			if (count - countQ > 1){ // the fragment has more than one node
				frag = unconvertToDrawTable(frag);
				
				String tt = convertToAlignmentFormat(frag);
				LblTree lt1 = LblTree.fromString(tt);
				LabelDictionary ld = new LabelDictionary();
				InfoTree it1 = new InfoTree(lt1, ld);
				
				for(int j=0; j<it1.info[POST2_PARENT].length; j++ ){
					if(it1.info[POST2_PARENT][j] > 0){
						int pp = it1.info[POST2_PARENT][j];
						String parentTag = ld.read(it1.info[POST2_LABEL][pp]).split("_")[0];
						if(parentTag.contains("?"))
							continue;
						String childTag = ld.read(it1.info[POST2_LABEL][j]).split("_")[0];
						if(childTag.contains("?"))
							continue;
						String parentID = ld.read(it1.info[POST2_LABEL][pp]).split("_")[1];
						String childID = ld.read(it1.info[POST2_LABEL][j]).split("_")[1];
						edges.put(parentID + "_" + childID, parentTag + "_" + childTag);						
					}
				}
			}
		}
		return edges;
	}
	
	/**
	 * The edges format is : (paretntID_childID , parentTag_childTag) 
	 * @param treeStr
	 * @return
	 */
	public static HashMap<String, String> extractAllEdgesOfTree(String treeStr) {
		HashMap<String, String> edges = new HashMap<String, String>();
		
		HashMap<Integer, Integer> mapID = new HashMap<Integer,Integer>(); //tree id to post-order id
		HashMap<Integer, Integer> mapIDRev = new HashMap<Integer,Integer>(); //post-order id to tree id
		Tree tree = Tree.valueOf(unconvertToDrawTable(treeStr));
		List<Tree> postOrder = tree.postOrderNodeList();
		int postID = 1;
		for(Tree node: postOrder){
			mapID.put(node.nodeNumber(tree), postID);
			mapIDRev.put(postID, node.nodeNumber(tree));
			postID++;
		}
		
		for(Tree node: postOrder){
			if((node.parent(tree) != null)){
				String parentTag = node.parent(tree).value();
				String childTag = node.value();
				int parentID = mapID.get(node.parent(tree).nodeNumber(tree));
				int childID = mapID.get(node.nodeNumber(tree));
				edges.put(parentID + "_" + childID, parentTag + "_" + childTag);
			}
		}
		
		return edges;
	}
	/**
	 * if the size of leaves of tree are not equal to size of words in the sentence, then remove the sentence
	 * and return True.
	 * also it checks the encoding of the sentence, if it contains a mal-encoded string, it will return false
	 * @param mtSent
	 * @param mtTree
	 * @return
	 */
	private static boolean isNotValidSentece(String sent, String treeStr) {
		int sentLen = sent.length() - countOccurrences(sent, ' ');
		Tree tree = Tree.valueOf(treeStr);
		int leavesLen = 0;
		List<Tree> leaves = tree.getLeaves();
		for( int i=0; i<leaves.size(); i++){
			leavesLen = leavesLen + leaves.get(i).value().length();
			if(leaves.get(i).value().equals("``") || leaves.get(i).value().equals("''"))
				leavesLen = leavesLen - 1;
			if (leaves.get(i).value().contains("/"))
				leavesLen = leavesLen - 1;
			if (leaves.get(i).value().contains("-LRB-") || leaves.get(i).value().contains("-RRB-"))
				leavesLen = leavesLen - 4;
		}
		if(Math.abs(leavesLen - sentLen)<2 && !isEncoded(sent))
			return false;
		return true;
	}
	
	public static boolean isEncoded(String text){
	    Charset charset = Charset.forName("US-ASCII");
	    String checked=new String(text.getBytes(charset),charset);
	    return !checked.equals(text);
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
    
	private static void printFinalStatistics() {
		System.out.println("----------Final Statistics------ Good & Bad averages");
		System.out.println(avgTEDGood/count + "\t" + avgFragNoGood/count + "\t" + avgFragLenGood/count + "\t" + avgOneFragGood/count  + "\t" + avgTEDBad/count + "\t" + avgFragNoBad/count + "\t" + avgFragLenBad/count + "\t" + avgOneFragBad/count);
	}

	private static void printFinalStatisticsRefTB() {
		System.out.println("----------Final Statistics------ Good & Bad averages for Ref and TB methods");
		System.out.println("count ref and TB: " + count + " " + countTB);
		System.out.println(avgTEDGood/count + "\t" + avgFragNoGood/count + "\t" + avgFragLenGood/count + "\t" + avgOneFragGood/count  + "\t" + avgTEDBad/count + "\t" + avgFragNoBad/count + "\t" + avgFragLenBad/count + "\t" + avgOneFragBad/count);
		System.out.println("0" + "\t" + avgFragNoGoodTB/countTB + "\t" + avgFragLenGoodTB/countTB + "\t" + avgOneFragGoodTB/countTB  + "\t" + "0" + "\t" + avgFragNoBadTB/countTB + "\t" + avgFragLenBadTB/countTB + "\t" + avgOneFragBadTB/countTB);

	}
	
	private static void updateStatistics(String fragmentOut) {
		String[] fragmentsInfo = fragmentOut.trim().split("\t");
		avgTEDGood = avgTEDGood + Double.valueOf(fragmentsInfo[0].trim()).doubleValue();
		avgFragNoGood = avgFragNoGood + Double.valueOf(fragmentsInfo[1].trim()).doubleValue();
		avgFragLenGood = avgFragLenGood + Double.valueOf(fragmentsInfo[2].trim()).doubleValue();
		avgOneFragGood = avgOneFragGood + Double.valueOf(fragmentsInfo[3].trim()).doubleValue();
		
		avgTEDBad = avgTEDBad + Double.valueOf(fragmentsInfo[4].trim()).doubleValue();
		avgFragNoBad = avgFragNoBad + Double.valueOf(fragmentsInfo[5].trim()).doubleValue();
		avgFragLenBad = avgFragLenBad + Double.valueOf(fragmentsInfo[6].trim()).doubleValue();
		avgOneFragBad = avgOneFragBad + Double.valueOf(fragmentsInfo[7].trim()).doubleValue();
		count++;
	}

	private static void updateStatisticsTB(String fragmentOut) {
		String[] fragmentsInfo = fragmentOut.trim().split("\t");
		avgFragNoGoodTB = avgFragNoGoodTB + Double.valueOf(fragmentsInfo[1].trim()).doubleValue();
		avgFragLenGoodTB = avgFragLenGoodTB + Double.valueOf(fragmentsInfo[2].trim()).doubleValue();
		avgOneFragGoodTB = avgOneFragGoodTB + Double.valueOf(fragmentsInfo[3].trim()).doubleValue();
		
		avgFragNoBadTB = avgFragNoBadTB + Double.valueOf(fragmentsInfo[5].trim()).doubleValue();
		avgFragLenBadTB = avgFragLenBadTB + Double.valueOf(fragmentsInfo[6].trim()).doubleValue();
		avgOneFragBadTB = avgOneFragBadTB + Double.valueOf(fragmentsInfo[7].trim()).doubleValue();
		countTB++;
	}
	
	private static String convertToDrawTable(String tree) {
		tree = tree.replace("(", "[");
		tree = tree.replace(")", "]");
		return tree;
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
	
	private static String readFragmentList(List<String> FragList) {
		String out = "";
		for (String string : FragList) {
			out = out + string + "\n";
		}
		return out;
	}

	private static String getSentenceFluency(Connection con, String table, String id) throws SQLException {
		Statement sta2 = con.createStatement();	
		String query = "SELECT * FROM " + table + " where ID=" + id ;	
		ResultSet rec2 = sta2.executeQuery(query);
		String fluency = null;
		if (rec2.next())
			fluency = rec2.getString("fluency");
		sta2.close();
		return fluency;
	}

	private static String getParseTreeFromDB(Connection con, String table, String id) throws SQLException {
		Statement sta2 = con.createStatement();	
		String query = "SELECT * FROM " + table + " where ID=" + id ;	
		ResultSet rec2 = sta2.executeQuery(query);
		String parse_tree=null;
		if (rec2.next())
			parse_tree = rec2.getString("parse_tree");
		sta2.close();
		return parse_tree;
	}

	private static String getSentFromDB(Connection con, String table, String id) throws SQLException {
		Statement sta2 = con.createStatement();	
		String query = "SELECT * FROM " + table + " where ID=" + id ;	
		ResultSet rec2 = sta2.executeQuery(query);
		String sent=null;
		if (rec2.next())
			sent = rec2.getString("sent");
		sta2.close();
		return sent;
	}
	
	private static boolean isTitle(Connection con, String table, String id) throws SQLException {
		Statement sta2 = con.createStatement();	
		String query = "SELECT * FROM " + table + " where ID=" + id ;	
		ResultSet rec2 = sta2.executeQuery(query);
		String title=null;
		if (rec2.next())
			title = rec2.getString("isTitle");
		sta2.close();
		if(title.equals("1"))
			return true;
		return false;
	}
	
	/**
	 * first we had to create the parse trees then fragment each good and bad sentence base on the reference parse tree
	 * @param refSent1
	 * @param refSent2
	 * @param mtSent
	 * @return
	 */
	public static String fragmentRef(String ref1, String ref2, String mt) {

		ref1 = Tree.valueOf(ref1).toString();
		ref2 = Tree.valueOf(ref2).toString();
		mt = Tree.valueOf(mt).toString();
		
		System.out.println("-------------------------\n" + convertToDrawTable(ref1) + "\n" + convertToDrawTable(ref2) + "\n" + convertToDrawTable(mt));

		goodRef = RefObj.fragmentwithRef(ref1, ref2);
		badRef = RefObj.fragmentwithRef(ref1, mt);
		
		String out = "good* " + goodRef.get(goodRef.size() - 1) + " ------- bad*  " + badRef.get(badRef.size() - 1);
		return out;
	}
	
	/**
	 * first we had to create the parse trees then fragment each good and bad sentence base on TB rules
	 * @param refSent1
	 * @param refSent2
	 * @param mtSent
	 * @return
	 */
	private static String fragmentTB0(String ref2, String mt) {
		ref2 = Tree.valueOf(ref2).toString();
		mt = Tree.valueOf(mt).toString();
		
		System.out.println("-------------------------\n" + convertToDrawTable(ref2) + "\n" + convertToDrawTable(mt));
		
		goodTB = fragObj.fragmentwithTB0(ref2);
		badTB = fragObj2.fragmentwithTB0(mt);
		
		String out = "good* " + goodTB.get(goodTB.size() - 1) + " ------- bad*  " + badTB.get(badTB.size() - 1);
		return out;
	}
	
	/**
	 * TB_1 method: fragment the input tree based on the extracted fragments from TB 
	 * @param ref2
	 * @param mt
	 * @return
	 */
	private static String fragmentTB1_Depth(String ref2, String mt) {
		ref2 = Tree.valueOf(ref2).toString();
		mt = Tree.valueOf(mt).toString();
		
		System.out.println("------------------------- " + count + " \n" + convertToDrawTable(ref2) + "\n" + convertToDrawTable(mt));
		
		goodTB = TB1D3Obj.fragment(ref2);
		badTB = TB1D3Obj2.fragment(mt);
		
		String out = "good* " + goodTB.get(goodTB.size() - 1) + " ------- bad*  " + badTB.get(badTB.size() - 1);
		return out;

	}
	
}
