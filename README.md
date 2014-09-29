## Parse Tree Fragmentation Evaluation Methodology

This package includes the code used in our [Technical report](http://www.cs.pitt.edu/techreports/reports/TR-2014-01-REPORT.pdf). 
The goal of this project is to address the problem of assigning reasonable syntactic analyses to disfluent sentences.
In order to achieve this goal, we introduce two heuristic methods to find reasonable subtrees of parse
trees.
Since there is no annotated corpus of segmented parse trees of disfluent sentences, evaluation is a challenging task. 
In this project, we propose an evaluation methodology by artificially generating errors based on machine translation (MT) errors.
Generating errors gives us additional information about the type and position of each error which we can then use to automatically find parts of the parse trees that make sense. 

#### Implementation 
This directory contains the code to generate artificially errors using machine translation error frequencies.
The main class is edu.pitt.isp.GoldFragment.mainError which needs Penn Treebank sentences and parse trees to generated artificially errors over them.
The two heuristic methods (REF and TBF) to fragment parse trees over disfluent sentences are also called in this method to compare fragments with the generated gold standard.

#### Results
This directory includes pre-compiled lists of tagged words extracted from all MT output systems in WMT12 shared task.
Also, it contains the count of pervious and next POS of each error type in order to calculate likelihood of position for the error type.
So, the position of an error in the sentence is decided based on the likelihood of error positions in the MT outputs. For
instance, an extra word appears more frequently between a determiner and a preposition than between a verb and a punctuation.
 

#### Citation
> Homa B. Hashemi. [Parse Tree Fragmentation Evaluation Methodology](http://www.cs.pitt.edu/techreports/reports/TR-2014-01-REPORT.pdf). Technical report TR-2014-01, University of Pittsburgh, 2014.


#### Contact
If you have any questions, please contact Homa Hashemi, Hashemi (at) cs dot pitt dot edu.