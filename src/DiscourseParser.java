import common.Constants;
import entity.recognize.*;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import net.didion.jwnl.data.IndexWord;
import opennlp.maxent.DataStream;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import org.ansj.splitWord.analysis.ToAnalysis;
import train.svm.wordRecSVM;
import train.svm.relRecSVM;
import common.util;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;
import syntax.PhraseParser;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.dom4j.DocumentException;
import train.relation.ImpRelFeatureExtract;
import train.relation.RelVectorItem;
import train.word.ConnVectorItem;
import resource.Resource;

import java.io.*;
import java.util.*;
import  train.maxent.Predict;

/**
 * 篇章分析主程序，主要是为了完成Discourse Parser程序的编写，采用自底向上进行编写处理。
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2013-12-10 09:02
 * Email: jhji@ir.hit.edu.cn
 */
public class DiscourseParser
{
    private wordRecSVM connSvmMode;      //连词识别模型
    private relRecSVM relationSVMMode;   //隐式关系识别模型

    private PhraseParser phraseParser;  //短语结构分析
    private ImpRelFeatureExtract impRelFeatureExtract;

    public DiscourseParser() throws DocumentException, IOException
    {
        //1: 加载词典信息
        Resource.LoadExpConnectivesDict();

        //2: 以及关联词指示的关系信息
        Resource.LoadWordRelDict();

        //3: 加载标注好的训练数据
        //Resource.LoadRawRecord();

        //5: 加载连词Arg位置统计数据
        Resource.LoadConnectiveArgs();
        Resource.LoadConnInP2AndP3();

        //6: 加载连词识别svm模型
        connSvmMode = new wordRecSVM();
        connSvmMode.loadModel();

        //7: 加载短语结构分析
        this.phraseParser = new PhraseParser();

        //8: 加载隐式关系识别模型
        relationSVMMode = new relRecSVM();
        relationSVMMode.loadModel();

        impRelFeatureExtract = new ImpRelFeatureExtract();

        //9: 加载关系列表，里面包含了关系编号和关系名称的对应。
        Resource.LoadSenseList();
    }

    /**供测试使用*/
    public DiscourseParser(boolean debug)
    {

    }


    /**
     * 针对一篇文章进行分析。首先需要将一篇文章进行分句，然后对每个句子进行处理。
     * 输入的是一篇文章的内容，返回的是封装好的DSA对象。
     * fileContent: 输入的文章，可以是一个句子或者是多个句子。
     * needSegment：是否进行分词的标志
     */
    public DSAParagraph parseRawFile(String fileContent, boolean needSegment) throws Exception
    {
        //0: 将文章读取并进行分割得到文章中的句子集合
        DSAParagraph      paragraph = new DSAParagraph(fileContent);
        ArrayList<String> sentences = util.filtStringToSentence(fileContent);

        //1：句内关系的判断:包括了一个句子内部的显式和隐式关系的过程
        for( int index = 0; index < sentences.size(); index++ )
        {
            DSASentence dsaSentence = new DSASentence( sentences.get(index) );
            dsaSentence.setId(index);

            try{
                this.processSentence(dsaSentence, needSegment);
            }
            catch (NullPointerException e){
                System.out.println(e.getMessage());
            }

            paragraph.sentences.add(dsaSentence);
        }

        //2：句间关系的识别:跨句显式关系识别,相邻的两句是一个候选
        for(int index = 0; index < paragraph.sentences.size() - 1; index++)
        {
            DSASentence curSentence  = paragraph.sentences.get(index);
            DSASentence nextSentence = paragraph.sentences.get(index + 1);

            //a: 判断是否是句间关系中的显式关系
            boolean hasExpRel = getCrossExpRelInPara(paragraph, curSentence, nextSentence);

            if( hasExpRel ) continue;

            //b: 获取两个句子之间可能存在的隐式关系
            int  senseType   = getCrossImpInPara(paragraph, curSentence, nextSentence);
        }

        return paragraph;
    }

    /**
     * 处理一个封装好的DSASentence，是旧版本run方法的重写。
     */
    public void processSentence(DSASentence sentence, boolean needSegment) throws IOException
    {
        int id = 0;
        boolean argPosition = true;     //默认将论元位置设置为SS

        //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
        this.preProcess(sentence, needSegment);

        //2: 识别单个连词和识别并列连词：不仅...而且
        this.findConnWordWithML(sentence);

//         if(!sentence.getConWords().isEmpty())      //Find Connective~
//         {

        argPosition = this.markConnAsInterOrCross(sentence,id);
        //        this.markConnAsInterOrCross(sentence);        原有方法,based on rules
        this.findParallelWord(sentence);

        //4: 将句子按照短语结构拆分成基本EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1：避免出现句子结构非法的Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return; }

        //论元位置为SS(分句关系)
        for(DSAConnective curWord:sentence.getConWords() )
        {
             Boolean isInterConnetive = curWord.getInterConnective();
             if(isInterConnetive)
             {
                 argLaberler(sentence);      //抽取特征，将句法树内部节点进行分类：Arg1 Node、Arg2 Node、None
             }
        }

        //5: 确定每个连词所涉及到的两个EDU来产生候选显式关系
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);
        //6: 根据获取到的信息，识别显式句间关系
        this.recognizeExpRelationType(sentence);

//         }
//         else
//         {
             //7: 识别可能存在的隐式句间关系
             this.getCandiateImpRelation(sentence);
             this.recognizeImpRelationType(sentence);
//         }

    }

    /**
     * 处理一个句子内部的句内关系。输入为字符串和是否需要分词.
     * 注意该方法已经被ProcessSentence替换。因为涉及到了sentID的问题。
     ***/
    public DSASentence run(String line, boolean needSegment) throws  IOException
    {
        DSASentence sentence = new DSASentence( line );
        sentence.setId(0);

        //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
        this.preProcess(sentence, needSegment);

        //2: 识别单个连词
        this.findConnWordWithML(sentence);
//        this.markConnAsInterOrCross(sentence);        //训练

        //3: 识别并列连词：不仅...而且
        this.findParallelWord(sentence);

        //4: 将句子按照短语结构拆分成基本EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1：避免出现句子结构非法的Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return sentence; }

        //5: 确定每个连词所涉及到的两个EDU来产生候选显式关系
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);

        //6: 根据获取到的信息，识别显式句间关系
        this.recognizeExpRelationType(sentence);

        //7: 识别可能存在的隐式句间关系
        this.recognizeImpRelationType(sentence);

        return sentence;
    }



    //-------------------------------0: 对句子进行底层处理--------------------------

    /**对输入的句子进行底层的NLP处理，主要是包括了分词、词性标注等。**/
    private void preProcess(DSASentence sentence, boolean needSegment)
    {
        List<Term> words;
        int beginIndex = 0;
        String segmentResult = "";
        ArrayList<ConnVectorItem> candidateTerms = new ArrayList<ConnVectorItem>();

        if( !needSegment )
        {
            //不需要分词, 已经分好词的结果，那么我们需要词性标注的结果
            List<String> lists = Arrays.asList( sentence.getContent().split(" ") );

            words = NatureRecognition.recognition(lists, 0) ;

            for( int index = 0; index < words.size(); index++ )
            {
                Term curTerm = words.get(index);

                if(index > 0) {
                    curTerm.setFrom( words.get(index - 1) );
                }
                if(index < words.size() - 1){
                    curTerm.setTo( words.get(index+1) );
                }
            }
        }
        else{

            words = ToAnalysis.parse(util.removeAllBlank(sentence.getContent()));
        }

        //a: 针对句子中的每个词进行抽取特征


        sentence.setAnsjWordTerms(words);
        sentence.setContent( util.removeAllBlank(sentence.getContent()) );

        for( Term wordItem : sentence.getAnsjWordTerms() )
        {
            String wContent     = wordItem.getName().trim();
            ConnVectorItem item = new ConnVectorItem(wContent);

            segmentResult += wContent + " ";
        }

        //设置分词后的结果
        sentence.setSegContent( segmentResult.trim() );
    }

    //-------------------------------1: Connective Recognize----------------------

    /**
     * 2: 识别单个连词加入到候选连词中
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence) throws IOException
    {
        int beginIndex = 0;
        String segmentResult = "";

        ArrayList<ConnVectorItem> candidateTerms = new ArrayList<ConnVectorItem>();

        //a: 针对句子中的每个词进行抽取特征
        for( Term wordItem : sentence.getAnsjWordTerms() )
        {
            String wContent     = wordItem.getName().trim();
            ConnVectorItem item = new ConnVectorItem(wContent);

            segmentResult += wContent + " ";

            //2: 过滤掉噪音词
            if( !Resource.allWordsDict.containsKey(wContent) ) continue;

            //3：获取词性特征
            String wNextPos = "w", wPrevPos = "w";
            Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

            if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
            if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

            item.setPos( wordItem.getNatrue().natureStr );
            item.setPrevPos(wPrevPos);  item.setNextPos(wNextPos);

            //4：获取该词在句子中的的位置
            beginIndex = sentence.getContent().indexOf(wContent, beginIndex);
            item.setPositionInLine( beginIndex );

            //6：获取该词在连词词典中出现的次数,以及歧义性
            double occurTime = 0.0, ambiguity = 1.0;

            if( Resource.allWordsDict.containsKey(wContent) )
            {
                DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);
                occurTime = wordDictItem.getExpNum();
                ambiguity = wordDictItem.getMostExpProbality();
            }

            item.setAmbiguity(ambiguity);
            item.setOccurInDict(occurTime);

            //5: 设置标签，因为是预测，可以随意设置标签, 默认不是连词
            if(occurTime < 3) item.setLabel( Constants.Labl_Not_ConnWord );
            else item.setLabel( Constants.Labl_is_ConnWord );

            //设置连词出现次数以及不作为连词出现次数
            Integer connNum    = Resource.allWordsDict.get(wContent).getExpNum();
            Integer notConnNum = Resource.NotAsDiscourseWordDict.get(wContent);

            if( connNum == null )    connNum = 0;
            if( notConnNum == null ) notConnNum = 0;

            item.setConnNum(connNum);
            item.setNotConnNum(notConnNum);

            candidateTerms.add(item);
        }

        ArrayList<String> features = new ArrayList<String>();

        //b: 使用模型判断每个候选的词是否是连词
        for( ConnVectorItem item : candidateTerms )
        {
//            int label = connSvmMode.predict(item);
            String strFeatures =  item.getContent() + " " + item.getLength() + " " + item.getPositionInLine() + " " +
                    item.getAmbiguity() + " " + item.getOccurInDict() + " " + item.getPos() + " " +
                    item.getPrevPos() + " " + item.getNextPos();
            features.add(strFeatures);
            util.writeLinesToFile("isConnective.test.txt",features);

            String result = ClassifyViaMaximumEntrop("isConnectiveModel.txt","isConnective.test.txt");
//            System.out.println("The result is " + result);
            int label  = Integer.valueOf(result);

            if( label > 0 || item.getOccurInDict() > 100 )
            {
                DSAConnective connective = new DSAConnective( item.getContent(), sentence.getId() );
                String strWords[] = segmentResult.split(" ");

                ArrayList<String> stringArrayList = new ArrayList<String>();
                for(int i = 0; i < strWords.length; i++){
                    stringArrayList.add(strWords[i]);
                }
                List<Term>  posOfWords  = util.getSegmentedSentencePosTag(segmentResult);

                int indexOfConnective = stringArrayList.indexOf(item.getContent());
                String prev1 = "",prev2 = "";
                try {
                    prev1 = stringArrayList.get(indexOfConnective - 1);
                    prev2 = stringArrayList.get(indexOfConnective - 2);
                }
                catch (IndexOutOfBoundsException e){
                    System.out.println(e.toString());
                }

                String prev2Pos  = "";
//                prev2Pos = sentence.getAnsjWordTerms().get(indexOfConnective - 1).getRealName();
                Term prev2Term;
                try{
                    prev2Term = sentence.getAnsjWordTerms().get(indexOfConnective - 3).getTo();
                    prev2Pos = prev2Term.getNatrue().natureStr;
                    item.setPrev2Pos(prev2Pos);
                }
                catch (IndexOutOfBoundsException e){
                    System.out.println(e.toString());
                }

                connective.setPrev1(prev1);
                connective.setPrev2(prev2);
                connective.setPosTag(item.getPos());
                connective.setPrevPosTag(item.getPrevPos());
                connective.setPrev2PosTag(item.getPrev2Pos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine(item.getPositionInLine());

                sentence.getConWords().add(connective);
            }
        }
    }

    /**
     * 为了区分一个连词是句间连词还是句内连词，在识别了连词之后，需要首先判断连词是连接句内关系还是连接句间关系。
     * DSAConnective里面有一个属性：isInterConnective
     **/
    private void markConnAsInterOrCross(DSASentence sentence)
    {
        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            //1: 获取该连词在p2和p3中分别出现的次数
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2：获取该连词连接Arg的类型
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: 连词位于句子中间的位置, 是否位于句首
            int position = (int) curWord.getPositionInLine();

            //4：是否位于第一个短句内
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("，") || temp.contains(",") ) inSentenceHead = false;

            if( numInP2 > numInP3 && inSentenceHead ) curWord.setInterConnective(false);
        }
    }




    /**
     * 为了区分一个连词是句间连词还是句内连词，在识别了连词之后，需要首先判断连词是连接句内关系还是连接句间关系。
     * DSAConnective里面有一个属性：isInterConnective
     * @param sentence
     * @return
     * @throws IOException
     */
    private boolean markConnAsInterOrCross(DSASentence sentence,int id) throws IOException {
        boolean argPosition = true;
        ArrayList results = new ArrayList();

        String wTestFileName = "arg_pos.test.txt" ,wModelFileName = "arg_posModel.txt";     //模型文件名，测试文件名

//      a. 抽取特征
        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            //1: 获取该连词在p2和p3中分别出现的次数
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2：获取该连词连接Arg的类型
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: 连词位于句子中间的位置, 是否位于句首
            int position = (int) curWord.getPositionInLine();

            //4：是否位于第一个短句内
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("，") || temp.contains(",") ) inSentenceHead = false;

