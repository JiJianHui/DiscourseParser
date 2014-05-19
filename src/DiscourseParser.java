import common.Constants;
import entity.recognize.*;
import entity.train.DSAWordDictItem;
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

import java.io.IOException;
import java.util.*;

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

            this.processSentence(dsaSentence, needSegment);
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
        //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
        this.preProcess(sentence, needSegment);

        //2: 识别单个连词和识别并列连词：不仅...而且
        this.findConnWordWithML(sentence);
        this.markConnAsInterOrCross(sentence);
        this.findParallelWord(sentence);

        //4: 将句子按照短语结构拆分成基本EDU
        //this.findArgumentInLine(sentence);
        boolean tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1：避免出现句子结构非法的Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return; }

        //5: 确定每个连词所涉及到的两个EDU来产生候选显式关系
        this.matchConnAndEDUforExpRelation(sentence);
        this.matchParallelConnAndEDUforExpRelation(sentence);

        //6: 根据获取到的信息，识别显式句间关系
        this.recognizeExpRelationType(sentence);

        //7: 识别可能存在的隐式句间关系
        this.getCandiateImpRelation(sentence);
        this.recognizeImpRelationType(sentence);
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
        this.markConnAsInterOrCross(sentence);

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

        sentence.setAnsjWordTerms(words);
        sentence.setContent( util.removeAllBlank(sentence.getContent()) );
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

        //b: 使用SVM模型判断每个候选的词是否是连词
        for( ConnVectorItem item : candidateTerms )
        {
            int label = connSvmMode.predict(item);

            if( label > 0 || item.getOccurInDict() > 100 )
            {
                DSAConnective connective = new DSAConnective( item.getContent(), sentence.getId() );

                connective.setPosTag(item.getPos());
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine(item.getPositionInLine());

                sentence.getConWords().add(connective);
            }
        }

        //设置分词后的结果
        sentence.setSegContent( segmentResult.trim() );
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
     * 3: 查找一段文字中的并列连词，比如：不仅...而且,
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
    //Segment a sentence into basic edus

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
     * 根据短语结构分析器来获取对应的短语结构。并以此来获取基本EDU
     * @param sentence
     */
    public boolean findArgumentWithPhraseParser(DSASentence sentence)
    {
        Tree phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );

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
     * 从树根开始，递归的构建EDU树，每层树根只能包含直接孩子节点的EDU，一直递归下去。
     * 此次获取到的只是简单的每个EDU在树形结构中的Root，后续需要再处理。
     **/
    private boolean getEDUsFromRoot(DSAEDU rootEDU)
    {
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
    private void matchConnAndEDUforExpRelation(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();

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
            String arg2Content = this.getArg2Content(curWord, connEDU);

            //e: 确定arg1包括的内容：这个就比较悲剧了。
            String arg1Content = this.getArg1Content(curWord, connEDU);

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
        String wContent = connWord.getContent();
        int argConnArg  = Resource.ConnectiveArgNum.get(wContent)[0];
        int connArgArg  = Resource.ConnectiveArgNum.get(wContent)[1];

        String arg1Content;

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
            String arg1Content = segSentContent.substring( curord.getBeginIndex1(), curord.getBeginIndex2() );
            String arg2Content = segSentContent.substring( curord.getBeginIndex2() );

            DSAInterRelation interRelation = new DSAInterRelation();
            interRelation.setArg1Content(arg1Content);
            interRelation.setArg2Content(arg2Content);
            interRelation.setSentID(sentence.getId());
            interRelation.setDsaConnective(curord);
        }
    }

    //-------------------------------3: Sense Recognize---------------------------

    /***识别具体的显式关系类型**/
    private void recognizeExpRelationType(DSASentence sentence)
    {
        for( DSAInterRelation curRel:sentence.getRelations() )
        {
            DSAConnective curConn = curRel.getDsaConnective();
            String wContent       = curConn.getContent();

            DSAWordDictItem item  = Resource.allWordsDict.get(wContent);
            double probality      = item.getMostExpProbality();
            String senseNO        = item.getMostExpProbalityRelNO();

            curRel.setRelType(Constants.EXPLICIT);
            curRel.setRelNO( util.convertOldRelIDToNew(senseNO) );
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
    public boolean getCrossExpRelInPara(DSAParagraph para, DSASentence curSentence, DSASentence nextSentence)
    {
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


    public static void main(String[] args) throws Exception
    {
        DiscourseParser discourseParser = new DiscourseParser();
        //discourseParser.parseRawFile("E:\\Program\\Java\\DiscourseParser\\data\\TestSentenceSplit.txt");
        //discourseParser.connectiveRecognize();

        String test     = "我 听到 过 很多 解释, 但是 我 觉得 我 从没有 看到 过 清晰 的 分析 。";
        String multiple = "中国 北方 重要 经济 城市 天津 与 俄罗斯 联邦 的 经贸 交往 目前 正 稳步 发展 ， 并 呈现 出 新 的 特色 。";
        String simple   = "浦东 开发 开放 是 一 项 振兴 上海 ， 建设 现代化 经济 、 贸易 、 金融 中心 的 跨世纪 工程 ， 因此 大量 出现 的 是 以前 不 曾 遇到 过 的 新 情况 、 新 问题 。";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);
        String twoSentence = "据 了解 ， 高行健 目前 已经 完成 了 一 部 新作 《 另 一 种 美 》 的 书稿 ， 并且 表示 能够 在 台湾 出版 。 高行健 １２月 １０号 将 在 瑞典 首都 斯德哥尔摩 举行 的 赠奖 仪式 当中 和 其他 诺贝尔 奖 得主 接受 瑞典 国王 卡尔 十六 世 古斯达夫 的 颁奖 。";

        boolean needSegment  = false;
        /**
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
         **/
        DiscourseParser dp = new DiscourseParser();
        dp.parseRawFile(twoSentence, needSegment);
    }
}

