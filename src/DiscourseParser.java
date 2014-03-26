import common.Constants;
import common.LibSVMTest;
import common.util;
import entity.*;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.DocumentException;
import resource.Resource;
import train.MLVectorItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 篇章分析主程序，主要是为了完成Discourse Parser程序的编写，采用自底向上进行编写处理。
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2013-12-10 09:02
 * Email: jhji@ir.hit.edu.cn
 */
public class DiscourseParser
{
    private String text;    //待分析文章

    private ArrayList<DSASentence> sentences;

    private LibSVMTest libsvmMode = null;

    public DiscourseParser() throws DocumentException, IOException
    {
        sentences = new ArrayList<DSASentence>();

        System.out.println("[--Info--]Loading Resource: word and relation dictionary");

        //1: 加载词典信息
        Resource.LoadExpConnectivesDict();

        //2: 以及关联词指示的关系信息
        Resource.LoadWordRelDict();

        //3: 加载标注好的训练数据
        Resource.LoadRawRecord();

        //4: 加载并列连词词典
        Resource.LoadParallelWordDict();

        //5: 加载连词识别svm模型
        libsvmMode = new LibSVMTest();
        libsvmMode.loadModel();
    }


    public void run(String line) throws  IOException
    {
        //1: 针对一句话识别句内关系
        DSASentence sentence = new DSASentence( util.removeAllBlank(line) );

        //2: 识别单个连词
        this.findConnWordWithML(sentence);

        //3: 识别并列连词：不仅...而且
        this.findParallelWord(sentence);

        //4: 识别Argument，目前采用的方法是简单的逗号分隔
        this.findArgumentInLine(sentence);

        //5: 根据获取到的信息，识别关系编号
        this.recognizeExpRelationType(sentence);

        //6: 识别隐式句间关系
        this.recognizeImpRelationType(sentence);
    }

    /**
     * 2: 识别单个连词加入到候选连词中
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence) throws IOException {
        //1：进行分词
        List<Term> words = ToAnalysis.parse( sentence.getContent().replaceAll(" ", "") );
        sentence.setAnsjWordTerms(words);

        int beginIndex = 0;

        ArrayList<MLVectorItem> candidateTerms = new ArrayList<MLVectorItem>();

        for(Term wordItem : words)
        {
            String wContent   = wordItem.getName().trim();
            MLVectorItem item = new MLVectorItem(wContent);

            //2: 过滤掉噪音词
            if( !Resource.ExpConnectivesDict.contains(wContent) ) continue;

            //3：获取词性特征
            String wNextPos = "w";
            String wPrevPos = "w";
            Term wNextTerm  = wordItem.getTo();
            Term wPrevTerm  = wordItem.getFrom();

            if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
            if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

            item.setPos( wordItem.getNatrue().natureStr );
            item.setPrevPos(wPrevPos);  item.setNextPos(wNextPos);

            //4：获取该词在句子中的的位置
            beginIndex = sentence.getContent().indexOf(wContent, beginIndex);
            item.setPositionInLine( beginIndex );

            //5: 设置标签，因为是预测，可以随意设置标签, 默认不是连词
            //item.setLabel( Constants.Labl_Not_ConnWord );

            //6：获取该词在连词词典中出现的次数,以及歧义性
            double occurTime = 0.0, ambiguity = 1.0;

            if( Resource.allWordsDict.containsKey(wContent) )
            {
                DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

                occurTime = wordDictItem.getExpNum();
                ambiguity = 1 - wordDictItem.getMostExpProbality();
            }

            item.setAmbiguity(ambiguity);
            item.setOccurInDict(occurTime);

            candidateTerms.add(item);
        }

        for(MLVectorItem item : candidateTerms)
        {
            double result = libsvmMode.predict(item);

            if( result > 0 )
            {
                DSAConnective connective = new DSAConnective( item.getContent() );
                connective.setPosTag( item.getPos() );
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine( item.getPositionInLine() );
            }
        }
    }


    /**
     * 3: 查找一段文字中的并列连词，比如：不仅...而且,
     * 目前的识别方法只是识别了一个句子中最为可能的并列连词。即按照出现次数的大小来判断
     * @param sentence
     */
    private void findParallelWord(DSASentence sentence)
    {
        int occurTimes      = -1;
        String sentContent  = sentence.getContent();
        ParallelConnective parallelConnective = null;

        for(Map.Entry<String, Integer> entry: Resource.ExpParallelWordDict.entrySet())
        {
            String parallelWord = entry.getKey();
            Integer numInDict = entry.getValue();

            //0: 判断词典中的一个并列连词是否出现在了该句子中
            if( util.isParallelWordInSentence(parallelWord, sentContent) )
            {
                //1: 清除单个连词的情况，防止重复认定
                String[] lists = parallelWord.split(";");
                int beginIndex1 = sentContent.indexOf(lists[0]);
                int beginIndex2 = sentContent.indexOf(lists[1]);

                Iterator<DSAConnective> ite = sentence.getConWords().iterator();

                while( ite.hasNext() )
                {
                    DSAConnective curConn = ite.next();
                    if( curConn.getContent().equalsIgnoreCase(lists[0]) )
                    {
                        if( Math.abs(curConn.getPositionInLine() - beginIndex1) < Constants.EPSION )
                        {
                            ite.remove();
                        }
                    }
                    else if( curConn.getContent().equalsIgnoreCase(lists[1]) )
                    {
                        if( Math.abs(curConn.getPositionInLine() - beginIndex2) < Constants.EPSION )
                        {
                            ite.remove();
                        }
                    }
                }

                //2: 修正候选并列连词列表
                if(  numInDict > occurTimes )
                {
                    if( parallelConnective == null ) parallelConnective = new ParallelConnective();
                    occurTimes = numInDict;
                    parallelConnective.setContent(parallelWord);
                    parallelConnective.setBeginIndex1(beginIndex1);
                    parallelConnective.setBeginIndex2(beginIndex2);
                }
            }
        }

        if( parallelConnective != null )
            sentence.getParallelConnectives().add(parallelConnective);
    }