//            if( numInP2 > numInP3 || inSentenceHead ) curWord.setInterConnective(false);

            //5：抽取特征，用于论元位置分类
//            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrevPosTag() + " " + curWord.getPositionInLine();
            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrev1() + " " + curWord.getPrevPosTag() + " "
                    +curWord.getPrev1() + curWord.getContent() + " " +curWord.getPosTag()+curWord.getPrevPosTag() +" "+ curWord.getPrev2()
                    +" "+ curWord.getPrev2PosTag() + " " +curWord.getPosTag() + curWord.getPrev2PosTag() + " "
                    + curWord.getContent() + curWord.getPrev2() +" " + curWord.getPositionInLine()
                    + " " + numInP2 + " " + numInP3 + " " + argConnArg + " " + connArgArg;
            results.add(str);

        }

        util.writeLinesToFile(wTestFileName,results);

        //b. 使用最大熵模型进行分类， result为分类结果
        String result =  ClassifyViaMaximumEntrop(wModelFileName,wTestFileName);

        if(result.equals("PS")){
            argPosition = false;
        }
        else{
            argPosition = true;
        }

        return  argPosition;
    }

    /**
     *论元位置分类，判断关联词所指示的关系是句内关系还是句间关系
     * @param sentence
     * @param id
     * @param train
     * @param pNumber
     * @param wConnective  Connective in Corpus
     * @return true 代表
     * @throws IOException
     */
    private boolean markConnAsInterOrCross(DSASentence sentence,int id,Boolean train,int pNumber,String wConnective) throws IOException {
        boolean argPosition = true;
        ArrayList results = new ArrayList();
        ArrayList resultsForTest = new ArrayList();
        int nDividing = pNumber==2?1524:4409;       //训练数据和测试数据的分界线

        for(DSAConnective curWord:sentence.getConWords() )
        {
            String wContent = curWord.getContent();

            if(!wContent.equalsIgnoreCase(wConnective)) continue;

            //1: 获取该连词在p2和p3中分别出现的次数
            int numInP2 = 0, numInP3 = 0;
            if( Resource.ConnInP2AndP3.containsKey(wContent) )
            {
                numInP2 = Resource.ConnInP2AndP3.get(wContent)[0];
                numInP3 = Resource.ConnInP2AndP3.get(wContent)[1];
            }

            //2：获取该连词连接Arg的类型
            int argConnArg = 0, connArgArg = 0;
            if( Resource.ConnectiveArgNum.containsKey(wContent) )
            {
                argConnArg = Resource.ConnectiveArgNum.get(wContent)[0];
                connArgArg = Resource.ConnectiveArgNum.get(wContent)[1];
            }

            //3: 连词位于句子中间的位置, 是否位于句首
            int position = (int) curWord.getPositionInLine();

            //4：是否位于第一个短句内
            boolean inSentenceHead = true;
            String temp = sentence.getContent().substring(0, position);
            if( temp.contains("，") || temp.contains(",") ) inSentenceHead = false;

            if( numInP2 > numInP3 || inSentenceHead ) curWord.setInterConnective(false);

            //5：抽取特征，用于论元位置分类

            String str = curWord.getContent() + " " + curWord.getPosTag() + " " + curWord.getPrev1() + " " + curWord.getPrevPosTag() + " "
                    +curWord.getPrev1() + curWord.getContent() + " " +curWord.getPosTag()+curWord.getPrevPosTag() +" "+ curWord.getPrev2()
                    +" "+ curWord.getPrev2PosTag() + " " +curWord.getPosTag() + curWord.getPrev2PosTag() + " "
                    + curWord.getContent() + curWord.getPrev2() +" " + curWord.getPositionInLine()
                    + " " + numInP2 + " " + numInP3 + " " + argConnArg + " " + connArgArg;
//                    +" " + wType;

            if(id < nDividing)
            {
                //用于训练
                if(3 == pNumber)        //SS
                {
                    argPosition = true;
                    str += " SS";
                }
                else            //PS
                {
                    argPosition = false;
                    str += " PS";
                }
                results.add(str);
                util.appendMethodB("arg_pos.txt",results);
            }

            else
            {
                //用于测试
                String string;
                if(3 == pNumber)        //SS
                {
                    argPosition = true;
                    string = "SS";

                }
                else            //PS
                {
                    argPosition = false;
                    string = "PS";
                }
                results.add(str);
                util.appendMethodB("arg_pos.test",results);
                resultsForTest.add(string);
                util.appendMethodB("arg_pos_result.txt",resultsForTest);
            }
        }

        return  argPosition;
    }

    /**
     * 3: 查找一段文字中的复合关联词，比如：不仅...而且
     * 目前的识别方法只是识别了一个句子中最为可能的并列连词。即按照出现次数的大小来判断
     * @param sentence
     */
    private void findParallelWord(DSASentence sentence)
    {
        int occurTimes      = -1;
        String sentContent  = sentence.getSegContent();

        DSAConnective parallelConnective = null;

        for(Map.Entry<String, DSAWordDictItem> entry:Resource.allWordsDict.entrySet())
        {
            if( !entry.getValue().isParallelWord() ) continue;

            String parallelWord = entry.getKey();
            Integer numInDict   = entry.getValue().getExpNum();

            //if( numInDict < 2 ) continue;

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

                //2: 修正候选并列连词列表. 只获取出现次数最大的并列连词
                if(  numInDict > occurTimes )
                {
                    if( parallelConnective == null )
                        parallelConnective = new DSAConnective(parallelWord, sentence.getId());

                    occurTimes = numInDict;
                    parallelConnective.setContent(parallelWord);
                    parallelConnective.setBeginIndex1(beginIndex1);
                    parallelConnective.setBeginIndex2(beginIndex2);
                    parallelConnective.setParallelWord(true);
                }
            }
        }

        if( parallelConnective != null )
            sentence.getParallelConnectives().add(parallelConnective);
    }

    //-------------------------------2: EDU segment-----------------------------

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
            DSAInterRelation relation = new DSAInterRelation();
            relation.setArg1Content(arg1.getContent());
            relation.setArg2Content(arg2.getContent());
            relation.setDsaConnective(curWord);
            relation.setRelType(Constants.EXPLICIT);
            sentence.getRelations().add(relation);
        }
    }


    /**
     * 根据短语结构分析器来获取对应的短语结构。并以此来获取基本EDU。注意输入的的是预处理(分好词)之后的DSASentence
     * @param sentence
     */
    public boolean findArgumentWithPhraseParser(DSASentence sentence) throws IOException {
        Tree phraseResult;
        try{
            phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );
        }
        catch (OutOfMemoryError e){
            return false;
        }

        //1: 递归的构建基本EDU，主要是获取各个部分的EDU的树根
        DSAEDU rootEDU = new DSAEDU(sentence.getContent(), sentence.getContent());
        rootEDU.setRoot(phraseResult.firstChild());

        boolean result = getEDUsFromRoot(rootEDU);

        if( result == false ) return false; //为false表示该句子的短语结构分析发现该句子不是合法的句子。

        //2: 清理和泛化单链EDU，主要是将单链EDU向上进行泛化, 将多余的EDU清理
        this.clearDuplictEDUs(rootEDU);

        //3: 提取各个EDU信息进行封装，主要是提取该EDU的内容等信息
        this.getEDUContent(rootEDU);

        //保存EDU信息
        sentence.setRootEDU(rootEDU);

        //4: 查找连词所连接的两个EDU
        //this.matchConnectiveAndEDU(sentence);

        return  true;
    }

    /**
     * 论元特征抽取，用于最大熵分类句法树中的节点：Arg1 node、Arg2 node 、None
     *抽取特征：连词的字符串、连词的句法分类（单个连词、并列连词）、连词左兄弟的个数、连词右兄弟的个数、路径P以及连词C的左兄弟的个数是否大于1，节点C的相对位置(左、中、右)
     * @param sentence
     * @throws IOException
     */
    public void argLaberler(DSASentence sentence) throws IOException {

//        将句子按照短语结构拆分成基本EDU

        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();
        String arg1Content = "",arg2Content = "";
        boolean  result = false;

//      标记句法树中的每一个节点

        Tree rootTree = rootEDU.getRoot();


        for(DSAConnective connective:conns)     //访问每个关联词
        {

            for( Tree child : rootTree.children() )       //访问句法树的每个孩子结点
            {
                //不考虑单链泛化的孩子节点：类似于：他->Pron->N之类的
                if( child.depth() == child.size() - 1 ) continue;

                String strChild = getRootContent(child);

                DSAEDU curEDU = new DSAEDU();
                curEDU.setRoot(child);
                curEDU.setParentEDU(rootEDU);
                curEDU.setDepth( rootEDU.getDepth() + 1 );

                boolean temp = getEDUsFromRoot( curEDU );

                if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                result = result || temp;

                //nClass为分类结果
                int nClass = InnerNodeClassification(child,sentence);

                System.out.println(strChild + ":" + nClass);

                if(1 == nClass){
                    connective.setArg1Content(strChild);
                }
                else if(2 == nClass){
                    connective.setArg2Content(strChild);
                }
                else continue;
            }
        }

    }

    /**
     * 句法树内部结点的最大熵分类模型 训练过程
     * @param tree,senseRecord
     * @return
     * @throws IOException
     */
    private int InnerNodeClassificationTrain(Tree tree,SenseRecord senseRecord,DSASentence sentence,int index,String wConnective) throws IOException
    {
        String text = "",arg1Content = "",arg2Content = "";
;       int type = 0;

        text        = senseRecord.getText();
        arg1Content = senseRecord.getArg1();
        arg2Content = senseRecord.getArg2();

//        DSASentence sentence = new DSASentence(text);
//        sentence.setSegContent(text);
//        sentence.setId(index++);
//
//        this.preProcess(sentence,false);
//        this.findConnWordWithML(sentence);
//        this.markConnAsInterOrCross(sentence,index);
//        this.findParallelWord(sentence);
//        this.findArgumentWithPhraseParser(sentence);

        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList featureList = new ArrayList();
        ArrayList classResult = new ArrayList();
        int leftSiblings = 0 ,rightSiblings = 0;
        ArrayList<DSAConnective> conns = sentence.getConWords();

        for( DSAConnective curWord : conns )
        {
//            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            ConnVectorItem conn = new ConnVectorItem(wContent);

            if(!wContent.equalsIgnoreCase(wConnective)) continue;

            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
            int numChildren;
            DSAEDU parrentEDU;

//            Tree parrentTree = connEDU.getRoot().parent();
//            Tree parrentTree = parrentEDU.getRoot();
//            connEDU.getBeginIndex();
//            int numChildren = parrentTree.numChildren();

            try {
                parrentEDU = connEDU.getParentEDU();
                numChildren = parrentEDU.getRoot().numChildren();
            }
            catch(NullPointerException e){
                continue;
            }


            for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
                if(!dsaEDU.equals(connEDU)) {
                    leftSiblings += 1;
                }
                else break;
            }

            rightSiblings = numChildren - leftSiblings - 1;

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

            List<Tree> listPath = tree.pathNodeToNode(connEDU.getRoot(),tree);

            int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;

            String results = "";
//           results += getRootContent(tree);
//                results += getRootContent(dsaedu.getRoot()) + " ";
//                results = curWord.getContent() + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings + " "  + listPath.toString() + " " +  leftSiblingsGreaterThanOne ;
//          results = curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ; // 4 dimention
            results = curWord.getContent() + " " + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ;


//            DSASentence dsaSentence1 = new DSASentence(arg1Content);
//            DSASentence dsaSentence2 = new DSASentence(arg2Content);
            String strType ;

            if(arg1Content.contains(getRootContent(tree))){
                strType = "arg1";
                type = 1;
            }
            else if(arg2Content.contains(getRootContent(tree))){
                strType = "arg2";
                type = 2;
            }
            else {
                strType = "None";
                type = 0;
            }

            if(index < 4409)
            {
                results += " " + strType; /* + " " + dsaedu.getRoot().toString() */
                featureList.add(results);
                util.appendMethodB("./arg_ext.txt", featureList);

            }
            else
            {
                featureList.add(results);
                util.appendMethodB("./arg_ext.test", featureList);
                classResult.add(strType);
                util.appendMethodB("./arg_extResult.txt",classResult);
            }

        }

        return type;
    }


    /**
     *句法树内部节点分类:1）抽取特征；2）最大熵分类
     * @param tree,sentence
     * @throws IOException
     */
    private int InnerNodeClassification(Tree tree, DSASentence sentence) throws IOException
    {

        int nType = 0;
//      1. 特征抽取
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList featureList = new ArrayList();

        int leftSiblings = 0 ,rightSiblings = 0;
        ArrayList<DSAConnective> conns = sentence.getConWords();

        String wModelFileName = "arg_extModel.txt" ,wTestFileName = "arg_ext.test.txt";       //模型文件名、测试文件名

//        int times = 0;
        for( DSAConnective curWord : conns )
        {

//            System.out.println("Times are " + times);
//            times ++;

            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            ConnVectorItem conn = new ConnVectorItem(wContent);

            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
            DSAEDU parrentEDU;

            connEDU.getBeginIndex();

            int numChildren;
            try {
                parrentEDU = connEDU.getParentEDU();
                numChildren = parrentEDU.getRoot().numChildren();
            }
            catch(NullPointerException e){
                continue;
            }

            for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
                if(!dsaEDU.equals(connEDU)) {
                    leftSiblings += 1;
                }
                else break;
            }

            rightSiblings = numChildren - leftSiblings - 1;

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

//            List<Tree> listPath = dsaedu.getRoot().pathNodeToNode(connEDU.getRoot(),dsaedu.getRoot());
            List<Tree> listPath = tree.pathNodeToNode(connEDU.getRoot(),tree);

            int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;

            String results = "";
//            results = curWord.getContent() + curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings + " "  + listPath.toString() + " " +  leftSiblingsGreaterThanOne ;
            results = curWord.getContent() + " " +  curWord.getPosTag() + " " + leftSiblings + " " + rightSiblings +  " " +  leftSiblingsGreaterThanOne ;
            featureList.add(results);
        }

        util.writeLinesToFile(wTestFileName,featureList);

        String wResult  = ClassifyViaMaximumEntrop(wModelFileName,wTestFileName);    //使用最大熵模型进行分类

        String wType = "";
        if(wResult.equals("arg1"))     {
            wType = "arg1";
            nType = 1;
        }
        else if(wResult.equals("arg2")){
            wType = "arg2";
            nType = 2;
        }
        else {
            wType = "None";
            nType = 0;
        }

        return nType;
    }

