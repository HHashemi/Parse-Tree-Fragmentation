package edu.pitt.isp.GoldFragment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.lang.model.type.ErrorType;

import edu.stanford.nlp.io.EncodingPrintWriter.err;
import edu.stanford.nlp.trees.Tree;

public class ErrorGeneration {
	private String origSent;
	private Tree origTree;
	private String errorSent;
	private String origPOS;
	private int position;
	private String errorWord;
	private String errorPOS;
	private HashMap<Integer, String> wordList = new HashMap<Integer, String>();
	private HashMap<Integer, String> POSList = new HashMap<Integer, String>();
	private List<Integer> errorPositionList = new ArrayList<Integer>();
	
	private String preCompiledListArticle = "../results/errorGenerationFiles/MT.listArticles.txt";
	private String preCompiledListPreposition = "../results/errorGenerationFiles/MT.listPrepositions.txt";
	private String preCompiledListPunctuation = "../results/errorGenerationFiles/MT.listPunctuation.txt";
	private String preCompiledListContentWord = "../results/errorGenerationFiles/MT.listContentWords.txt";
	
	private String positionLikelihoodFilePunctuation = "../results/errorGenerationFiles/errorPosition.punctuation";
	private String positionLikelihoodFileMissThe = "../results/errorGenerationFiles/errorPosition.ArticleMissThe";
	private String positionLikelihoodFileMissA = "../results/errorGenerationFiles/errorPosition.ArticleMissA";
	private String positionLikelihoodFileExtraThe = "../results/errorGenerationFiles/errorPosition.ArticleExtraThe";
	private String positionLikelihoodFileExtraA = "../results/errorGenerationFiles/errorPosition.ArticleExtraA";
	private String positionLikelihoodFileConfusion = "../results/errorGenerationFiles/errorPosition.ArticleConfusion";
	private String positionLikelihoodFileOther = "../results/errorGenerationFiles/errorPosition.ArticleOther";
	private String positionLikelihoodFilePrepositionRep = "../results/errorGenerationFiles/errorPosition.PrepositionRep";
	private String positionLikelihoodFilePrepositionIns = "../results/errorGenerationFiles/errorPosition.PrepositionIns";
	private String positionLikelihoodFilePrepositionDel = "../results/errorGenerationFiles/errorPosition.PrepositionDel";
	private String positionLikelihoodFileWordRep = "../results/errorGenerationFiles/errorPosition.WordRep";
	private String positionLikelihoodFileWordIns = "../results/errorGenerationFiles/errorPosition.WordIns";
	private String positionLikelihoodFileWordDel = "../results/errorGenerationFiles/errorPosition.WordDel";
	
	private List<String> listArticle = new ArrayList<String>();
	private List<String> listPreposition = new ArrayList<String>();
	private List<String> listPunctuation = new ArrayList<String>();
	private List<String> listContentWord = new ArrayList<String>();
	private HashMap<String, String> mapPunctuation = new HashMap<String, String>();
	private HashMap<String, String> mapContentWord = new HashMap<String, String>();
	
	private HashMap<String, Double> mapPositionPunctuation = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleMissThe = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleMissA = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleExtraThe = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleExtraA = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleConfusion = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionArticleOther = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionPrepositionRep = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionPrepositionIns = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionPrepositionDel = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionWordRep = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionWordIns = new HashMap<String, Double>();
	private HashMap<String, Double> mapPositionWordDel = new HashMap<String, Double>();

	
	//The MT analysis frequencies
	double article_freq = 0.09;
	double preposition_freq = 0.14;
	double wordChoice_freq = 0.65;
	double punctuation_freq = 0.08;
	
	double A_miss_the = 0.16;
	double A_miss_a = 0.12;
	double A_ex_the = 0.36;
	double A_ex_a = 0.11;
	double A_conf = 0.08;
	double A_other = 0.17;
	
	double P_repl = 0.19;
	double P_ins = 0.46;
	double P_del = 0.35;
	
	double W_repl = 0.16;
	double W_ins = 0.42;
	double W_del = 0.42;
	
	/**
	 * This is the constructor and initialize the wordList and POSList which will be used in error generation process.
	 * @param origSent
	 * @param origTree
	 * @throws IOException
	 */
	public ErrorGeneration(String origSent, String origTree) throws IOException{
		initialize(origSent, origTree);
	}
	
