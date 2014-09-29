package edu.pitt.isp.GoldFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

import edu.pitt.isp.DB.Mysql;
import edu.pitt.isp.treeFragments.CompareFragmentSets;
import edu.pitt.isp.treeFragments.MLbuildTrainFile;
import edu.pitt.isp.treeFragments.Ref_fragmentWithRef;
import edu.pitt.isp.treeFragments.fragmentStatistics;
import edu.pitt.isp.treeFragments.goldStandard;
import edu.pitt.isp.treeFragments.TB.TB_0_fragmentWithTB;
import edu.pitt.isp.treeProcessing.addNodeInfos;
import edu.pitt.isp.treeProcessing.addWordCategoriesToTree;
import edu.pitt.isp.treeProcessing.matchLabels;
import edu.pitt.isp.util.StatisticsUtil;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.LabeledScoredTreeReaderFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeNormalizer;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * This is the main class to generate errors and then create gold standard fragments.
 * It first reads treebank sentences then apply artificially generated method over it.
 * For each inserted error, the parse tree over TB sentences is changes based on the type and position of inserted error.
 */

public class mainError {
	static HashMap<String, String> hashSent=new HashMap<String, String>();
	private static matchLabels matchLabelsObj = new matchLabels();
	private static addWordCategoriesToTree objWordCat;
	private static StatisticsUtil stat;
	
	public static void main(String args[]) throws IOException{	
		generateErrorAndGoldFragmentsUsingTBSentencesMysql(5000);
	}
	