//   private String getConnectFeatures()
//   {
//       String results = "";
//       int leftSiblings = 0 ,rightSiblings = 0;
//       ArrayList<DSAConnective> conns = sentence.getConWords();
//
//       for( DSAConnective curWord : conns )
//       {
//           if( !curWord.getInterConnective() ) continue;
//
//           String wContent = curWord.getContent();
//           ConnVectorItem conn = new ConnVectorItem(wContent);
//
//           DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);
//           DSAEDU parrentEDU = connEDU.getParentEDU();
//
//           connEDU.getBeginIndex();
//           int numChildren = parrentEDU.getChildrenEDUS().size();
//
//           for(DSAEDU dsaEDU :parrentEDU.getChildrenEDUS() ){
//               if(!dsaEDU.equals(connEDU)) {
//                   leftSiblings += 1;
//               }
//               else break;
//           }
//
//           rightSiblings = numChildren - leftSiblings - 1;
//
//           if( connEDU == null )
//           {
//               System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
//               System.out.println(sentence.getContent());
//               curWord.setIsConnective(false);
//               continue;
//           }
//
//           int leftSiblingsGreaterThanOne = (leftSiblings>1)?1:0;
//
//           results = curWord.getContent() + " " + leftSiblings + " " + rightSiblings + " " +  leftSiblingsGreaterThanOne;
//       }
//
//        return  results;
//   }





    /**
     * 从树根开始，递归的构建EDU树，每层树根只能包含直接孩子节点的EDU，一直递归下去。
     * 此次获取到的只是简单的每个EDU在树形结构中的Root，后续需要再处理。
     **/
    private boolean getEDUsFromRoot(DSAEDU rootEDU ) throws IOException {
        boolean  result = false;

        //递归处理各个孩子节点
        for( Tree child : rootEDU.getRoot().children() )
        {
            //不考虑单链泛化的孩子节点：类似于：他->Pron->N之类的
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU );

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;

            DSASentence sentence;
            try {
                sentence = new DSASentence(curEDU.getRoot().toString());
            }
            catch (NullPointerException e){
                continue;
            }

        }
        //判断当前节点是否构成了一个最基本的EDU,递归终止条件
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("vp") )
        {
            if( rootEDU.getRoot().children().length > 1 ) result = true;
        }
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("np") )
        {
            result = false;
        }

        return result;
    }

    /**
     * 获取EDU，用于训练分类模型
     * @param rootEDU
     * @param senseRecord
     * @return
     * @throws IOException
     */
    private boolean getEDUsFromRoot(DSAEDU rootEDU,SenseRecord senseRecord) throws IOException {
        boolean  result = false;

        //递归处理各个孩子节点
        for( Tree child : rootEDU.getRoot().children() )
        {
            //不考虑单链泛化的孩子节点：类似于：他->Pron->N之类的
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU ,senseRecord);

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;
        }

        //判断当前节点是否构成了一个最基本的EDU,递归终止条件
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("vp") )
        {
            if( rootEDU.getRoot().children().length > 1 ) result = true;
        }
        if( rootEDU.getRoot().nodeString().equalsIgnoreCase("np") )
        {
            result = false;
        }

        return result;
    }



    /**
     * 对基本EDU进行整理，此步骤主要是为了清除多余的EDU，主要是将单链进行合并。对只有一个EDU的root进行向上泛化
     */
    private void clearDuplictEDUs(DSAEDU rootEDU)
    {
        /**
        //对孩子节点进行edu整理
        for(DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            this.clearDuplictEDUs( childEDU );
        }

       //如果该节点只有一个孩子EDU节点，那么可以将该孩子节点EDU向上泛化，并取消该孩子节点的EDU属性
        if( rootEDU.getChildrenEDUS().size() == 1 )
        {
            DSAEDU childEDU = rootEDU.getChildrenEDUS().get(0);
            ArrayList<DSAEDU> temp = null;

            if( childEDU.getChildrenEDUS().size() > 1 ) temp = childEDU.getChildrenEDUS();

            rootEDU.getChildrenEDUS().clear();

            if( temp != null )
            {
                rootEDU.setChildrenEDUS(temp);
                for(DSAEDU curEDU:temp) curEDU.setParentEDU(rootEDU);
            }
        }
         **/

        for(DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            this.clearDuplictEDUs(childEDU);
        }

        //只对叶子节点的EDU进行清理
        if(rootEDU.getChildrenEDUS().size() == 1)
        {
            if(rootEDU.getChildrenEDUS().get(0).getChildrenEDUS().size() == 0)
            {
                rootEDU.getChildrenEDUS().clear();
            }
        }
    }


    /**
     * 针对每个EDU节点，提取该节点所代表的字符串值。包括了子EDU和非EDU节点的值。
     */
    private String getEDUContent(DSAEDU rootEDU)
    {
        //首先计算根节点的content
        Tree   root    = rootEDU.getRoot();
        String content = getRootContent(root);

        rootEDU.setContent(content);

        //计算各个孩子节点的content值
        for( DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            String temp = getEDUContent(childEDU);
        }

        return content;
    }

    /**在短语结构树中获取该root节点下的字符串值。**/
    private String getRootContent(Tree root)
    {
        String content = "";

        for( Label curWord : root.yield() ) content += curWord.value() + " ";

        if( content.length() > 1 ) content = content.substring(0, content.length() - 1 );

        return content;
    }

    //-------------------------------2.1: Matchd Edu and connective--------------------

    /***
     * 寻找显式连词所关联的两个EDU
     * @param sentence
     */
    private ArrayList<String> matchConnAndEDUforExpRelation(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();
        ArrayList<String> argList = new ArrayList<String>();

        //针对每个连词都需要进行处理. 每个连词最少有一个EDU存在。
        for( DSAConnective curWord : conns )
        {
            if( !curWord.getInterConnective() ) continue;

            String wContent = curWord.getContent();
            DSAEDU connEDU  = this.getConnectiveBelongEDU(curWord, rootEDU);

            if( connEDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.println(sentence.getContent());
                curWord.setIsConnective(false);
                continue;
            }

            //d: 确定arg2包括的内容：从arg2EDU开始，向右的所有内容
            //将句法树中所有标记为arg2 Node的节点组装起来

            //e: 确定arg1包括的内容：这个就比较悲剧了。                    //将句法树中所有标记为arg1 Node的节点组装起来

//            String arg1Content = "",arg2Content = "";;

            String arg1Content = this.getArg1Content(curWord, connEDU);     //基于规则
//            String arg1Content = sentence.getArg1();          //基于机器学习

//            arg1Content = curWord.getArg1Content();     //
            curWord.setArg1Content(arg1Content);
            argList.add(arg1Content);

            String arg2Content = this.getArg2Content(curWord, connEDU);       //基于规则
//            String arg2Content = sentence.getArg2();      //基于机器学习

//            arg2Content = curWord.getArg2Content();     //
            curWord.setArg2Content(arg2Content);
            argList.add(arg2Content);

//            if(sentence.getArg1().isEmpty())
//            {
//                sentence.setArg1("论元 1 是 空的 ");
//                arg1Content = sentence.getArg1();
//            }
//            if(sentence.getArg2().isEmpty())
//            {
//                sentence.setArg2("论元 2 是 空的");
//                arg2Content = sentence.getArg2();
//            }

            if( arg2Content == null || arg2Content.length() < 2 ){ curWord.setIsConnective(false); continue; }
            if( arg1Content == null || arg1Content.length() < 2 ){ curWord.setInterConnective(false); continue;}

            //f: 生成候选关系, 此时关系还只有连词和argument，还没有识别具体关系
            DSAInterRelation interRelation = new DSAInterRelation();

            interRelation.setDsaConnective(curWord);
            interRelation.setArg2Content(arg2Content);
            interRelation.setArg1Content(arg1Content);
            interRelation.setSentID(sentence.getId());

            sentence.getRelations().add(interRelation);
        }

        return argList;
    }


    //在句法结构树中找到关联词节点
    private  Tree getConnectiveInTree(DSAConnective curWord,DSAEDU rootEDU)
    {
        int index = 0;

        Tree curWordNode = null;
        String  wContent = curWord.getContent();

        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        for( Tree curNode : phraseLeaves )
        {
            String curWordContent = curNode.nodeString();

            if( curWordContent.equalsIgnoreCase(wContent) )
            {
                if( Math.abs( curWord.getPositionInLine() - index ) < 3 )
                {
                    curWordNode = curNode;
                    break;
                }
            }
            index = index + curWordContent.length();
        }

        if( curWordNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s Tree Node In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        return curWordNode;
    }


    /**获取一个连词在短语结构树中所依附的EDU节点。最终返回的是该连词最直接所属的EDU节点**/
    private DSAEDU getConnectiveBelongEDU(DSAConnective curWord, DSAEDU rootEDU)
    {
        //a: 查找该词在短语结构分析树中所在的节点
        int index = 0;

        Tree curWordNode = null;
        String  wContent = curWord.getContent();

        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        for( Tree curNode : phraseLeaves )
        {
            String curWordContent = curNode.nodeString();

            if( curWordContent.equalsIgnoreCase(wContent) )
            {
                if( Math.abs( curWord.getPositionInLine()-index ) < 3 )
                {
                    curWordNode = curNode; break;
                }
            }
            index = index + curWordContent.length();
        }

        if( curWordNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s Tree Node In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        //b: 对connNode进行去除单链泛化,To-Do考虑修饰型连词 Modified Connective:部分原因是：
        while( curWordNode.parent(rootEDU.getRoot()) != null )
        {
            if(curWordNode.parent(rootEDU.getRoot()).children().length == 1)
            {
                curWordNode = curWordNode.parent(rootEDU.getRoot());
            }
            else break;
        }
        curWord.setConnNode(curWordNode);

        //c: 确定该连词所依附的EDU，向上直到碰见的第一个EDU节点作为父节点连词依附的EDU
        int num = 0, error = 0;
        Tree parentNode = curWordNode.parent( rootEDU.getRoot() );

        while( parentNode != null && !this.isAEDURoot(parentNode, rootEDU) )
        {
            parentNode = parentNode.parent(rootEDU.getRoot());
            if( parentNode.nodeString().equalsIgnoreCase("np") || num++ > 3 )
            {
                error = 1;break;
            }
        }

        //d: 如果该连词没有附着在任何一个有效的EDU上
        if( error == 1 || parentNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s ParentNode In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        return this.findTreeEDU(parentNode, rootEDU);
    }

    /***
     * 确定一个连词的Arg2范围，最直接上层EDU的右孩子节点集合. 返回的是arg2的字符串内容，按照空格分割
     * @param connWord：连词
     * @param arg2EDU：连词所属的直接上层EDU
     */
    private String getArg2Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg2Content = "";
        if( arg2EDU.getChildrenEDUS().size() < 2 )
        {
            connWord.getArg2Nodes().add( arg2EDU.getRoot() );
            connWord.setArg2EDU( arg2EDU );

            arg2Content = this.getEDUContent(arg2EDU);
        }
        else
        {
            //查找arg2的开始点
            boolean find  = false;
            Tree    root  = arg2EDU.getRoot();

            for( Tree curChild : root.children() )
            {
                //判断当前孩子节点是否是开始节点
                if( !find )
                {
                    if( curChild == connWord.getConnNode() ){
                        find = true;
                    }
                }
                else
                {
                    //从开始到最右边的都当做arg2,直到最右边的EDU,如果将该行语句放到循环上面，则arg2将不包括连词节点
                    connWord.getArg2Nodes().add(curChild);

                    arg2Content += this.getRootContent(curChild) + " ";

                    //在碰见第一个EDU的时候退出。
                    if( this.isAEDURoot(curChild, arg2EDU) )
                    {
                        DSAEDU temp = this.findTreeEDU(curChild, arg2EDU);
                        connWord.setArg2EDU( temp );
                        break;
                    }
                }
            }
        }
     return  arg2Content;
    }


    /**
     *将句法树中所有标记为arg2 Node的节点组装起来
     * @param connWord
     * @param arg2EDU
     * @return
     */
    private String getArg2ContentNew(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg2Content = "";


        return arg2Content;
    }

    /*** 判断一个句法分析树中的节点是否是EDU节点。*/
    private boolean isAEDURoot(Tree root, DSAEDU rootEDU)
    {
        boolean result = false;

        /**
        DSAEDU rootEDU = new DSAEDU();
        rootEDU.setRoot(root);

        result = this.getEDUsFromRoot(rootEDU);
         **/

        if( rootEDU.getRoot() == root ) return  true;

        for( DSAEDU childEDU:rootEDU.getChildrenEDUS() )
        {
            boolean temp = this.isAEDURoot(root, childEDU);

            result = result || temp;
        }

        return result;
    }

    /**查找一个Tree root 所位于的EDU节点**/
    private DSAEDU findTreeEDU(Tree root, DSAEDU rootEDU)
    {
        if( rootEDU.getRoot() == root ) return rootEDU;

        for( DSAEDU curEDU:rootEDU.getChildrenEDUS() )
        {
            DSAEDU childResult = this.findTreeEDU(root, curEDU);

            if( childResult != null ) return childResult;
        }

        return null;
    }

    private String getArg1Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg1Content = "";
        String wContent = connWord.getContent();
        int argConnArg  = Resource.ConnectiveArgNum.get(wContent)[0];
        int connArgArg  = Resource.ConnectiveArgNum.get(wContent)[1];


        if( argConnArg > connArgArg )
        {
            arg1Content = this.getArgConnArgContent(connWord, arg2EDU);
        }
        else
        {
            arg1Content = this.getConnArgArgContent(connWord, arg2EDU);
        }
        return arg1Content;
    }

    //    将句法树中标记为Arg1 Node 的内部节点组装起来
    private String getArg1ContentNew(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String arg1Content = "";

        return arg1Content;
    }


    /**
     * 查找类型为Arg-Conn-Arg的连词所涉及到的另一个EDU。
     * 返回的是同一Arg2EDU节点下的兄弟节点，或者是Arg2父节点中的兄弟节点
     **/
    private String getArgConnArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //如果arg2EDU单独成为一个EDU，则arg1EDU为arg2EDU的兄弟EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU = arg2EDU.getParentEDU();

            if( parentEDU == null ) return null;

            int parentChildSize = parentEDU.getChildrenEDUS().size();

            //查找对应的arg1EDU位置。
            for(int index = 1; index < parentChildSize; index++)
            {
                if(parentEDU.getChildrenEDUS().get(index) == arg2EDU)
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index - 1);
                    break;
                }
            }
            //没有找到arg1EDU的情况为：arg2EDU为parent的第一个 || arg2EDU的parent只有一个
            //如果是Arg-Conn-Arg类型的没有找到，则肯定是因为arg2EDU是parent的第一个孩子
            if( arg1EDU == null )
            {
                //再向上找一层,因为会出现并列EDU的情况
                DSAEDU grandPaEDU = parentEDU.getParentEDU();
                if( grandPaEDU != null )
                {
                    int grandPaChildSize = grandPaEDU.getChildrenEDUS().size();
                    for(int index = 1; index < grandPaChildSize; index++)
                    {
                        if( grandPaEDU.getChildrenEDUS().get(index) == arg2EDU ){
                            arg1EDU = parentEDU.getChildrenEDUS().get(index-1);
                        }
                    }
                }

                //此时：如果因为arg2EDU就是根部的EDU了，那么直接返回null, 否则是父亲节点的直接右孩子
                if( arg1EDU == null && arg2EDU.getParentEDU() != null && parentChildSize > 1 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(1);
                }
            }
        }
        else
        {
            //如果arg2EDU下面的edu不止一个，则在Arg-Conn-Arg类型下，arg1EDU有两种情况：
            //1：位于arg2EDU中的一个孩子EDU中。
            //2: 位于arg2EDU父亲节点中的一个
            for( int index = 0; index < arg2EDU.getChildrenEDUS().size(); index++)
            {
                DSAEDU curEDU = arg2EDU.getChildrenEDUS().get(index);

                if( curEDU == connWord.getArg2EDU() && index > 0 )
                {
                    arg1EDU = arg2EDU.getChildrenEDUS().get(index -  1);
                    break;
                }
            }
            if( arg1EDU == null )
            {
                //从上一层进行抽取一个
                if( arg2EDU.getParentEDU() != null )
                {
                    DSAEDU prev = null;
                    for( DSAEDU curEDU : arg2EDU.getParentEDU().getChildrenEDUS() )
                    {
                        if( curEDU == arg2EDU ){
                            arg1EDU = prev;
                            break;
                        }else{
                            prev = curEDU;
                        }
                    }
                    //如果上层也没有
                    if( arg1EDU == null ) arg1EDU = arg2EDU.getChildrenEDUS().get(1);
                }
            }
        }

        if( arg1EDU != null )
        {
            connWord.setArg1EDU(arg1EDU);
            arg1Content = this.getEDUContent(arg1EDU);
        }

        return arg1Content;
    }

    /***获取类型为ConnArgArg的arg1节点。**/
    private String getConnArgArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //如果arg2EDU单独成为一个EDU，则arg1EDU为arg2EDU的兄弟EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU    = arg2EDU.getParentEDU();

            if( parentEDU == null ) return null;

            int parentChildSize = parentEDU.getChildrenEDUS().size() - 1;

            //查找对应的arg1EDU位置。
            for( int index = 0; index < parentChildSize; index++ )
            {
                if( parentEDU.getChildrenEDUS().get(index) == arg2EDU )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            //如果是Conn-Arg-Arg类型的没有找到，则肯定是因为arg2EDU是parent的最后一个孩子
            if( arg1EDU == null )
            {
                //此时：如果因为arg2EDU就是根部的EDU了，那么直接返回null, 否则是父亲节点的直接左孩子
                if( arg2EDU.getParentEDU() != null && parentChildSize >= 2 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(parentChildSize - 2);
                }
            }
        }
        else
        {
            //如果arg2EDU下面的edu不止一个，则在Conn-Arg-Arg类型下，arg1EDU有两种情况：
            //1：位于arg2EDU中的一个孩子EDU中。
            //2: 位于arg2EDU父亲节点中的一个
            for( int index = 0; index < arg2EDU.getChildrenEDUS().size() - 1; index++)
            {
                DSAEDU curEDU = arg2EDU.getChildrenEDUS().get(index);

                if( curEDU == connWord.getArg2EDU() )
                {
                    arg1EDU = arg2EDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            if( arg1EDU == null )
            {
                //从上一层进行抽取一个
                if( arg2EDU.getParentEDU() != null )
                {
                    for( int index = 0; index < arg2EDU.getParentEDU().getChildrenEDUS().size() - 1; index++ )
                    {
                        DSAEDU curEDU = arg2EDU.getParentEDU().getChildrenEDUS().get(index);
                        if( curEDU == arg2EDU )
                        {
                            arg1EDU = arg2EDU.getParentEDU().getChildrenEDUS().get(index + 1);
                        }
                    }
                    //如果上层也没有. 则因为curWord的arg2EDU是根部arg2EDU的最右边的孩子
                    if( arg1EDU == null )
                    {
                        arg1EDU = arg2EDU.getChildrenEDUS().get(arg2EDU.getChildrenEDUS().size() - 2);
                    }
                }
            }
        }

        if( arg1EDU != null )
        {
            connWord.setArg1EDU( arg1EDU );
            arg1Content = this.getEDUContent(arg1EDU);
        }

        return arg1Content;
    }


    /**使用并列连词来获取EDU来产生候选的关系**/
    private void matchParallelConnAndEDUforExpRelation(DSASentence sentence)
    {
        String segSentContent = sentence.getSegContent();

        for(DSAConnective curord:sentence.getParallelConnectives())
        {
            String wContent = curord.getContent();

            String arg1Content ="",arg2Content="";
            try{
                arg1Content = segSentContent.substring( curord.getBeginIndex1(), curord.getBeginIndex2() );
                arg2Content = segSentContent.substring( curord.getBeginIndex2() );
            }
            catch (StringIndexOutOfBoundsException e){
                System.out.println(e.toString());
            }

            DSAInterRelation interRelation = new DSAInterRelation();
            interRelation.setArg1Content(arg1Content);
            interRelation.setArg2Content(arg2Content);
            interRelation.setSentID(sentence.getId());
            interRelation.setDsaConnective(curord);
        }
    }

    //-------------------------------3: Sense Recognize---------------------------

    /***识别具体的显式关系类型**/
    private void recognizeExpRelationType(DSASentence sentence) throws IOException
    {
        for( DSAInterRelation curRel:sentence.getRelations() )
        {
            DSAConnective curConn = curRel.getDsaConnective();
            String wContent       = curConn.getContent();

            String wFeature = "",wPrev = "",wPosOfConn = "";
            ArrayList<String> arrayList = new ArrayList<String>();
            String arrayfWords[] = sentence.getContent().split(" ");
            int indexOfConnective = 0;

//            for(int nIndex = 0 ; nIndex < arrayfWords.length ; nIndex++)
//            {
//                if(arrayfWords[nIndex].equalsIgnoreCase(wContent))
//                {
//                    indexOfConnective = nIndex;
//                }
//            }
//
//            wPosOfConn = sentence.getAnsjWordTerms().get(indexOfConnective).getNatrue().natureStr;
//
//            try {
//                wPrev = arrayfWords[indexOfConnective - 1];
//                if (wPrev.isEmpty())
//                {
//                    wPrev = arrayfWords[indexOfConnective - 3];
//                }
//            }
//            catch (ArrayIndexOutOfBoundsException e)
//            {
//                System.out.println(e.toString());
//            }
//
//            wFeature = wContent + " " + wPosOfConn + " " + wContent + wPrev;
//            arrayList.add(wFeature);
//            util.writeLinesToFile(".\\explicit.test.txt",arrayList);

//           String   senseNO =  ClassifyViaMaximumEntrop("arg_posModel.txt","explicit.test.txt");

            DSAWordDictItem item  = Resource.allWordsDict.get(wContent);
            double probality      = item.getMostExpProbality();
            String senseNO        = item.getMostExpProbalityRelNO();

            curRel.setRelType(Constants.EXPLICIT);

            if( Constants.SenseVersion == Constants.NewSenseVersion )
            {
                curRel.setRelNO( util.convertOldRelIDToNew(senseNO) );
            }
            else
            {
                curRel.setRelNO(senseNO);
            }

            curRel.setProbality(probality);
        }
    }


    /**不识别，只是用来生成候选的隐式关系**/
    private void getCandiateImpRelation(DSASentence sentence) throws IOException
    {
        if(sentence.getRelations().size() > 0 ) return;

        // 在基本的EDU的基础上开始进行两两匹配
        DSAEDU rootEDU = sentence.getRootEDU();

        //获取句子中的EDU对，作为隐式关系的候选
        //从根部往下开始寻找第一个孩子节点个数大于1的EDU节点
        while( rootEDU.getChildrenEDUS().size() == 1 )
        {
            rootEDU = rootEDU.getChildrenEDUS().get(0);
        }

        //最终退出的原因可能是因为只有一个孩子节点，下面没有了
        if( rootEDU.getChildrenEDUS().size() > 1 )
        {
            //两两顺序配对产生候选
            for(int index = 1; index < rootEDU.getChildrenEDUS().size(); index++)
            {
                DSAEDU curEDU  = rootEDU.getChildrenEDUS().get(index);
                DSAEDU prevEDU = rootEDU.getChildrenEDUS().get(index - 1);

                //判断孩子节点
                this.getImpRelationInSubEDU(sentence, curEDU);
                this.getImpRelationInSubEDU(sentence, prevEDU);

                DSAInterRelation interRelation = new DSAInterRelation();

                interRelation.setSentID(sentence.getId());
                interRelation.setRelType(Constants.IMPLICIT);
                interRelation.setRelNO(Constants.DefaultRelNO);

                interRelation.setArg1Content(prevEDU.getContent());
                interRelation.setArg2Content(curEDU.getContent());

                //判断是否有关系
                RelVectorItem item = impRelFeatureExtract.getFeatureLine(
                interRelation.getArg1Content(), interRelation.getArg2Content() );
                item.relNO = Constants.DefaultRelNO;

                int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

                if( senseType == 0 ) continue;

                interRelation.setRelNO( String.valueOf(senseType) );
                //sentence.getImpRelations().add(interRelation);
                sentence.getRelations().add(interRelation);
            }
        }
    }

    private void getImpRelationInSubEDU(DSASentence sentence, DSAEDU rootEDU) throws IOException
    {
        if( rootEDU.getChildrenEDUS().size() < 2 ) return;

        //两两顺序配对产生候选
        for(int index = 1; index < rootEDU.getChildrenEDUS().size(); index++)
        {
            DSAEDU curEDU  = rootEDU.getChildrenEDUS().get(index);
            DSAEDU prevEDU = rootEDU.getChildrenEDUS().get(index - 1);

            DSAInterRelation interRelation = new DSAInterRelation();

            interRelation.setSentID(sentence.getId());
            interRelation.setRelType(Constants.IMPLICIT);
            interRelation.setRelNO(Constants.DefaultRelNO);

            interRelation.setArg1Content(prevEDU.getContent());
            interRelation.setArg2Content(curEDU.getContent());

            //判断是否有关系
            RelVectorItem item = impRelFeatureExtract.getFeatureLine(
                    interRelation.getArg1Content(), interRelation.getArg2Content() );
            item.relNO = Constants.DefaultRelNO;

            int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

            if( senseType == 0 ) continue;

            interRelation.setRelNO( String.valueOf(senseType) );
            //sentence.getImpRelations().add(interRelation);
            sentence.getRelations().add(interRelation);
        }
    }

    /***识别可能存在的隐式句间关系**/
    private void recognizeImpRelationType(DSASentence sentence) throws IOException
    {

    }

    private ArrayList<DSAConnective> findConnective(String line) throws Exception
    {
        ArrayList<String> sentences = util.filtStringToSentence(line);

        DSASentence dsaSentence = new DSASentence(line);

        //this.findConnectiveWithRule(dsaSentence);

        ArrayList<DSAConnective> conWords = dsaSentence.getConWords();

        return conWords;
    }

    //-----------------------3.1: Cross Sentence Sense Recognize-----------------

    /**判断一个段落中的两个句子是否存在显式的句间关系。**/
    public boolean getCrossExpRelInPara(DSAParagraph para, DSASentence curSentence, DSASentence nextSentence) throws IOException {
        boolean hasExpRel = false;

        //1: 首先判断是否存在connArgArg类型的关系
        for(DSAConnective conn:curSentence.getConWords())
        {
            if( !conn.getInterConnective() && conn.getIsConnective() )
            {
                int argConnArg = Resource.ConnectiveArgNum.get(conn.getContent())[0];
                int connArgArg = Resource.ConnectiveArgNum.get(conn.getContent())[1];

                if( connArgArg > argConnArg )
                {
                    DSACrossRelation crossRelation = new DSACrossRelation();

                    crossRelation.arg1SentID  = curSentence.getId();
                    crossRelation.arg2SentID  = nextSentence.getId();
                    crossRelation.arg1Content = curSentence.getContent();
                    crossRelation.arg2Content = nextSentence.getContent();

                    crossRelation.conn        = conn;
                    crossRelation.relType     = Constants.EXPLICIT;

                    DSAWordDictItem item      = Resource.allWordsDict.get( conn.getContent() );
                    double     probality      = item.getMostExpProbality();
                    String       senseNO      = item.getMostExpProbalityRelNO();

                    crossRelation.relNO       = util.convertOldRelIDToNew(senseNO);
                    crossRelation.probality   = probality;

                    para.crossRelations.add(crossRelation);

                    hasExpRel = true;
                }
            }
        }

        //2: 判断是否存在conn-Arg-Arg类型的关系
        for( DSAConnective conn:nextSentence.getConWords() )
        {
            if( !conn.getInterConnective() && conn.getIsConnective() )
            {
                int argConnArg = Resource.ConnectiveArgNum.get(conn.getContent())[0];
                int connArgArg = Resource.ConnectiveArgNum.get(conn.getContent())[1];

                if( connArgArg <= argConnArg )
                {
                    DSACrossRelation crossRelation = new DSACrossRelation();

                    crossRelation.arg1SentID  = curSentence.getId();
                    crossRelation.arg2SentID  = nextSentence.getId();
                    crossRelation.arg1Content = curSentence.getContent();
                    crossRelation.arg2Content = nextSentence.getContent();

                    crossRelation.conn        = conn;
                    crossRelation.relType     = Constants.EXPLICIT;

                    DSAWordDictItem item      = Resource.allWordsDict.get( conn.getContent() );
                    double     probality      = item.getMostExpProbality();
                    String       senseNO      = item.getMostExpProbalityRelNO();

                    crossRelation.relNO       = util.convertOldRelIDToNew(senseNO);
                    crossRelation.probality   = probality;

                    para.crossRelations.add(crossRelation);

                    hasExpRel = true;
                }
            }
        }

        return hasExpRel;
    }


    /**判断一个段落中的两个句子是否存在隐式关系**/
    private int getCrossImpInPara(DSAParagraph para, DSASentence curSentence,
                                  DSASentence nextSentence) throws IOException
    {
        String cur  = curSentence.getSegContent();
        String nex = nextSentence.getSegContent();

        RelVectorItem item = impRelFeatureExtract.getFeatureLine(cur, nex);
        item.relNO         = Constants.DefaultRelNO;

        int senseType = this.relationSVMMode.predict( item.toLineForLibsvm() );

        //如果存在非0隐式关系
        if( senseType != 0 )
        {
            DSACrossRelation crossRelation = new DSACrossRelation();

            crossRelation.arg1SentID  = curSentence.getId();
            crossRelation.arg2SentID  = nextSentence.getId();
            crossRelation.arg1Content = curSentence.getContent();
            crossRelation.arg2Content = nextSentence.getContent();

            crossRelation.relType     = Constants.IMPLICIT;
            crossRelation.relNO       = String.valueOf(senseType);
            crossRelation.probality   = 1.0;
            crossRelation.conn        = null;

            //TO-DO: 后续可以添加上候选连词，比如哪个连词可以插入
            para.crossRelations.add(crossRelation);
        }

        return senseType;
    }


     //-----------------------4：计算数据-----------------
    /**计算EDU切分代码的准确度**/
    public void countEDUAccuray() throws DocumentException, IOException {
        Resource.LoadRawRecord();
        this.phraseParser = new PhraseParser();

        int allEDUNum = 0, arg1Correct = 0, arg2Correct = 0;//完全相同的个数
        int[] arg1CorrectInPercent   = new int[10];
        int[] arg2CorrectInPercent   = new int[10];

        DSAEDU rootEDU = null;
        String text, arg1Content, arg2Content, arg1EDU, arg2EDU;

        //随机获取200个来检查准确率， 用300个做缓冲
        boolean[] exist = util.getRandomArrays(Resource.Raw_Train_Annotation_p3.size(), 500);

        for(int index = 0; index < Resource.Raw_Train_Annotation_p3.size(); index++)
        {
            if( !exist[index] ) continue;

            SenseRecord curRecord = Resource.Raw_Train_Annotation_p3.get(index);

            text        = curRecord.getText();
            arg1Content = curRecord.getArg1();
            arg2Content = curRecord.getArg2();

            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( text.length() > Constants.Max_Sentence_Length + 100 ) continue;

            //System.out.println(curRecord.getfPath());
            //System.out.println(text);

            //查看进行短语结构分析得到的两个Argument
            DSASentence dsaSentence = new DSASentence(text);
            dsaSentence.setSegContent(text);

            try
            {
                boolean  eduResult = this.findArgumentWithPhraseParser(dsaSentence);
                if( !eduResult ) continue;
            }
            catch(OutOfMemoryError e)
            {
                continue;
            }

            //获取自动分析的结果
            rootEDU = dsaSentence.getRootEDU();

            while( rootEDU.getChildrenEDUS().size() == 1 ) rootEDU = rootEDU.getChildrenEDUS().get(0);
            if( rootEDU.getChildrenEDUS().size() < 2 ) continue;

            allEDUNum = allEDUNum + 2;

            arg1EDU = rootEDU.getChildrenEDUS().get(0).getContent();
            arg2EDU = rootEDU.getChildrenEDUS().get(1).getContent();

            //判断人工标注和自动分析的EDU的相似度
            int sameNum1 = util.countSameCharatersNum(arg1Content, arg1EDU);
            int sameNum2 = util.countSameCharatersNum(arg2Content, arg2EDU);

            int length1 = arg1Content.length() > arg1EDU.length() ? arg1EDU.length():arg1Content.length();
            int length2 = arg2Content.length() > arg2EDU.length() ? arg2EDU.length():arg2Content.length();

            if( Math.abs(sameNum1 - length1) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg1Correct++;
            if( Math.abs(sameNum2 - length2) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg2Correct++;


            //超过7成相似，则不相似的占据3成
            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.4) arg1CorrectInPercent[6]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.3) arg1CorrectInPercent[7]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.3) arg2CorrectInPercent[7]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.2) arg1CorrectInPercent[8]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.2) arg2CorrectInPercent[8]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.1) arg1CorrectInPercent[9]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.1) arg2CorrectInPercent[9]++;

             //输出自动分析结果和人工标注结果
            System.out.println("-------------****************************----------------------");
            System.out.println(text);
            System.out.println("annote1: " + arg1Content);
            System.out.println("myAuto1: " + arg1EDU);
            System.out.println("annote2: " + arg2Content);
            System.out.println("myAuto1: " + arg2EDU);
        }

        System.out.println("All EDU:" + allEDUNum );
        System.out.println(" arg1Correct:" + arg1Correct + " arg2Correct:" + arg2Correct );

        for(int index = 7; index < 10; index ++)
        {
            System.out.format("Same Percentnge %d0 percent arg1CorrectIn:%d arg2Correct:%d\n", index, arg1CorrectInPercent[index], arg2CorrectInPercent[index]);
        }
    }

    /**
     * 计算使用逗号来对EDU进行切分得到的准确率。目前的做法是简单使用逗号对句子进行分割
     * 并和基于规则的EDU切分进行对比。
     * @throws DocumentException
     */
    public void countEDUAccurayWithComma() throws DocumentException, IOException
    {
        Resource.LoadRawRecord();

        int allEDUNum = 0, arg1Correct = 0, arg2Correct = 0;//完全相同的个数
        int[] arg1CorrectInPercent   = new int[10];
        int[] arg2CorrectInPercent   = new int[10];


        String text, arg1Content, arg2Content, arg1EDU, arg2EDU;

        //随机获取200个来检查准确率， 用300个做缓冲
        boolean[] exist = util.getRandomArrays(Resource.Raw_Train_Annotation_p3.size(), 500);

        String fPath = "data/edu/eduSplitWithSimpleComma.txt";
        BufferedWriter fw = new BufferedWriter(new FileWriter(new File(fPath)));

        for(int index = 0; index < Resource.Raw_Train_Annotation_p3.size(); index++)
        {
            if( !exist[index] ) continue;

            SenseRecord curRecord = Resource.Raw_Train_Annotation_p3.get(index);

            text        = curRecord.getText();
            arg1Content = curRecord.getArg1();
            arg2Content = curRecord.getArg2();

            if( curRecord.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( text.length() > Constants.Max_Sentence_Length + 100 ) continue;

            //对句子进行逗号分割并输出结果
            allEDUNum = allEDUNum + 2;

            String[] edus =  util.splitSentenceWithComma(text);

            if( edus.length < 2 ) continue;

            arg1EDU = edus[0];
            arg2EDU = edus[1];

            fw.write("---------------************************---------------------------\n");
            fw.write( text + "\nannoted1: " + arg1Content + "\n comma: " + edus[0] + "\n");
            fw.write( "annoted2: " + arg2Content + "\n comma: " + edus[1] + "\n\n");

            //程序自动计算准确率
            int sameNum1 = util.countSameCharatersNum(arg1Content, arg1EDU);
            int sameNum2 = util.countSameCharatersNum(arg2Content, arg2EDU);

            int length1 = arg1Content.length() > arg1EDU.length() ? arg1EDU.length():arg1Content.length();
            int length2 = arg2Content.length() > arg2EDU.length() ? arg2EDU.length():arg2Content.length();

            if( Math.abs(sameNum1 - length1) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg1Correct++;
            if( Math.abs(sameNum2 - length2) < 3 && Math.abs(arg1EDU.length() - arg1Content.length()) < 3 ) arg2Correct++;


            //超过7成相似，则不相似的占据3成
            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.4) arg1CorrectInPercent[6]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.3) arg1CorrectInPercent[7]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.3) arg2CorrectInPercent[7]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.2) arg1CorrectInPercent[8]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.2) arg2CorrectInPercent[8]++;

            if( Math.abs(sameNum1 - length1) * 1.0 <= length1 * 0.1) arg1CorrectInPercent[9]++;
            if( Math.abs(sameNum2 - length2) * 1.0 <= length2 * 0.1) arg2CorrectInPercent[9]++;


        }
        fw.close();

        System.out.println("All EDU:" + allEDUNum );
        System.out.println(" arg1Correct:" + arg1Correct + " arg2Correct:" + arg2Correct );

        for(int index = 7; index < 10; index ++)
        {
            System.out.format("Same Percentnge %d0 percent arg1CorrectIn:%d arg2Correct:%d\n", index, arg1CorrectInPercent[index], arg2CorrectInPercent[index]);
        }

    }

    /**
     * 句法树内部结点分类, prepare corpus for training and testing.
     * @throws IOException
     */
    private void train( ) throws IOException
    {

        int index = 0;
//      标记句法树中的每一个节点
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;      //filter implicit relations.

            int nClass = 0;
            String wConnective = senseRecord.getConnective();
            String arg1Content = "",arg2Content = "";
            String text = senseRecord.getText();
            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            try{
                this.preProcess(sentence,false);
                this.findConnWordWithML(sentence);
                this.markConnAsInterOrCross(sentence,index);
                this.findParallelWord(sentence);

                this.findArgumentWithPhraseParser(sentence);

                DSAEDU rootEDU = sentence.getRootEDU();

                Tree tChildren[] = rootEDU.getRoot().children();

                for( Tree child : rootEDU.getRoot().children() )
                {
                    //不考虑单链泛化的孩子节点：类似于：他->Pron->N之类的
                    if( child.depth() == child.size() - 1 ) continue;

                    DSAEDU curEDU = new DSAEDU();
                    curEDU.setRoot(child);
                    curEDU.setParentEDU(rootEDU);
                    curEDU.setDepth( rootEDU.getDepth() + 1 );

                    boolean temp = getEDUsFromRoot( curEDU);
//                    if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                    nClass = InnerNodeClassificationTrain(child,senseRecord,sentence,index,wConnective);
                    if(1 == nClass)          sentence.setArg1(curEDU.getContent());
                    else if(2 == nClass)     sentence.setArg2(curEDU.getContent());
                    else  continue;

                }
                index++;
                System.out.println("SenseRecord number is :" + index);
            }
            catch ( NullPointerException e){
                System.out.println("NullPointerException!");
            }
        }

    }

    /**
     * 获取arg1、arg2、None结点的训练、测试数目
     * @throws IOException
     */
    private void trainArgument( ) throws IOException
    {
        String arg1Content = "",arg2Content = "",text;
        int index = 0;

        int nClass = 0;
//      标记句法树中的每一个节点

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;

            String wConnective = senseRecord.getConnective();

            try{
                text = senseRecord.getText();

                DSASentence sentence = new DSASentence(text);
                sentence.setSegContent(text);
                sentence.setId(index);

                this.preProcess(sentence,false);
                this.findConnWordWithML(sentence);
                this.markConnAsInterOrCross(sentence,index);
                this.findParallelWord(sentence);

                this.findArgumentWithPhraseParser(sentence);

                DSAEDU rootEDU = sentence.getRootEDU();

                Tree tChildren[] = rootEDU.getRoot().children();

                for( Tree child : rootEDU.getRoot().children() )
                {
                    //不考虑单链泛化的孩子节点：类似于：他->Pron->N之类的
                    if( child.depth() == child.size() - 1 ) continue;

                    DSAEDU curEDU = new DSAEDU();
                    curEDU.setRoot(child);
                    curEDU.setParentEDU(rootEDU);
                    curEDU.setDepth( rootEDU.getDepth() + 1 );

                    boolean temp = getEDUsFromRoot( curEDU);
//                    if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

                    InnerNodeClassificationTrain(child,senseRecord,sentence,index,wConnective);
                    if(1 == nClass)          sentence.setArg1(curEDU.getContent());
                    else if(2 == nClass)     sentence.setArg2(curEDU.getContent());
                    else  continue;;

                }
                index++;
                System.out.println("SenseRecord number is :" + index);
            }
            catch ( NullPointerException e){
                System.out.println("NullPointerException!");
            }
        }

    }


    /**
     * 论元位置分类
     * @throws IOException
     */
    private void trainPosition( ) throws IOException
    {
        String text;
        int index = 0;

//      标记句法树中的每一个节点
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            text        = senseRecord.getText();

            String wConnective = senseRecord.getConnective();

            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            this.preProcess(sentence,false);
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence,index,true,3,wConnective);
            this.findParallelWord(sentence);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }
    }


    /**
     * 统计1字数、P2字数、P3字数、总字数
     */
    private void countWordNumbers()
    {
        String text,arg1Content,arg2Content;
        long lWordNumberofP1 = 0,lWordNumberofP2 = 0 ,lWordNumberofP3 = 0,lWordNumberofTotal = 0;
        long lRelationNumber = 0;

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p1)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP1 += strContent.length();
        }

        System.out.print("Total word number of P1 is ");
        System.out.println(lWordNumberofP1);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP2 += strContent.length();
        }

        System.out.print("Total word number of P2 is ");
        System.out.println(lWordNumberofP2);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofP3 += strContent.length();
        }
        System.out.print("Total word number of P3 is ");
        System.out.println(lWordNumberofP3);

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation)
        {

            String strContent;
            text        = senseRecord.getText();
            strContent = text.replaceAll(" ","");

            lWordNumberofTotal += strContent.length();
            lRelationNumber ++;
        }

        System.out.print("Total word number is ");
        System.out.println(lWordNumberofTotal);

        System.out.print("Total relation number is ");
        System.out.println(lRelationNumber);

    }

    /**
     * 计算论元自动切分的准确率
     * @throws DocumentException
     */
    public void computeArgumentAccuracy() throws Exception {
//        Resource.LoadRawRecord();

        double  dAccuracyOfArg1,dAccuracyOfArg2;

        int nIndex = 0,nNumbers = 0;      //index 为Record的编号，而numbers是所选取的Record的编号
        int nRightOfArg1 = 0, nRightOfArg2 = 0;                 //得到正确论元的数目

        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {

            if(record.getType().equals(Constants.IMPLICIT) || (nIndex % 3) != 0) //遇到隐式关系则跳过，而且随机抽取一些编号正好是3的倍数的record，如果不是3的倍数，则跳过；
            {
                nIndex++;
                continue;
            }
            else
            {
                if(nNumbers < 300)      //只抽取300条显示篇章关系的记录
                {
                    String content = record.getText();
                    DSASentence sentence = new DSASentence(content);
                    sentence.setId(nNumbers);

                    try{
                        this.processSentence(sentence,false);
                    }
                    catch (NullPointerException e){
                        nIndex++;
                        continue;
                    }

                    String arg1 = sentence.getArg1();
                    String arg2 = sentence.getArg2();

                    if(arg1.equals(record.getArg1()))
                    {
                        nRightOfArg1 ++;
                    }
                    if(arg2.equals(record.getArg2()))
                    {
                        nRightOfArg2++;
                    }

                    nIndex++;
                    nNumbers++;
                }
                else  break;

            }
        }

        System.out.println("The number of right arg1 is " + nRightOfArg1);
        System.out.println("The number of right arg2 is " + nRightOfArg2);
        System.out.println("The number of total arguments is " + nNumbers);

        dAccuracyOfArg1 = (double)nRightOfArg1 / nNumbers;
        dAccuracyOfArg2 = (double)nRightOfArg2 / nNumbers;

        System.out.println("The accuracy of arg1 is " + dAccuracyOfArg1);
        System.out.println("The accuracy of arg2 is " + dAccuracyOfArg2);
    }

    /**
     * 得到每条测试语料的arg1和arg2，并写入文件arg.txt，用于之后计算论元识别准确率
     * @throws IOException
     */
    public void argument() throws IOException {

        int index = 0;

//      标记句法树中的每一个节点

//        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
            if(index++ < 6053)    continue;

            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;

            ArrayList<String> argList = new ArrayList<String>();

//            text        = senseRecord.getText();
//            arg1Content = senseRecord.getArg1();
//            arg2Content = senseRecord.getArg2();

            argList.add(senseRecord.getArg1() + "|" + senseRecord.getArg2());
            util.appendMethodB("arg.txt",argList);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }
    }


    public void getArgumentOfTestCorpora() throws  IOException
    {
        int index = 0;

//        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
            if(index++ < 6053)    continue;

            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;

            String strArg1="",strArg2="";

            ArrayList<String> argList = new ArrayList<String>();
            boolean argPosition = true;     //默认将论元位置设置为SS

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(index);

            //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
            this.preProcess(sentence, false);

            //2: 识别单个连词和识别并列连词：不仅...而且
            this.findConnWordWithML(sentence);
            argPosition = this.markConnAsInterOrCross(sentence,index);
            this.findParallelWord(sentence);

            //4: 将句子按照短语结构拆分成基本EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1：避免出现句子结构非法的Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); return; }

            //论元位置为SS(分句关系)
            if(argPosition)
            {
                //内部节点分类：Arg1、Arg2、None
                argLaberler(sentence);      //抽取特征，将句法树内部节点进行分类：Arg1 Node、Arg2 Node、None
            }

            argList = this.matchConnAndEDUforExpRelation(sentence);

//            if(argList.isEmpty())   continue;

            strArg1 = argList.get(0);
            strArg2 = argList.get(1);

            argList.add(strArg1 + "|" + strArg2);
            util.appendMethodB("argOfTestCorpora.txt",argList);

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
            System.out.println(senseRecord.getText());

        }

    }

    public void comuteRelationAccuracy() throws IOException
    {
        int index = 0,nNumberOfRight = 0, nNumberOfTotal= 0; ;
        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {
//            if(index++ < 4366)    continue;
//            if(index++ < 6053)    continue;
            index++;
            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;
            if(senseRecord.getText().length() >= 500)    continue;
            if(26 == index || 53 == index)     continue;

//            if(index >= 50) break;
//            if(index < 55)  continue;
            if(index > 200) break;
            if(nNumberOfTotal > 100)   break;      //只选取200句


            String strArg1="",strArg2="",strTypeInCorpus = "",strType = "";

            strTypeInCorpus = senseRecord.getRelNO();
            String arrayOfRightTypy[] = strTypeInCorpus.split("-");


            ArrayList<String> argList = new ArrayList<String>();
            boolean argPosition = true;     //默认将论元位置设置为SS

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(index);

            //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
            this.preProcess(sentence, false);

            //2: 识别单个连词和识别并列连词：不仅...而且
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence);
            this.findParallelWord(sentence);

            //4: 将句子按照短语结构拆分成基本EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1：避免出现句子结构非法的Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); continue; }

            ArrayList<DSAConnective> arrayConnective = sentence.getConWords();


            try
            {
                //5: 确定每个连词所涉及到的两个EDU来产生候选显式关系
                this.matchConnAndEDUforExpRelation(sentence);
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString());
                continue;
            }

            try
            {
                this.matchParallelConnAndEDUforExpRelation(sentence);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                System.out.println(e.toString());
                continue;
            }

            //6: 根据获取到的信息，识别显式句间关系
            this.recognizeExpRelationType(sentence);
            //7: 识别可能存在的隐式句间关系
            this.getCandiateImpRelation(sentence);
            this.recognizeImpRelationType(sentence);

            int nNumOfRelations = sentence.getRelations().size();

            if(sentence.getRelations().isEmpty())   continue;

            nNumberOfTotal ++;
            strType = sentence.getRelations().get(0).getRelNO();
            String arrayOfType[] = strType.split("-");

            if (arrayOfType[0].equalsIgnoreCase(arrayOfRightTypy[0]))
            {
                nNumberOfRight++;
            }