	/**
	 * This method generate errors using MT distribution of errors
	 * @param origSent
	 * @param origTree
	 * @return
	 * @throws InterruptedException
	 * @throws IOException 
	 */
	public List<String> generateMTdistributionRandomError(String sent) throws InterruptedException, IOException {
		List<String> errorList = new ArrayList<String>();
		errorList.add("");
		errorList.add("");
		if(wordList.size()<2){
			return errorList;
		}
	
		int randomErrorCount = 1;
		int errorCount = 0;
		
		boolean flag = true;
		String errorType = "";
		int count = 0;
		//generate a random error
		while(flag){
			double rand = Math.random(); //random float in range (0,1)	
						
			if(rand < article_freq) {
				System.out.println("article error");
				errorType = makeArticleError();			
			} else if (rand <= (article_freq + preposition_freq)){
				System.out.println("prepositin error");
				errorType = makePrepositionError();			
			} else if (rand < (article_freq + preposition_freq + wordChoice_freq)){
				System.out.println("word choice error");
				errorType = makeWordChoiceError();
			} else {
				System.out.println("punctuation error");
				errorType = makePunctuationError();
			}		
			if (errorType != ""){
				errorCount++;
			}
			
			if(errorCount >= randomErrorCount){
				flag=false;
				break;
			}
			count++;
			if (count > wordList.size()*10){ //in order to prevent infinite loop to generate error
				return errorList;
			}
		}		
		
		//update wordList and POSList, so we can use them in future adding errors
		//only when a new word is inserted to the sentence
		if (errorType.startsWith("ex_") || errorType.endsWith("_ins")){
			insertShiftHashmap(position, errorWord, errorPOS);
			addErrorPositionList(position);
			errorPositionList.add(position+1);
		} else
			errorPositionList.add(position);

		errorList.set(0, errorType);
		errorList.set(1, errorSent);
		errorList.add(2, String.valueOf(errorCount));
		errorList.add(3, Integer.toString(position)); //position -1 : because the words start with 1 here, but the parser starts with 0 , but I changed it, because the labels starts from 1 in goldFragment class
		errorList.add(4, errorWord);
		errorList.add(5, errorPOS);
		
		return errorList;
	}
	

	private String makeArticleError() {
		boolean flag = true;
		String type = "";
		int position = 0;
		int count=0;
		while(flag){
			double rand = Math.random();
						
			//missing the
			if (rand < A_miss_the && wordList.containsValue("the")) {
				position = deleteWord("the", "DT", mapPositionArticleMissThe);
				type = "miss_the";
				
		    //missing a or an
			} else if (rand < (A_miss_the + A_miss_a) && (wordList.containsValue("a") || wordList.containsValue("an") ) ){
				if(rand <= (A_miss_the + A_miss_a/2) && wordList.containsValue("a"))
					position = deleteWord("a", "DT", mapPositionArticleMissA);
				else
					position = deleteWord("an", "DT", mapPositionArticleMissA);
				type = "miss_a";
				
		    //extra the
			} else if (rand < (A_miss_the + A_miss_a + A_ex_the)) { 
				position = insertExtraArticle("the", mapPositionArticleExtraThe);
				type = "ex_the";
				
		    //extra a or an
			} else if (rand < (A_miss_the + A_miss_a + A_ex_the + A_ex_a)) { 
				if(rand <= (A_miss_the + A_miss_a + A_ex_the + A_ex_a/2))
					position = insertExtraArticle("a", mapPositionArticleExtraA);
				else
					position = insertExtraArticle("an", mapPositionArticleExtraA);
				type = "ex_a";
				
			//article confusion between the and a/an
			} else if (rand < (A_miss_the + A_miss_a + A_ex_the + A_ex_a + A_conf) && (wordList.containsValue("the") || wordList.containsValue("a") || wordList.containsValue("an"))) {
				position = confuseArticle(mapPositionArticleConfusion);
				type = "a_conf";
				
		    //other article error: replace, insert or delete other articles	
			} else {
				int randomOtherError = (new Random()).nextInt(3) + 1;
				switch(randomOtherError){
					case 1: position = replaceTagWord(listArticle, mapPositionArticleOther);
							type = "a_other_repl";
							break;
					case 2: position = insertExtraArticle("", mapPositionArticleOther);
							type = "a_other_ins";
							break;
					case 3: position = deleteTag("DT", mapPositionArticleOther);
							type = "a_other_del";
							break;		
				}
				
			}
			if(position != -1) {
				this.position = position;
				this.errorPOS = "DT";
				flag = false;
			}
			count++;
			if (count > wordList.size()*10){
				return "";
			}
		}
		return type;
	}
	

