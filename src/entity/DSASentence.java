package entity;

import common.Constants;
import common.util;
import org.ansj.domain.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-02-19 20:27
 * Email: jhji@ir.hit.edu.cn
 */
public class DSASentence
{
    private String content;

    private ArrayList<DSARelation> relations;
    private ArrayList<DSAConnective> conWords;

    private ArrayList<ParallelConnective> parallelConnectives;

    /**保存了分词信息**/
    private List<Term> ansjWordTerms;
    private ArrayList<DSAEDU> edus;

    public DSASentence(String content)
    {
        this.content  = content;

        this.relations = new ArrayList<DSARelation>();
        this.conWords = new ArrayList<DSAConnective>();
        this.parallelConnectives = new ArrayList<ParallelConnective>();

        this.edus = new ArrayList<DSAEDU>();
    }

    public String getConnWordContent()
    {
        String result = "";
        for(DSAConnective curConn:conWords)
        {
            result += curConn.getContent() + ";";
        }

        if( result.length() > 1)
            result = result.substring(0, result.length() - 1);
        return  result;
    }

    public boolean containWord(String word)
    {
        boolean result = false;

        String[] words = word.split(";");

        //if( conWords.size() - words.length > 1 ) return false;


        for(int index = 0; index < words.length; index++)
        {
            String curWord = words[index];

            result = false;

            for(DSAConnective curConn : conWords)
            {
                if( curConn.getContent().equalsIgnoreCase(curWord) )
                {
                    result = true;
                }
            }

            if(result == false)
            {
                result = false;
                break;
            }
        }

        //if( this.getConnWordContent().indexOf(word) != -1 ) result = true;
        return result;
    }


    /**将一个句子分割为基本语义单元集合**/
    public void seperateSentenceToEDU()
    {
        String[] EDUs = seperateSingleSentenceToEDU(this.content);
        for(String curEDU : EDUs)
        {
            DSAEDU dsaEDU = new DSAEDU(curEDU, this.content);
            this.edus.add(dsaEDU);
        }
    }

    /**将一单独的句子拆分为基本语义单元，目前采用的是简单的逗号等基于规则的拆分方式。可以考虑使用短语结构分析来拆分。**/
    private String[] seperateSingleSentenceToEDU(String sentContent)
    {
        Matcher matcher   = Constants.Sentence_Element_Pattern.matcher(sentContent);
        String[] EDUs = Constants.Sentence_Element_Pattern.split(sentContent);

        //将句子结束符连接到相应的句子后，生成粗糙的分割结果
        if( EDUs.length > 0 )
        {
            int index = 0;
            while(index < EDUs.length)
            {
                if(matcher.find())
                {
                    EDUs[index] += matcher.group();
                }
                index++;
            }
        }
        return EDUs;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ArrayList<DSAConnective> getConWords() {
        return conWords;
    }

    public void setConWords(ArrayList<DSAConnective> conWords) {
        this.conWords = conWords;
    }

    public ArrayList<DSARelation> getRelations() {
        return relations;
    }

    public void setRelations(ArrayList<DSARelation> relations) {
        this.relations = relations;
    }

    public List<Term> getAnsjWordTerms() {
        return ansjWordTerms;
    }

    public void setAnsjWordTerms(List<Term> ansjWordTerms) {
        this.ansjWordTerms = ansjWordTerms;
    }

    public ArrayList<ParallelConnective> getParallelConnectives() {
        return parallelConnectives;
    }

    public void setParallelConnectives(ArrayList<ParallelConnective> parallelConnectives) {
        this.parallelConnectives = parallelConnectives;
    }

    public ArrayList<DSAEDU> getEdus() {
        return edus;
    }

    public void setEdus(ArrayList<DSAEDU> edus) {
        this.edus = edus;
    }
}