    /**
     * 识别一个关系涉及到的Argument，可以考虑的是使用argument的标注信息来进行训练，
     * 比如但是的argument1一般是位于但是后面，而argument2一般位于前面。
     * 隐式的比较好判断，直接两两配对
     * 目前认为连词前后的都属于argument
     * @param sentence
     */
    private void findArgumentInLine(DSASentence sentence)
    {
        //0: 将句子拆分为基本语义单元
        sentence.seperateSentenceToEDU();

        //1：确定显式连词的argument，主要困难在于，是在前面还是在后面。
        ArrayList<DSAConnective> singleConnWords = sentence.getConWords();

        for(DSAConnective curWord:singleConnWords)
        {
            String word   = curWord.getContent();
            int posInLine = (int) curWord.getPositionInLine();

            DSAArgument arg1 = null, arg2 = null;

            int index  = 0;
            boolean isSoloWord = false;
            DSAEDU curEDU = null;

            //确定arg1：即包含了连词的argument
            for( index = 0; index < sentence.getEdus().size(); index++ )
            {
                curEDU = sentence.getEdus().get(index);

                if( curEDU.getContent().indexOf(word)!= -1 )
                {
                    if( posInLine >= curEDU.getBeginIndex() && posInLine < curEDU.getEndIndex() )
                    {
                        //需要考虑连词作为一个独立的EDU时候的情况
                        if( curEDU.getContent().trim().length() - word.length() <= 1 )
                        {
                            isSoloWord = true;
                            curEDU = sentence.getEdus().get(index == 0 ? 1: index - 1);
                        }

                        arg1 = new DSAArgument( curEDU.getContent(), sentence.getContent() );
                        break;
                    }
                }
            }

            //确定arg2：即连词的另外一个argument,,以及判断EDU位于句首的情况
            if( arg1 != null )
            {
                if( isSoloWord )
                    curEDU = sentence.getEdus().get( index == 0 ? 2: index - 1);
                else
                    curEDU = sentence.getEdus().get( index ==0 ? 1 : index - 1 );
                arg2 = new DSAArgument( curEDU.getContent(), sentence.getContent() );
            }

            //生成对应的句间关系,句间关系的编号还需要下一步进行判定
            DSARelation relation = new DSARelation();
            relation.setArg1(arg1);
            relation.setArg2(arg2);
            relation.setDsaConnective(curWord);
            relation.setRelType(Constants.ExpRelationType);

            sentence.getRelations().add(relation);
        }
    }


    /***识别具体的显式关系类型**/
    private void recognizeExpRelationType(DSASentence sentence)
    {
        for( DSARelation candiatateRel : sentence.getRelations() )
        {
            if( candiatateRel.getDsaConnective() == null ) continue;

            String connWord = candiatateRel.getDsaConnective().getContent();
            String relNO    = Resource.connectiveRelationDict.get(connWord);

            if( relNO == null ) relNO = Constants.DefaultRelNO;

            candiatateRel.setRelNO( relNO );
        }
    }


    /***识别可能存在的隐式句间关系**/
    private void recognizeImpRelationType(DSASentence sentence)
    {
        //在基本的EDU的基础上开始进行两两匹配

    }


    /**
     * 针对一篇文章进行分析。
     * @param fPath: 原始语料的路径
     */
    private void parseRawFile(String fPath) throws Exception
    {
        //0: 将文章读取并得到文章中的句子集合
        String fileContent = util.readFileToString(fPath);
        ArrayList<String> sentences = util.filtStringToSentence(fileContent);


        //1：首先是句内关系的判断
        for( String sentence : sentences )
        {
            DSASentence dsaSentence = new DSASentence( sentence );
            //this.recognizeRelationInSentence( dsaSentence.getContent() );
            //this.findConnectiveWithRule(dsaSentence);

        }

    }


    private ArrayList<DSAConnective> findConnective(String line) throws Exception
    {
        ArrayList<String> sentences = util.filtStringToSentence(line);

        DSASentence dsaSentence = new DSASentence(line);

        //this.findConnectiveWithRule(dsaSentence);

        ArrayList<DSAConnective> conWords = dsaSentence.getConWords();

        return conWords;
    }


    public static void main(String[] args) throws Exception
    {
        DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test = "我 听到 过 很多 解释, 但是 我 觉得 我 从没有 看到 过 清晰 的 分析 。";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        discourseParser.run(test);
    }
}