	private String makePrepositionError() {
		boolean flag = true;
		String type = "";
		int position = 0;
		int count = 0;
		while(flag){
			double rand = Math.random();

			//preposition replacement
			if (rand < P_repl && POSList.containsValue("IN")) {
				System.out.println("preposition replacement error");
				position = replaceTagWord(listPreposition, mapPositionPrepositionRep);
				type = "p_repl";
				
		    //preposition insertion
			} else if (rand < (P_repl + P_ins)){
				System.out.println("preposition insertion error");
				position = insertExtraWord(listPreposition, mapPositionPrepositionIns);
				type = "p_ins";
				
		    //preposition deletion
			} else{ 
				System.out.println("preposition deletion error");
				position = deleteTag("IN", mapPositionPrepositionDel);
				type = "p_del";
			}
			if(position != -1){
				this.position = position;
				this.errorPOS = "IN";
				flag = false;
			}
			count++;
			if (count > wordList.size()*10){
				return "";
			}
		}
		return type;
	}


	private String makeWordChoiceError() {
		boolean flag = true;
		String type = "";
		int position = 0;
		int count = 0;
		while(flag){
			double rand = Math.random();
			//content word replacement
			if (rand < W_repl) {
				position = replaceTagWord(listContentWord, mapPositionWordRep);
				type = "w_repl";
				
		    //content word insertion
			} else if (rand < (W_repl + W_ins)){
				position = insertExtraWord(listContentWord, mapPositionWordIns);
				type = "w_ins";
				
		    //content word deletion
			} else{ 
				position = deleteWord(listContentWord, mapPositionWordDel);
				type = "w_del";
			}
			if(position != -1) {
				this.position = position;
				this.errorPOS = mapContentWord.get(errorWord);
				flag = false;
			}
			count++;
			if (count > wordList.size()*10){
				return "";
			}
		}
		return type;
	}
	

	/**
	 * we don't have distribution of insertion and deletion of punctuation errors, so we randomly pick one of the replacement, insertion and deletion errors.
	 * @return
	 */
	private String makePunctuationError() {
		boolean flag = true;
		String type = "";
		int position = 0;
		int count = 0;
		while(flag){
			int randomOtherError = (new Random()).nextInt(3) + 1;
			switch(randomOtherError){
				case 1: 
						System.out.println("Replace punctuation");
						position = replaceTagWord(listPunctuation, mapPositionPunctuation);
						type = "punc_repl";
						break;
				case 2: 
						System.out.println("Insert punctuation");
						//in order to remove . from the punctuation list, because adding . in the middle of sentence will make parser to separate the sentence
						listPunctuation.remove(listPunctuation.indexOf("."));
						listPunctuation.remove(listPunctuation.indexOf("?"));
						//listPunctuation.remove(listPunctuation.indexOf("!"));
						position = insertExtraWord(listPunctuation, mapPositionPunctuation);
						listPunctuation.add(".");
						listPunctuation.add("?");
						//listPunctuation.add("!");
						type = "punc_ins";
						break;
				case 3: 
						System.out.println("Delete punctuation");
						position = deleteWord(listPunctuation, mapPositionPunctuation);
						type = "punc_del";
						break;	
			}
			if(position != -1){
				this.position = position;
				this.errorPOS = mapPunctuation.get(errorWord);
				flag = false;
			}			
			count++;
			if (count > wordList.size()*10){
				return "";
			}
		}
		return type;
	}