	private static void generateErrorAndGoldFragmentsUsingTBSentencesMysql(int thresholdTB) {
		System.out.println("generate artificial errors of TB...");
		try {
			objWordCat = new addWordCategoriesToTree();
			objWordCat.loadWordCategories();
			
			TB_0_fragmentWithTB fragObj = new TB_0_fragmentWithTB();		
			fragObj.findGrammarRulesFreq();	
			fragObj.setThreshold(thresholdTB);
			
	 	    Connection con2 = Mysql.getConn("MTdata");	
			Statement st2 = con2.createStatement();
			Statement st3 = con2.createStatement();
			
			String query = "SELECT * FROM TB order by rand() limit 50000";
			String TB_id, origSent, origTree;
			ResultSet rec = st2.executeQuery(query);
			List<String> errorList;
			int count = 0;
			double avgFragNoGold = 0.0, avgFragNoRef =0.0, avgFragNoTB=0.0;
			BigDecimal avgFragLenGold = new BigDecimal(0.0);
			Double avgFragLenRef =0.0, avgFragLenTB=0.0;
			
			List<Double> RefFmeasureEdgeList = new ArrayList<Double>();
			List<Double> RefFmeasureNodeList = new ArrayList<Double>();
			List<Double> RefRandIndexList = new ArrayList<Double>();
			List<Double> RefNMIList = new ArrayList<Double>();
			List<Double> RefPrecisionList = new ArrayList<Double>();
			List<Double> RefRecallList = new ArrayList<Double>();
			List<Double> RefExactMatchList = new ArrayList<Double>();
			List<Double> RefARIEdgeList = new ArrayList<Double>();
			List<Double> RefARINodeList = new ArrayList<Double>();
			
			List<Double> TBFmeasureEdgeList = new ArrayList<Double>();
			List<Double> TBFmeasureNodeList = new ArrayList<Double>();
			List<Double> TBRandIndexList = new ArrayList<Double>();
			List<Double> TBNMIList = new ArrayList<Double>();
			List<Double> TBPrecisionList = new ArrayList<Double>();
			List<Double> TBRecallList = new ArrayList<Double>();
			List<Double> TBExactMatchList = new ArrayList<Double>();
			List<Double> TBARIEdgeList = new ArrayList<Double>();
			List<Double> TBARINodeList = new ArrayList<Double>();
			
			List<Double> ErrorCountList = new ArrayList<Double>();
			List<Double> TEDList = new ArrayList<Double>();
			
			
			while(rec.next()){
				TB_id = rec.getString("ID");
				origSent = queryOverDB("Select sent from TB where id='" + TB_id + "'" , st3);
				origTree = queryOverDB("Select tree from TB where id='" + TB_id + "'" , st3);
				System.out.println("--------\nTB ID: " + TB_id);
								
				if(origSent.length()<2 || origTree.length()<2 || hashSent.containsKey(origSent)) 
					continue;		    	
				hashSent.put(origSent, "1");
						
				//****Main process****
				//generate random number of error and for each generated error, build the deterministic gold standard fragments
				Random rand1 = new Random();		
				int randomErrorCount = rand1.nextInt(5) + 1; // number of total errors is maximum 2
				
				int errorCount = 0;
				boolean flag = true;
				String errorSent = origSent, errorType = "", errorTree="", errorPosition = "", errorWord = "", errorPOS = "";
				List<String> errorTypeList = new ArrayList<String>();
				List<Tree> fragments = new ArrayList<Tree>();
				ErrorGeneration errorObj = new ErrorGeneration(origSent, origTree);
				
				System.out.println(origSent);
				System.out.println(origTree);
				
				GoldFragments goldObj = new GoldFragments(origSent, origTree);
				System.out.println(convertToDrawTable(origTree.toString()));
				while(flag){
						//1. generate errors using MT error analysis results
						System.out.println("1. generate error:");
			    		errorList = errorObj.generateMTdistributionRandomError(errorSent);
			    	
			    		System.out.println(errorList);
			    		if(errorList.size() < 6)
			    			break;
			    		
						errorType = errorList.get(0);
						errorSent = errorList.get(1);
						errorPosition = errorList.get(3);
						errorWord = errorList.get(4);
						errorPOS = errorList.get(5);
						if(errorSent.length()<2)
							continue;
						errorCount++;
						errorTypeList.add(errorType);
						
			    		//2. extract deterministic gold standard fragment
			    		System.out.println("2. gold fragment");
			    		System.out.println("\nbefore:");
			    		printFragments(fragments);
			    		fragments = goldObj.makeGoldFragments(errorType, errorPosition, errorWord, errorPOS, errorSent);
			    		System.out.println("\nafter:");
			    		printFragments(fragments);
			    		
			    		System.out.println("\n");
						if(errorCount >= randomErrorCount)
							flag=false;
				}								
				//remove the NONE fragment form the gold standard fragments
				fragments = removeNONEFragment(fragments);
				
				//refine the sentence, in order to parse it. Stanford parser is sensitive to punctuation in the middle of the sentence
				String errorSentClean = errorSent.replace("-0-", "");
				errorSentClean = errorSentClean.replace(" .", ".");
	    		errorTree = parseSentence(errorSentClean).toString();

	    		//is the parse tree from standford parser valid? means that it matches the sentence in the leaves size only? if not continue;
	    		if(errorTree.length() < 2 || isNotValidSentece(errorSentClean, errorTree) || fragments.size()<1)
	    				continue;
	    		
				//calculate REf method fragmentation and TED
	    		Ref_fragmentWithRef refObj = new Ref_fragmentWithRef();
	    		List<String> refFragments = refObj.fragmentwithRef(unconvertToDrawTable(origTree), unconvertToDrawTable(errorTree)); 
				String TED = refObj.findTED(refFragments);
				String fragmentsRef = refObj.findFragments(refFragments);
				double WER = (new goldStandard()).calcWordErrorRate(origSent, errorSent);
				List<Integer> no10 = calcNumberOfEdges10(listTreeToSting(fragments), origTree);
				Integer no1 = no10.get(0);
				Integer no0 = no10.get(1);
				int sentLen = origSent.split(" ").length;
				
				double TEDint = Double.parseDouble(TED);

				if( TEDint > 20)
					continue;
				
				//calculate TB method fragmentation
				List<String> fragmentsTBList = fragObj.fragmentwithTB0(unconvertToDrawTable(errorTree));
				String fragmentsTB = refObj.findFragments(fragmentsTBList);			

				//compare two sets of fragments, Deterministic gold and Ref method
				System.out.println("\norignal sent:\n" + origSent + "\nerror sent:\n" + errorSent + "\n");
	    		System.out.println("\noriginal Tree:\n" + convertToDrawTable(origTree));
	    		System.out.println("\nerror Tree:\n" + convertToDrawTable(errorTree));
	    		
				System.out.println("\nGold Fragments:\n");
				printFragmentsOut(listTreeToSting(fragments));
				System.out.println("\nRef Fragments:\n");
				printFragmentsOut(stringToList(fragmentsRef));
				System.out.println("\nTB Fragments:\n");
				printFragmentsOut(stringToList(fragmentsTB));
				
				//find simple statistics from fragments
				avgFragNoGold = avgFragNoGold + fragments.size();
				avgFragNoRef = avgFragNoRef + stringToList(fragmentsRef).size();
				avgFragNoTB = avgFragNoTB + stringToList(fragmentsTB).size();
				
				avgFragLenGold = avgFragLenGold.add(new BigDecimal(findAvgLengthOfFragments(listTreeToSting(fragments))));
				avgFragLenRef = avgFragLenRef + findAvgLengthOfFragments(stringToList(fragmentsRef));
				avgFragLenTB = avgFragLenTB + findAvgLengthOfFragments(stringToListTB(fragmentsTB));
				
				TEDList.add(Double.parseDouble(TED));
				ErrorCountList.add((double) randomErrorCount);			

			
				//find Ref method similarity with Gold
				CompareFragmentSets compareRefObj = new CompareFragmentSets(stringToList(fragmentsRef), listTreeToSting(fragments), 
																			new addNodeInfos().addNodeNumbers(Arrays.asList(Tree.valueOf(errorTree))));
				double NMIRef = compareRefObj.compareFragmentsNormalizedMutualInformation(stringToList(fragmentsRef), listTreeToSting(fragments));
				double fmeasureEdgeRef = compareRefObj.compareFragmentsFmeasureSharedEdges(stringToList(fragmentsRef), listTreeToSting(fragments));
				double precisionRef = compareRefObj.getPrecision();
				double recallRef = compareRefObj.getRecall(); 
				double fmeasureNodeRef = compareRefObj.compareFragmentsFmeasureSharedNodes(stringToList(fragmentsRef), listTreeToSting(fragments));
				double randIndexRef = compareRefObj.compareFragmentsRandIndex( stringToList(fragmentsRef), listTreeToSting(fragments));	
				double ARIEdgeRef = compareRefObj.compareFragmentsAdjustedRandIndexSharedEdges(stringToList(fragmentsRef), listTreeToSting(fragments));
				double ARINodeRef = compareRefObj.compareFragmentsAdjustedRandIndexSharedNodes(stringToList(fragmentsRef), listTreeToSting(fragments));
				double exactMatchRef = compareRefObj.getExactMatch();
				System.out.println("\nF-score Edge Ref:" + fmeasureEdgeRef + "\tF-score Node Ref" + fmeasureNodeRef + "\tRandIndex Ref:" + randIndexRef + "\tNMI:" + NMIRef + "\n");
				
				RefFmeasureEdgeList.add(fmeasureEdgeRef);
				RefFmeasureNodeList.add(fmeasureNodeRef);
				RefRandIndexList.add(randIndexRef);
				RefNMIList.add(NMIRef);
				RefPrecisionList.add(precisionRef);
				RefRecallList.add(recallRef);
				RefExactMatchList.add(exactMatchRef);
				RefARIEdgeList.add(ARIEdgeRef);
				RefARINodeList.add(ARINodeRef);
				
				//find TB method similarity with Gold 
				List<String> fragmentsHead = (new addNodeInfos()).addHeadAndParentsToTree(fragments, Tree.valueOf(origTree));
				List<String> fragmentsHeadTB = (new addNodeInfos()).addHeadAndParentsToTree( stringToListTree(fragmentsTB), Tree.valueOf(errorTree));
				CompareFragmentSets compareTBObj = new CompareFragmentSets(fragmentsHeadTB, fragmentsHead, 
																			new addNodeInfos().addNodeNumbers(Arrays.asList(Tree.valueOf(errorTree))));
				double NMITB = compareTBObj.compareFragmentsNormalizedMutualInformation(fragmentsHeadTB, fragmentsHead);
				double fmeasureEdgeTB = compareTBObj.compareFragmentsFmeasureSharedEdges( fragmentsHeadTB, fragmentsHead);
				double precisionTB = compareTBObj.getPrecision();
				double recallTB = compareTBObj.getRecall();
				double fmeasureNodeTB = compareTBObj.compareFragmentsFmeasureSharedNodes(fragmentsHeadTB, fragmentsHead);
				double randIndexTB = compareTBObj.compareFragmentsRandIndex(fragmentsHeadTB, fragmentsHead);
				double ARIEdgeTB = compareTBObj.compareFragmentsAdjustedRandIndexSharedEdges(fragmentsHeadTB, fragmentsHead);
				double ARINodeTB = compareTBObj.compareFragmentsAdjustedRandIndexSharedNodes(fragmentsHeadTB, fragmentsHead);
				double exactMatchTB = compareTBObj.getExactMatch();
				System.out.println("\nF-score Edge TB:" + fmeasureEdgeTB  + "\tF-score Node TB:" + fmeasureNodeTB + "\tRandIndex TB:" + randIndexTB + "\tNMI:" + NMITB + "\n");
				
				TBFmeasureEdgeList.add(fmeasureEdgeTB);
				TBFmeasureNodeList.add(fmeasureNodeTB);
				TBRandIndexList.add(randIndexTB);
				TBNMIList.add(NMITB);
				TBPrecisionList.add(precisionTB);
				TBRecallList.add(recallTB);
				TBExactMatchList.add(exactMatchTB);
				TBARIEdgeList.add(ARIEdgeTB);
				TBARINodeList.add(ARINodeTB);
				
	    		count++;
	    		if(count>5000)
	    			break;
			}
			System.out.println("----\ncount: " + count);
			System.out.println("TB threshold teta: " + thresholdTB);
			System.out.println("Gold: avgNoFrag\tavgSize\n\t");
			System.out.println("Ref: avgNoFrag\tavgSize\tExactMatch\tExactMatchSD\tPrecision\tPrecisionSD\tRecall\tRecallSD"
					+ "\tFmeasure\tFmeasureSD\tRandIndex\tRandIndexSD\tNMI\tNMISD\tARI\tARISD\n\t");
			System.out.println("TB: avgNoFrag\tavgSize\tExactMatch\tExactMatchSD\tPrecision\tPrecisionSD\tRecall\tRecallSD"
					+ "\tFmeasure\tFmeasureSD\tRandIndex\tRandIndexSD\tNMI\tNMISD\tARI\tARISD\n\t");
			System.out.println(avgFragNoGold/count + "\t" +  avgFragLenGold.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP));
			System.out.println(avgFragNoRef/count + "\t" + avgFragLenRef/count + "\t" + stat.getMean(RefExactMatchList)  + "\t" + stat.getStandardDeviation(RefExactMatchList) +
								"\t" + stat.getMean(RefPrecisionList) + "\t" + stat.getStandardDeviation(RefPrecisionList) + 
								"\t" + stat.getMean(RefRecallList) + "\t" + stat.getStandardDeviation(RefRecallList) +
								"\t" + stat.getMean(RefFmeasureEdgeList) + "\t" + stat.getStandardDeviation(RefFmeasureEdgeList) + 
								"\t" + stat.getMean(RefFmeasureNodeList) + "\t" + stat.getStandardDeviation(RefFmeasureNodeList) +
								"\t" + stat.getMean(RefRandIndexList) + "\t" + stat.getStandardDeviation(RefRandIndexList) +
								"\t" + stat.getMean(RefNMIList) + "\t" + stat.getStandardDeviation(RefNMIList) + 
								"\t" + stat.getMean(RefARIEdgeList) + "\t" + stat.getStandardDeviation(RefARIEdgeList) +
								"\t" + stat.getMean(RefARINodeList) + "\t" + stat.getStandardDeviation(RefARINodeList));
			
			System.out.println(avgFragNoTB/count + "\t" + avgFragLenTB/count + "\t" + stat.getMean(TBExactMatchList) + "\t" + stat.getStandardDeviation(TBExactMatchList) +
								"\t" + stat.getMean(TBPrecisionList) + "\t" + stat.getStandardDeviation(TBPrecisionList) + 
								"\t" + stat.getMean(TBRecallList) + "\t" + stat.getStandardDeviation(TBRecallList) +
								"\t" + stat.getMean(TBFmeasureEdgeList) + "\t" + stat.getStandardDeviation(TBFmeasureEdgeList) + 
								"\t" + stat.getMean(TBFmeasureNodeList) + "\t" + stat.getStandardDeviation(TBFmeasureNodeList) + 
								"\t" + stat.getMean(TBRandIndexList) + "\t" + stat.getStandardDeviation(TBRandIndexList) +
								"\t" + stat.getMean(TBNMIList) + "\t" + stat.getStandardDeviation(TBNMIList) + 
								"\t" + stat.getMean(TBARIEdgeList) + "\t" + stat.getStandardDeviation(TBARIEdgeList) +
								"\t" + stat.getMean(TBARINodeList) + "\t" + stat.getStandardDeviation(TBARINodeList));

			System.out.println("avg ErrorCount: " + stat.getMean(ErrorCountList) + "\t" + stat.getStandardDeviation(ErrorCountList));
			System.out.println("avg TED: " + stat.getMean(TEDList) + "\t" + stat.getStandardDeviation(TEDList));
			
			
	 	    st2.close();
	 	    st3.close();
	 	    con2.close();  	 	    
		
		} catch (Exception e) {
			System.err.println("Exception: "+e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
		}
		
	}