//            for(int i = 0; i <nNumOfRelations; i++)
//            {
//                strType = sentence.getRelations().get(i).getRelNO();
//
//                if(strTypeInCorpus.equalsIgnoreCase(strType))
//                {
//                    nNumberOfRight++;
//                }
//            }

            System.out.println("SenseRecord number is :" + index);
        }

        double dAccuracy;
        dAccuracy = (double)nNumberOfRight / nNumberOfTotal;
        System.out.println("The number of right is " + nNumberOfRight);
        System.out.println("The total number is " + nNumberOfTotal);
        System.out.println("The accuracy of explicit relation recognition is " + dAccuracy);

    }

    /**
     * 获取语料中标注的论元位置分类结果
     * @throws IOException
     */
    public void getConnectiveIsInterOrNot() throws IOException
    {
        int nIndex = 0;
        ArrayList<String> arrayList = new ArrayList<String>();

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT)) continue;

            String bIsInterConnective = "true";
            arrayList.add(bIsInterConnective);
            nIndex++;
        }

        System.out.println("The record number is " + nIndex);
        util.appendMethodB("positionResult.txt",arrayList);

    }




    /**
     * 获取论元位置分类结果，用于之后计算机PRF
     * @throws IOException
     */
    public void computPRFOfPosition() throws IOException
    {

        int index = 0,nNumberOfTotal = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p2)
        {
            index++;
            if(senseRecord.getType().equals(Constants.IMPLICIT)) continue;
            if(senseRecord.getText().length() >= 500)    continue;
//            if(26 == index || 53 == index)     continue;

            DSASentence sentence = new DSASentence(senseRecord.getText());
            sentence.setId(nNumberOfTotal);

            //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
            this.preProcess(sentence, false);

            //2: 识别单个连词和识别并列连词：不仅...而且
            this.findConnWordWithML(sentence);
            this.markConnAsInterOrCross(sentence);
            this.findParallelWord(sentence);

            //4: 将句子按照短语结构拆分成基本EDU
            //this.findArgumentInLine(sentence);
            boolean tempResult = this.findArgumentWithPhraseParser(sentence);

            //4.1：避免出现句子结构非法的Sentence
            if( !tempResult ){ sentence.setIsCorrect(false); continue; }

            ArrayList<DSAConnective> arrayConnective = sentence.getConWords();

            try
            {
                //5: 确定每个连词所涉及到的两个EDU来产生候选显式关系
                this.matchConnAndEDUforExpRelation(sentence);
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString());
                continue;
            }


            String isInterConnective = "false";
            for(DSAConnective conn:arrayConnective)
            {
                if(!conn.getIsConnective())  continue;

                if(conn.getInterConnective())
                {
                    isInterConnective = "true";
                }
            }

            stringArrayList.add(isInterConnective);

            nNumberOfTotal ++;
            System.out.println("SenseRecord number is :" + nNumberOfTotal);
        }

        util.appendMethodB("positionForTest.txt",stringArrayList);

    }

    /**
     * 获取每一条语料的内容
     * @throws IOException
     */
    public void getSentRecord() throws IOException
    {

        int nIndex = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            String wIndex = Integer.toString(nIndex);
            String wText = senseRecord.getText();
            String wResult = wIndex + " : " + wText;

            stringArrayList.add(wResult);
            nIndex++;
        }

        util.writeLinesToFile("./SenseRecord.txt",stringArrayList);
    }

    /**
     * 获取每条语料，及其对应的论元
     * @throws IOException
     */
    public void getSentRecordAndArgument() throws IOException
    {

        int nIndex = 0;
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for(SenseRecord senseRecord: Resource.Raw_Train_Annotation_p3)
        {

            String wIndex = Integer.toString(nIndex);
            String wText = senseRecord.getText();
            String wArg1 = senseRecord.getArg1(), wArg2 = senseRecord.getArg2();


            String wResult = wIndex + " : " + wText + "; Arg1: " + wArg1 + "; Arg2:" + wArg2;

            stringArrayList.add(wResult);
            nIndex++;
        }

        util.writeLinesToFile("./SenseRecordAndArgument.txt",stringArrayList);
    }


    /**
     * 使用最大熵模型进行分类
     * @param wModelFileName    最大熵模型的文件名
     * @param wTestFileName     测试数据的文件名
     * @return                  返回分类结果
     */
    public String ClassifyViaMaximumEntrop(String wModelFileName, String wTestFileName)
    {
        String wResult="";      //wResult为分类结果

        // 采用最大熵分类器进行分类
        Predict predictor = null;
        String modelFileName = wModelFileName;
        try {
            GISModel m =
                    new SuffixSensitiveGISModelReader(
                            new File(modelFileName)).getModel();
            predictor = new Predict(m);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        try {
            DataStream ds =
                    new PlainTextByLineDataStream(
                            new FileReader(new File(wTestFileName)));

            while (ds.hasNext()) {
                String s = (String)ds.nextToken();
                wResult = predictor.eval(s.substring(0, s.lastIndexOf(' ')));
            }
        }
        catch (Exception e) {
            System.out.println("Unable to read from specified file: "
                    + "arg_pos.test");
            System.out.println();

        }

        return  wResult;
    }

    /**
     *获取P2文件中显式关系的数目，结果为2032
     */
    public void getExplicitNumbersInP2()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p2)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT)) index++;
        }
        System.out.println("The number of explicit relation is " + index);
    }

    /**
     *获取P3文件中显式关系的数目，结果为5879
     */
    public void getExplicitNumbersInP3()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT)) index++;
        }
        System.out.println("The number of explicit relation is " + index);
    }

    /**
     * 分别获取P2文件和P3文件中论元位置分类中的错误实例
     * @throws IOException
     */
    public void getErrorResultOfArgPos() throws IOException
    {
        int index = 0;
        ArrayList<String> arrayListOfError = new ArrayList<String>();

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {

            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String text        = senseRecord.getText();
            String wRelNo = senseRecord.getRelNO();
            String arrType[] = wRelNo.split("-");
            String wType = arrType[0];
            String wResult ="";

            wResult = senseRecord.getConnective() + ": " + text;

            DSASentence sentence = new DSASentence(text);
            sentence.setSegContent(text);
            sentence.setId(index);

            this.preProcess(sentence,false);
            this.findConnWordWithML(sentence);
            Boolean bResult = this.markConnAsInterOrCross(sentence,index);
            this.findParallelWord(sentence);

            if (!bResult)
            {
                arrayListOfError.add(wResult);
            }

            index++;
            System.out.print("SenseRecord number is :");
            System.out.println(index);
        }

        util.writeLinesToFile("errorInstanceInP3.txt",arrayListOfError);

    }

    /**
     * Count number that the connective is in the head of sentence.
     */
    public void countHeadInP2P3()
    {
        int nIndexOfP2 =0,nIndexOfP3 = 0;
        int nHeadInP2 = 0,nHeadInP3 = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p2)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String sConnective = senseRecord.getConnective();
            String sText[] = senseRecord.getArg2().split(" ");

            if(sText[0].equalsIgnoreCase(sConnective))  nHeadInP2++;

            nIndexOfP2++;
        }

        double dHeadInP2 = (double)nHeadInP2 / nIndexOfP2;
        System.out.println("The explicit number of P2 is :" + nIndexOfP2);
        System.out.println("The number of cases where connective is at the head of sentece: " + nHeadInP2);
        System.out.println("The rate of Head in P2 is : " + dHeadInP2);

        System.out.println("________________________________~我是华丽丽的分割线~________________________________________");

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p3)
        {
            if(senseRecord.getType().equalsIgnoreCase(Constants.IMPLICIT))  continue;

            String sConnective = senseRecord.getConnective();
            String sText[] = senseRecord.getText().split(" ");

            if(sText[0].equalsIgnoreCase(sConnective)) nHeadInP3++;

            nIndexOfP3++;
        }

        double dHeadInP3 = (double)nHeadInP3 / nIndexOfP3;
        System.out.println("The explicit number of P3 is :" + nIndexOfP3);
        System.out.println("The number of cases where connective is at the head of sentece: " + nHeadInP3);
        System.out.println("The rate of Head in P3 is : " + dHeadInP3);
    }


    public void countNumbersOfP1()
    {
        int index = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation_p1)
        {
            index++;
        }

        System.out.println("The instances number of P1 is "+ index);
    }


    public void countCorpus()
    {
        int nExplicitOne = 0, nExplicitTwo = 0, nExplicitThree = 0, nExplicitFour = 0;
        int nImplicitOne = 0, nImplicitTwo = 0, nImplicitThree = 0, nImplicitFour = 0;

        for(SenseRecord senseRecord:Resource.Raw_Train_Annotation)
        {
            String strTypeArr[] = senseRecord.getRelNO().split("-");
            String strType = strTypeArr[0];

            if (senseRecord.getType().equalsIgnoreCase(Constants.EXPLICIT))
            {
                if (strType.equalsIgnoreCase("1"))       nExplicitOne++;
                else if(strType.equalsIgnoreCase("2"))  nExplicitTwo++;
                else if(strType.equalsIgnoreCase("3"))  nExplicitThree++;
                else  nExplicitFour++;
            }
            else
            {
                if (strType.equalsIgnoreCase("1"))       nImplicitOne++;
                else if(strType.equalsIgnoreCase("2"))  nImplicitTwo++;
                else if(strType.equalsIgnoreCase("3"))  nImplicitThree++;
                else  nImplicitFour++;
            }

        }

        System.out.println("The explicit relations:");
        System.out.println("One: " + nExplicitOne +" Two: " + nExplicitTwo +" Three: " + nExplicitThree + " Four:" + nExplicitFour);

        System.out.println("The implicit relations:");
        System.out.println("One: " + nImplicitOne +" Two: " + nImplicitTwo +" Three: " + nImplicitThree + " Four:" + nImplicitFour);

    }



    public static void main(String[] args) throws Exception
    {
        //DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test     = "我 听到 过 很多 解释, 但是 我 觉得 我 从没有 看到 过 清晰 的 分析 。";
        String multiple = "中国 北方 重要 经济 城市 天津 与 俄罗斯 联邦 的 经贸 交往 目前 正 稳步 发展 ， 并 呈现 出 新 的 特色 。";
        String simple   = "浦东 开发 开放 是 一 项 振兴 上海 ， 建设 现代化 经济 、 贸易 、 金融 中心 的 跨世纪 工程 ， 因此 大量 出现 的 是 以前 不 曾 遇到 过 的 新 情况 、 新 问题 。";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);
        String twoSentence = "据 了解 ， 高行健 目前 已经 完成 了 一 部 新作 《 另 一 种 美 》 的 书稿 ， 并且 表示 能够 在 台湾 出版 。 高行健 １２月 １０号 将 在 瑞典 首都 斯德哥尔摩 举行 的 赠奖 仪式 当中 和 其他 诺贝尔 奖 得主 接受 瑞典 国王 卡尔 十六 世 古斯达夫 的 颁奖 。";

        String line = "我 听到 过 很多 解释, 但是 我 觉得 我 从没有 看到 过 清晰 的 分析 。";

        boolean needSegment  = false;

        /**
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
         **/

        //对特定文件进行Discourse Parse 并将得到的XML写入文件