	/**
	 * constructor method
	 * @param origSent2
	 * @param origTree2
	 * @throws IOException 
	 */
	private void initialize(String origSent2, String origTree2) throws IOException {
		this.origSent = origSent2;
		this.origTree = Tree.valueOf(origTree2);
		this.errorSent = "";
		this.origPOS = "";
		
		int j=0;
		for(int i=1; i<=origTree.size(); i++){
			Tree node = origTree.getNodeNumber(i);
			if(node.isPreTerminal()){
				j++;
				POSList.put(j, node.value());
				wordList.put(j, node.getChild(0).value());
				origPOS = origPOS + node.value() + " "; 
			}
		}
		fillPreCompiledList(preCompiledListArticle, listArticle);
		fillPreCompiledList(preCompiledListPreposition, listPreposition);
		fillPreCompiledList(preCompiledListPunctuation, listPunctuation, mapPunctuation);
		fillPreCompiledList(preCompiledListContentWord, listContentWord, mapContentWord);
		
		mapPositionPunctuation = fillPositionLikelihood(positionLikelihoodFilePunctuation);
		mapPositionArticleMissThe = fillPositionLikelihood(positionLikelihoodFileMissThe);
		mapPositionArticleMissA = fillPositionLikelihood(positionLikelihoodFileMissA);
		mapPositionArticleExtraThe = fillPositionLikelihood(positionLikelihoodFileExtraThe);
		mapPositionArticleExtraA = fillPositionLikelihood(positionLikelihoodFileExtraA);
		mapPositionArticleConfusion = fillPositionLikelihood(positionLikelihoodFileConfusion);
		mapPositionArticleOther = fillPositionLikelihood(positionLikelihoodFileOther);
		mapPositionPrepositionRep = fillPositionLikelihood(positionLikelihoodFilePrepositionRep);
		mapPositionPrepositionIns = fillPositionLikelihood(positionLikelihoodFilePrepositionIns);
		mapPositionPrepositionDel = fillPositionLikelihood(positionLikelihoodFilePrepositionDel);
		mapPositionWordRep = fillPositionLikelihood(positionLikelihoodFileWordRep);
		mapPositionWordIns = fillPositionLikelihood(positionLikelihoodFileWordIns);
		mapPositionWordDel = fillPositionLikelihood(positionLikelihoodFileWordDel);
	}
	
	private HashMap<String, Double> fillPositionLikelihood(String file) throws NumberFormatException, IOException {
		HashMap<String, Double> temp = new HashMap<String, Double>(); 
		HashMap<String, Double> map = new HashMap<String, Double>(); 
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		double freq;
		double sum = 0;
		while((line = br.readLine()) != null) {
			freq = Double.parseDouble(line.split("=")[1]);
			sum = sum + freq;
			temp.put(line.split("=")[0].trim(), freq);
		}
		for(String key : temp.keySet()){
			freq = temp.get(key)/sum;
			map.put(key, freq);
		}
		return map;
	}