	private static List<Tree> removeNONEFragment(List<Tree> fragments) {
		List<Tree> temp = new ArrayList<Tree>();
		for(Tree tree: fragments){
			if(tree.toString().startsWith("(-NONE- -0-"))
				continue;
			else
				temp.add(tree);
		}
		return temp;
	}

	private static double findAvgLengthOfFragments(List<String> fragmentTrees) {
		int sumLen = 0, count = 0, countQ = 0;
		int noFrag = fragmentTrees.size();
		System.out.println();
		int noOneFragments = 0;
		for (String string : fragmentTrees) {
			string = convertToDrawTable(string);
			count = countOccurrences(string, ']');
			countQ = countOccurrences(string, '?');
			sumLen = sumLen + (count - countQ);
			if((count - countQ) == 1)
				noOneFragments++;
		}
        double len = (double) sumLen/noFrag;
		return len;
	}



	private static List<String> stringToList(String fragmentsRef) {
		List<String> fragmentsStr = new ArrayList<String>();
		String[] temp = fragmentsRef.split("\t");
		for(int i=0; i<temp.length; i++){
			String frag = temp[i];
			fragmentsStr.add(frag);
		}
		return fragmentsStr;
	}

	private static List<String> stringToListTB(String fragments) {
		List<String> fragmentsStr = new ArrayList<String>();
		String[] temp = fragments.split("\t");
		for(int i=0; i<temp.length; i++){
			String frag = temp[i];
			frag = frag.replace(" )", ")");
			frag = convertToDrawTable(frag);
			frag = printFormatFragment(frag);
			fragmentsStr.add(frag);
		}
		return fragmentsStr;
	}
	
