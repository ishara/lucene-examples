package com.vthacker.luceneExamples;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.List;

public class Autosuggest {
  
  public static final String FIELD = "title";
  public static final String INDEX_DIR = "/tmp/lucene/";
  public static final int RESULTS_TO_DISPLAY = 10;
  
  private FuzzySuggester suggestor = new FuzzySuggester(Autosuggest.getAnalyzer());
  
  public static Analyzer getAnalyzer() {
  //Defining a custom analyzer which will be used to index and suggest the data set
    Analyzer autosuggestAnalyzer = new Analyzer() {
      
      final String [] stopWords =  {"a", "an", "and", "are", "as", "at", "be", "but", "by",
          "for", "if", "in", "into", "is", "it",
          "no", "not", "of", "on", "or", "s", "such",
          "t", "that", "the", "their", "then", "there", "these",
          "they", "this", "to", "was", "will", "with"};
      
      @Override
      protected TokenStreamComponents createComponents(final String fieldName) {
        final Tokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tok = new LowerCaseFilter(tokenizer);
        tok = new StopFilter(tok, StopFilter.makeStopSet( stopWords, true));
        return new TokenStreamComponents(tokenizer, tok) {
          @Override
          protected void setReader(final Reader reader) {
            super.setReader(reader);
          }
        };
      }
    };
    return autosuggestAnalyzer;
  }
  
  /**
   * 
   * @param indexDir Pass the directory where the index is made
   * This uses the default field
   * @return
   */
  public boolean buildSuggestor(String indexDir) {
    try {
      Directory dir = new MMapDirectory(new File(indexDir).toPath());
      return buildSuggestor(dir, FIELD);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    
  }
  
  /**
   * 
   * @param indexDir Pass the directory where the index is made
   * @param fieldName Uses the field to build the index
   * @return
   */
  public boolean buildSuggestor(String indexDir, String fieldName) {
    try {
      Directory dir = new MMapDirectory(new File(indexDir).toPath());
      return buildSuggestor(dir, fieldName);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean buildSuggestor(Directory directory, String fieldName) {

    IndexReader reader;
    try {
      
      reader = DirectoryReader.open(directory);
      LeafReader aReader = SlowCompositeReaderWrapper.wrap(reader); // Should use reader.leaves instead ?
      Terms terms = aReader.terms(fieldName);
      
      if (terms == null) return false; 
      
      TermsEnum termEnum = terms.iterator();
//      TermFreqIterator wrapper = new TermFreqIterator.TermFreqIteratorWrapper(termEnum);
      InputIterator inputIterator =new InputIterator.InputIteratorWrapper(termEnum);
      suggestor.build(inputIterator);
      
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }
  
  public String[] suggest(String q) throws IOException {
    List<LookupResult> results = suggestor.lookup(CharBuffer.wrap(q), false, Autosuggest.RESULTS_TO_DISPLAY);
    String[] autosuggestResults = new String[results.size()];
    for(int i=0; i < results.size(); i++) {
      LookupResult result = results.get(i);
      autosuggestResults[i] = result.key.toString();
    }
    
    return autosuggestResults;
  }

}