//        ArrayList<String> stringArrayList = new ArrayList<String>();
//        ArrayList<String> stringArrayResult = new ArrayList<String>();
//
//        if(args[0].isEmpty())
//        {
//            System.out.println("Parameter Error??");
//            return;
//        }
//
//        String strFileName = args[0];
//        String strResultFileName = strFileName + "Result.xml";
//
//        util.readFileToLines(strFileName,stringArrayList);
//
//        int index = 0;
//        for (String str:stringArrayList)
//        {
//            System.out.println(index + str);
//            index++;
//        }
//
//        DiscourseParser dp = new DiscourseParser();
//
//        for (String strContext:stringArrayList)
//        {
//            String strResult = "";
//            DSAParagraph paragraph = dp.parseRawFile(strContext, true);
//            strResult = paragraph.toXML();
//            stringArrayResult.add(strResult);
//        }
//
//        util.appendMethodB(strResultFileName,stringArrayResult);

        //Experiments
        //实验相关
        DiscourseParser dp = new DiscourseParser();

//        dp.getExplicitNumbersInP2();


//        dp.parseRawFile(test, needSegment);
//        dp.computeArgumentAccuracy();

//        dp.getSentRecord();
//        dp.getSentRecordAndArgument();

//        dp.getConnectiveIsInterOrNot();

         //论元相关
//        dp.argument();
//        dp.getArgumentOfTestCorpora();

        //论元位置分类实验
//        dp.trainPosition();

        //获取论元位置分类的错误实例
//        dp.getErrorResultOfArgPos();

//        dp.computPRFOfPosition();

//        dp.computeArgumentAccuracy();


        //句法树内部结点分类实验
        dp.train();

        //统计字数
//        dp.countWordNumbers();
//        dp.countNumbersOfP1();
        //Count numbers of the connective is at the head of sentence.
//        dp.countHeadInP2P3();

//        dp.countCorpus();   //count numbers of four classes in Explicit and Implicit.
//        dp.comuteRelationAccuracy();
//        DSASentence dsaSentence = new DSASentence(test);
//        dp.countEDUAccuray();
//        dp.countEDUAccurayWithComma();


    }
}