	private static List<Tree> stringToListTree(String fragmentsRef) {
		List<Tree> fragmentsStr = new ArrayList<Tree>();
		String[] temp = fragmentsRef.split("\t");
		for(int i=0; i<temp.length; i++){
			String frag = temp[i];
			Tree tree = Tree.valueOf(frag);
			Tree tree1 = Tree.valueOf(frag, new LabeledScoredTreeReaderFactory(new TreeNormalizer()));
			fragmentsStr.add(tree1);
		}
		return fragmentsStr;
	}


	private static List<String> listTreeToSting(List<Tree> fragments) {
		List<String> fragmentsStr = new ArrayList<String>();
		for(int i=0; i<fragments.size(); i++){
			String frag = fragments.get(i).toString();
			//unify labels
			matchLabels matchLabelsObj = new matchLabels();
			frag = matchLabelsObj.unifyTreeLabelsTB(frag);
			//keep words and then add word categories to leaves			
			frag = objWordCat.addWordCategoriesToParseTree(frag);
			frag = frag.replace(" [", "[");
			frag = frag.replace("["," [").trim();
			frag = printFormatFragment(frag);
			fragmentsStr.add(frag);
		}
		return fragmentsStr;
	}

	public static String printFormatFragment(String tree) {
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

	private static String printFragments(List<Tree> fragments) {
		String fragment = "";
		for(int i=0; i<fragments.size(); i++){
			String frag = fragments.get(i).toString();
			String temp = "";
			//put each leaf node to parentheses ()
			frag = frag.replace(")", " )");		
			String[] words = frag.split(" ");
			for(int j=0; j<words.length ;j++){
				if(!words[j].equals(")") && !words[j].startsWith("(")){
					words[j] = "(" + words[j] + ")";
				}
				temp = temp + words[j];
			}
			frag = temp;			
			fragment = fragment + "\t" +  convertToDrawTable(frag).replace("'", "''");
			System.out.println(convertToDrawTable(frag));
		}
		return fragment.trim();
	}

	private static void printFragmentsOut(List<String> fragments){
		for(int i=0; i<fragments.size(); i++){
			System.out.println(convertToDrawTable(fragments.get(i)));
		}
	}

	private static boolean isItADuplicateRecord(String query, Statement sta) throws SQLException {
		ResultSet rec = sta.executeQuery(query);
		if (rec.next())
			return true;
		else
			return false;
	}


	private static String queryOverDB(String query, Statement sta) throws SQLException {
		ResultSet rec = sta.executeQuery(query);
		String str = null;
		if (rec.next())
			str = rec.getString(1);
		return str;
	}

	
	/**
	 * Parse tree
	 * @param sentence
	 * @return
	 */
	private static Tree parseSentence(String sentence) {
		Tree tree = null;
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, parse");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    Annotation document = new Annotation(sentence);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(CoreMap sent: sentences) {
	    	tree = sent.get(TreeAnnotation.class);
	    }
	    
	    System.out.println(tree);
		return tree;
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
	
	public static List<Integer> calcNumberOfEdges10(List<String> fragments, String parse_MT) {
		//calculate number of edges with 1 and 0 label			
		HashMap<String, String> treeEdges = (new MLbuildTrainFile()).extractAllEdgesOfTree(unconvertToDrawTable(parse_MT));
		System.out.println("in new calcNumberOfEdges10, treeEdges: " + treeEdges);
		System.out.println("before calc 10 fragment: " + fragments);
		CompareFragmentSets compareObj = new CompareFragmentSets();
		HashMap<String, String> fragmentEdges = compareObj.extractAllEdgesOfFragments(fragments, new HashMap<String, Integer>());
		System.out.println("in new calcNumberOfEdges10, fragmentEdges: " + fragmentEdges);
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
	public static boolean isEncoded(String text){
	    Charset charset = Charset.forName("US-ASCII");
	    String checked=new String(text.getBytes(charset),charset);
	    return !checked.equals(text);
	}
}
