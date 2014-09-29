package edu.pitt.isp.treeProcessing;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import edu.pitt.isp.DB.Mysql;

public class addWordCategoriesToTree {

	private static HashMap<String, String> hashAllWordCat=new HashMap<String, String>();
	

	/**
	 * This method will add word categories instead of words to trees
	 * @param tree
	 * @return
	 */
	public String addWordCategoriesToParseTree(String tree) {
		tree = tree.replace(")", "]");
		tree = tree.replace("(", "[");
		
		tree = tree.replace("]", " ]");		
		String[] words = tree.split(" ");
		String treeCat = "";
		String wordCat = "";
		for(int i=0; i<words.length ;i++){
			if(!words[i].equals("]") && !words[i].startsWith("[")){
				wordCat = getWordCategory(words[i].split("_")[0]);
				String temp = words[i].replace(words[i].split("_")[0], wordCat);
				words[i] = " " + temp  ;
			}
			treeCat = treeCat + words[i];
		}
		
		return treeCat;		
	}

	public static String getWordCategory(String word) {
		String category = null;		
		if(hashAllWordCat.containsKey(word)){
			category = hashAllWordCat.get(word);
		} 		
		if(category == null)
			category=word;
		return category;
	}

	public void loadWordCategories() {
		try {
	 	    Connection con2 = Mysql.getConn("MTdata");	
			Statement st2 = con2.createStatement();
			
			String query = "SELECT * FROM wordCategories";			
			String word,lemma,category;
			ResultSet rec = st2.executeQuery(query);
			
			while(rec.next()){
				word = rec.getString("word");
				lemma = rec.getString("lemma");
				category = rec.getString("category");
				if(!hashAllWordCat.containsKey(word)){
					hashAllWordCat.put(word, lemma); //TODO: add category instead of lemma
				}
			}
	 	    st2.close();
	 	    con2.close();  	 	    
		
		} catch (Exception e) {
			System.err.println("Exception: "+e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
		}		
	}
	
}