	private void fillPreCompiledList(String file, List<String> list) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while((line = br.readLine()) != null) {
			list.add(line.split("\t")[0].trim());
		}
	}

	private void fillPreCompiledList(String file, List<String> list, HashMap<String, String> map) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while((line = br.readLine()) != null) {
			list.add(line.split("\t")[0].trim());
			if(!map.containsKey(line.split("\t")[0].trim()))
				map.put(line.split("\t")[0].trim(), line.split("\t")[1].trim());
		}
	}
	
	private int deleteWord(String word, String tag, HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihood(positionLikelihood); 
		int randomWord;
		
		String sent = "";
		int count=0;
		do{
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while delete tag");
			count++;
			if (count > wordList.size()*10){
				return -1;
			}
			if(!wordList.containsKey(randomWord)){
				System.out.println("deleteWord(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
			if(errorPositionList.contains(randomWord))
				System.out.println("***");
		}while(!wordList.get(randomWord).equals(word) || !POSList.get(randomWord).equals(tag) || wordList.get(randomWord).equals("-0-") || errorPositionList.contains(randomWord));
		
//		Random rand = new Random();
//		int randomWord = rand.nextInt(wordList.size()) + 1;

		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				errorWord = wordList.get(randomWord);
				sent = sent + "-0-" + " ";
				wordList.put(j, "-0-");
				POSList.put(j, "-NONE-");
				continue;
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}	
		errorSent = sent.trim();
		return randomWord;
	}
	
	/**
	 * deleting a pos tag 
	 * @param string
	 * @return
	 */
	private int deleteTag(String tag, HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihood(positionLikelihood); 
		int randomWord;
		
		String sent = "";
		int count=0;
		do{
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while delete tag ");
			count++;
			if (count > wordList.size()*10){
				return -1;
			}
			if(!wordList.containsKey(randomWord)){
				System.out.println("deleteTag(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
			if(errorPositionList.contains(randomWord))
				System.out.println("***");
		}while(!POSList.get(randomWord).equals(tag) && (!wordList.get(randomWord).equals("the") && !wordList.get(randomWord).equals("a") && !wordList.get(randomWord).equals("an")) || wordList.get(randomWord).equals("-0-") || errorPositionList.contains(randomWord));
		//all the words can be removed except three articles (the/a/an) that are investigated as separate errors

		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				errorWord = wordList.get(randomWord);
				sent = sent + "-0-" + " ";
				wordList.put(j, "-0-");
				POSList.put(j, "-NONE-");
				continue;
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}

	/**
	 * remove one of the words from the pre-compiled list extracted from TB
	 * @return
	 */
	private int deleteWord(List<String> list, HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihood(positionLikelihood); 
		int randomWord;
		String sent = "";
		int count=0;
		System.out.println(sentPositionLikelihood);
		do{			
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while delete word from pre-compiled list");
			count++;
			if (count > wordList.size()*10){
				return -1;
			}
			if(!wordList.containsKey(randomWord)){
				System.out.println("deleteWord(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
			if(errorPositionList.contains(randomWord))
				System.out.println("***");
		}while(wordList.get(randomWord).equals("-0-") || !list.contains(wordList.get(randomWord).toLowerCase()) || (listArticle.contains(wordList.get(randomWord)) || listPreposition.contains(wordList.get(randomWord))) || errorPositionList.contains(randomWord));
		
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				errorWord = wordList.get(randomWord);
				sent = sent + "-0-" + " ";
				wordList.put(j, "-0-");
				POSList.put(j, "-NONE-");
				continue;
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}
	
	
	private int insertExtraWord(List<String> list, HashMap<String, Double> positionLikelihood){
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihoodForInsertion(positionLikelihood); 
		
 		Random rand = new Random();
		int randomWord;
		String sent = "";
		int count=0;
		String word = "";
				
		do{
			//randomWord = rand.nextInt(wordList.size()-1) + 1;
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while insert extra word ");
			count++;
			if (count > wordList.size()*10)
				return -1;
			if(randomWord ==0)
				break;
			if(!wordList.containsKey(randomWord)){
				System.out.println("wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
		}while( wordList.get(randomWord).equals("-0-") );
					
		if(randomWord==0){
			word = pickRandomWordFromPrecompiledList(list);
			errorWord = word;
			sent = sent + " " + word + " ";
		}
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				word = pickRandomWordFromPrecompiledList(list);
				errorWord = word;
				sent = sent + wordList.get(j) + " " + word + " ";
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}
	
	/**
	 * Insert an extra article before JJ,NN and not after DT/JJ/NN
	 * @return
	 */
	private int insertExtraArticle(String article, HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihoodForInsertion(positionLikelihood);
		Random rand = new Random();
		int randomWord;
		String sent = "";
		int count=0;
		if(article == ""){
			article = pickRandomWordFromPrecompiledList(listArticle);
		}
		if(sentPositionLikelihood.size()<1)
			return -1;
		do{
			//randomWord = rand.nextInt(wordList.size()-1) + 1;
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while insert extra article ");
			count++;
			if (count > wordList.size()*10)
				return -1;
			if(randomWord ==0)
				break;
			System.out.println(sentPositionLikelihood);
			System.out.println(randomWord);
			if(!wordList.containsKey(randomWord)){
				System.out.println("insertExtraArticle(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
		}while(wordList.get(randomWord).equals("-0-") );
		// I have seen some MT outputs like 'the the', "the is", "the have"
			
		if(randomWord==0){
			errorWord = article;
			sent = sent + " " + article + " ";
		}
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				errorWord = article;
				sent = sent + wordList.get(j) + " ";
				sent = sent + article + " ";
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}
	
	/**
	 * confuse articles the with a/an
	 * @return
	 */
	private int confuseArticle(HashMap<String, Double> positionLikelihood){
		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihood(positionLikelihood);
		
		Random rand = new Random();
		int randomWord;
		String sent = "";
		int count=0;
		String article = "";
		do{
			//randomWord = rand.nextInt(wordList.size()) + 1;
			randomWord = getRandomPosition(sentPositionLikelihood);
			System.out.println("while confuse article");
			count++;
			if (count > wordList.size()*10)
				return -1;
			if(!wordList.containsKey(randomWord)){
				System.out.println("confuseArticles(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
			if(errorPositionList.contains(randomWord))
				System.out.println("***");
		}while(!wordList.get(randomWord).equals("the") && !wordList.get(randomWord).equals("a") && !wordList.get(randomWord).equals("an") || errorPositionList.contains(randomWord));
				
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				if (wordList.get(randomWord).equals("the"))
					article = "a";
				else
					article = "the";
				errorWord = article;
				sent = sent + article + " ";
				wordList.put(j, article);
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}
	
	/**
	 * replace one of the words with another word from the list that has the same POS tag
	 * @param listArticle2
	 * @return
	 */
 	private int replaceTagWord(List<String> list, HashMap<String, Double> positionLikelihood) {
 		HashMap<Integer, Double> sentPositionLikelihood = getSentPositionLikelihood(positionLikelihood);
 		Random rand = new Random();
		int randomWord;
		String sent = "";
		int count=0;
		String word = "";
		boolean flag = false;
		do{
			//randomWord = rand.nextInt(wordList.size()) + 1;
			randomWord = getRandomPosition(sentPositionLikelihood);
			count++;
			if (count > wordList.size()*10)
				return -1;
			if(!wordList.containsKey(randomWord)){
				System.out.println("replaceTagWord(): wordlist does not contain randomWord pos: " + randomWord);
				return -1;
			}
			if(list.size()>100 && mapContentWord.containsKey(wordList.get(randomWord).toLowerCase())){
				if(!mapContentWord.get(wordList.get(randomWord).toLowerCase()).equals(POSList.get(randomWord)) )
					flag = true;
				else
					flag = false;
			}
			if(errorPositionList.contains(randomWord))
				System.out.println("***");
		}while(!list.contains(wordList.get(randomWord).toLowerCase()) || flag || errorPositionList.contains(randomWord)); //check the POS of the replaced words 
					
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				word = pickRandomWordFromPrecompiledList(list, POSList.get(j));
				errorWord = word;
				sent = sent + word + " ";
				wordList.put(j, word);
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		errorSent = sent.trim();
		return randomWord;
	}

 	
	private String pickRandomWordFromPrecompiledList(List<String> list) {
		int randomWord = (new Random()).nextInt(list.size());
		String word = list.get(randomWord);
		return word;
	}

	private String pickRandomWordFromPrecompiledList(List<String> list, String POS) {
		int randomWord = 0;
		String word;
		do {
			randomWord = (new Random()).nextInt(list.size());
			word = list.get(randomWord);
			if(list.size()<100)
				break;
		}while(!POS.equals(mapContentWord.get(word)));
		return word;
	}

	/**
	 * Swapping two words
	 * a random word with the word after it
	 * @return
	 */
	private String swapWordOrder() {
		Random rand = new Random();
		int randomWord; 
		String sent = "";
		int count=0;
		do{
			randomWord = rand.nextInt(wordList.size()-1) + 1; // it has size()-1
			System.out.println("while swapping word");
			count++;
			if (count > wordList.size()*10)
				return sent;
		}while((wordList.get(randomWord).length() < 1) || (wordList.get(randomWord+1).length() < 1) || 
			   (wordList.get(randomWord).length()==1 && !Character.isLetter(wordList.get(randomWord).charAt(0))) || 
			   (wordList.get(randomWord+1).length()==1 && !Character.isLetter(wordList.get(randomWord+1).charAt(0)))); // this word may be punctuation				
		
		for(int j=1; j<=wordList.size(); j++){
			if(j == randomWord){
				sent = sent + wordList.get(j+1) + " ";
				sent = sent + wordList.get(j) + " ";
				String temp = wordList.get(j);
				wordList.put(j, wordList.get(j+1));
				wordList.put(j+1, temp);
				temp = POSList.get(j);
				POSList.put(j, POSList.get(j+1));
				POSList.put(j+1, temp);
				j++;
			}else{
				sent = sent + wordList.get(j) + " ";
			}		
		}		
		return sent.trim();
	}
	
	private void insertShiftHashmap(int position, String word, String POS) {
		if(POS == "" && mapContentWord.containsKey(word)) 
			POS = mapContentWord.get(word);
		else if (POS == "" && mapPunctuation.containsKey(word))
			POS = mapPunctuation.get(word);
		
		HashMap<Integer, String> wordListTemp = new HashMap<Integer, String>();
		HashMap<Integer, String> POSListTemp = new HashMap<Integer, String>();
		int i = 1;
		while(i<=wordList.size()+1){
			if(i<=position) {
				wordListTemp.put(i, wordList.get(i));
				POSListTemp.put(i, POSList.get(i));
				i++;
			} else if (i== (position+1)){
				wordListTemp.put(i, word);
				POSListTemp.put(i, POS);
				i++;
			} else{
				wordListTemp.put(i, wordList.get(i-1));
				POSListTemp.put(i, POSList.get(i-1));
				i++;
			}
		}
		wordList.clear();
		wordList.putAll(wordListTemp);
		POSList.clear();
		POSList.putAll(POSListTemp);
	}
	
	private void addErrorPositionList(int position){
		for(int i=0; i<errorPositionList.size(); i++){
			if(errorPositionList.get(i)>position)
				errorPositionList.set(i, errorPositionList.get(i)+1);
		}
	}
	
	/**
	 * find the likelihood of each position in the sentence
	 * @param positionLikelihood
	 * @return
	 */
	private HashMap<Integer, Double> getSentPositionLikelihood(HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentLikelihood = new HashMap<Integer, Double>();
		String beforeTag, afterTag;
		double sum = 0;
		for(int i=1; i<=POSList.size(); i++){
			if(i==1) 
				beforeTag = "0";
			else
				beforeTag = POSList.get(i-1);
			
			if(i==POSList.size())
				afterTag = "0";
			else
				afterTag = POSList.get(i+1);
			
			String key = beforeTag + "_" + afterTag;
			if(positionLikelihood.containsKey(key)){
				sentLikelihood.put(i, positionLikelihood.get(key));
				sum = sum + positionLikelihood.get(key);
			}
		}
		for(int key : sentLikelihood.keySet()){
			sentLikelihood.put(key, sentLikelihood.get(key)/sum);
		}
		return sentLikelihood;
	}
	/**
	 * For insertion errors, there are n+1 places that a new word can be added
	 * @param positionLikelihood
	 * @return
	 */
	private HashMap<Integer, Double> getSentPositionLikelihoodForInsertion(HashMap<String, Double> positionLikelihood) {
		HashMap<Integer, Double> sentLikelihood = new HashMap<Integer, Double>();
		String beforeTag, afterTag;
		double sum = 0;
		//do not enter an error at the end of the sentence !! i<=POSList.size()+1
		for(int i=1; i<= POSList.size(); i++){
			if(i==1) 
				beforeTag = "0";
			else
				beforeTag = POSList.get(i-1);
			
			afterTag = POSList.get(i);
			
			String key = beforeTag + "_" + afterTag;
			if(positionLikelihood.containsKey(key)){
				sentLikelihood.put(i-1, positionLikelihood.get(key));
				sum = sum + positionLikelihood.get(key);
			}
		}
		for(int key : sentLikelihood.keySet()){
			sentLikelihood.put(key, sentLikelihood.get(key)/sum);
		}
		return sentLikelihood;
	}
	
	/**
	 * find a random position in the sentence based on a vector of probability of positions
	 * @param sentPositionLikelihood
	 * @return
	 */
	private int getRandomPosition(HashMap<Integer, Double> sentPositionLikelihood) {
		double rand = Math.random();
		double sum = 0;
		for(Integer key : sentPositionLikelihood.keySet()){
			sum = sum + sentPositionLikelihood.get(key);
			if(rand < sum)
				return key;
		}
		return -1;
	}

}
