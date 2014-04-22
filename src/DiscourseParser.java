import common.Constants;
import common.LibSVMTest;
import common.util;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.Tree;
import entity.*;
import ltp.PhraseParser;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.DocumentException;
import resource.Resource;
import train.MLVectorItem;

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
    private String text;    //待分析文章
    private boolean needSegment; //是否需要分词

    private ArrayList<DSASentence> sentences;   //句子集合

    private LibSVMTest libsvmMode = null;   //连词识别模型

    private PhraseParser phraseParser;  //短语结构分析

    public DiscourseParser() throws DocumentException, IOException
    {
        needSegment = true;
        sentences   = new ArrayList<DSASentence>();

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

        //6: 加载短语结构分析
        this.phraseParser = new PhraseParser();

        //7: 加载连词Arg位置统计数据
        Resource.LoadConnectiveArgs();
    }


    public DSASentence run(String line, boolean needSegment) throws  IOException
    {
        //1: 针对一句话识别句内关系
        DSASentence sentence = new DSASentence( line );

        //2: 识别单个连词
        this.findConnWordWithML(sentence, needSegment);

        //3: 识别并列连词：不仅...而且
        this.findParallelWord(sentence);

        //4: 识别Argument，目前采用的方法是简单的逗号分隔
        //this.findArgumentInLine(sentence);
        this.findArgumentWithPhraseParser(sentence);

        //5: 确定候选连词和EDU的关系
        this.matchConnectiveAndEDU(sentence);


        //6: 根据获取到的信息，识别关系编号
        this.recognizeExpRelationType(sentence);

        //7: 识别隐式句间关系
        this.recognizeImpRelationType(sentence);

        return sentence;
    }

    //-------------------------------1: Connective Recognize----------------------

    /**
     * 2: 识别单个连词加入到候选连词中
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence, boolean needSegment) throws IOException
    {
        List<Term> words = null;

        //不需要分词, 已经分好词的结果，那么我们需要词性标注的结果
        if(needSegment == false)
        {
            List<String> lists = Arrays.asList(sentence.getContent().split(" "));
            words = NatureRecognition.recognition(lists, 0) ;

            for(int index = 0; index < words.size(); index++)
            {
                Term curTerm = words.get(index);
                if(index > 0) curTerm.setFrom( words.get(index - 1) );
                if(index < words.size() - 1) curTerm.setTo( words.get(index+1) );
            }
        }
        else
        {
            //1：进行分词
            words = NlpAnalysis.parse( util.removeAllBlank(sentence.getContent()) );
        }

        sentence.setContent( util.removeAllBlank(sentence.getContent()) );
        sentence.setAnsjWordTerms(words);

        int beginIndex = 0;
        String segmentResult = "";

        ArrayList<MLVectorItem> candidateTerms = new ArrayList<MLVectorItem>();

        for(Term wordItem : words)
        {
            String wContent   = wordItem.getName().trim();
            MLVectorItem item = new MLVectorItem(wContent);

            segmentResult += wContent + " ";

            //2: 过滤掉噪音词
            if( !Resource.ExpConnWordDict.containsKey(wContent) ) continue;

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

            candidateTerms.add(item);
        }

        for(MLVectorItem item : candidateTerms)
        {
            int label = libsvmMode.predict(item);

            if( label > 0 )
            {
                DSAConnective connective = new DSAConnective( item.getContent() );
                connective.setPosTag( item.getPos() );
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine( item.getPositionInLine() );

                sentence.getConWords().add(connective);
            }
        }

        //设置分词后的结果
        sentence.setSegContent( segmentResult.trim() );
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

    //-------------------------------2: EDU Recognize-----------------------------

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


    /**
     * 根据短语结构分析器来获取对应的短语结构。并以此来获取基本EDU
     * @param sentence
     */
    public void findArgumentWithPhraseParser(DSASentence sentence)
    {
        Tree phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );

        //1: 递归的构建基本EDU，主要是获取各个部分的EDU的树根
        DSAEDU rootEDU = new DSAEDU(sentence.getContent(), sentence.getContent());
        rootEDU.setRoot(phraseResult.firstChild());

        boolean result = getEDUsFromRoot(rootEDU);

        //2: 清理和泛化单链EDU，主要是将单链EDU向上进行泛化, 将多余的EDU清理
        this.clearDuplictEDUs(rootEDU);

        //3: 提取各个EDU信息进行封装，主要是提取该EDU的内容等信息
        this.getEDUContent(rootEDU);

        //保存EDU信息
        sentence.setRootEDU(rootEDU);

        //4: 查找连词所连接的两个EDU
        this.matchConnectiveAndEDU(sentence);
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
            //不考虑单链泛化的孩子节点
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU );

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;
        }

        //判断当前节点是否构成了一个最基本的EDU
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
        String content = "";
        Tree   root    = rootEDU.getRoot();

        for( Label curWord : root.yield() ) content += curWord.value() + " ";

        content = content.substring(0, content.length() - 1 );
        rootEDU.setContent(content);

        //计算各个孩子节点的content值
        for( DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            String temp = getEDUContent(childEDU);
        }

        return content;
    }


    /***
     * 寻找显式连词所关联的两个EDU
     * @param sentence
     */
    private void matchConnectiveAndEDU(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();
        ArrayList<DSAConnective> conns = sentence.getConWords();

        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        //针对每个连词都需要进行处理
        for( DSAConnective curWord : conns )
        {
            String  wContent = curWord.getContent();
            Tree curWordNode = null;

            //a: 查找该词在短语结构分析树中所在的节点
            int index = 0;
            for(Tree curNode : phraseLeaves)
            {
                String curWordContent = curNode.nodeString();

                if( curWordContent.equalsIgnoreCase(wContent) )
                {
                    if( Math.abs( curWord.getPositionInLine()-index ) < 2 )
                    {
                        curWordNode = curNode; break;
                    }
                }
                index = index + curWordContent.length();
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
            Tree parentNode = curWordNode.parent(rootEDU.getRoot());

            int num = 0, error = 0;
            while( parentNode != null && !this.isAEDURoot(parentNode, rootEDU) )
            {
                parentNode = parentNode.parent(rootEDU.getRoot());
                if( num++ > 2 ) {
                    error = 1;break;
                }
            }

            //如果该连词没有附着在任何一个有效的EDU上
            if( error == 1 ) return;

            DSAEDU arg2EDU = this.findTreeEDU(parentNode, rootEDU);

            //d: 确定arg2包括的内容：从arg2EDU开始，向右的所有内容
            this.getArg2Content(curWord, arg2EDU);

            //e: 确定arg1包括的内容：这个就比较悲剧了。
            this.getArg1Content(curWord, arg2EDU);
        }

        Iterator<DSAConnective> iter = conns.iterator();

        while(iter.hasNext())
        {
            DSAConnective cur = iter.next();

            if(cur.getArg1EDU() == null || cur.getArg2EDU() == null ) iter.remove();
        }

    }


    /***
     * 确定一个连词的Arg2范围，最直接上层EDU的右孩子节点集合
     * @param connWord：连词
     * @param arg2EDU：连词所属的直接上层EDU
     */
    private void getArg2Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        if( arg2EDU.getChildrenEDUS().size() < 2 )
        {
            connWord.getArg2Nodes().add( arg2EDU.getRoot() );
            connWord.setArg2EDU( arg2EDU );
        }
        else
        {
            //查找arg2的开始点
            Tree    root  = arg2EDU.getRoot();
            boolean find  = false;

            for( Tree curChild : root.children() )
            {
                //判断当前孩子节点是否是开始节点
                if( find == false )
                {
                    if( curChild == connWord.getConnNode() )
                    {
                        find = true;
                        continue;
                    }
                }

                //从开始到最右边的都当做arg2,直到最右边的EDU,如果将该行语句放到循环上面，则arg2将不包括连词节点
                if( find == true )
                {
                    connWord.getArg2Nodes().add(curChild);

                    //在碰见第一个EDU的时候退出。
                    if( this.isAEDURoot(curChild, arg2EDU) )
                    {
                        DSAEDU temp = this.findTreeEDU(curChild, arg2EDU);
                        connWord.setArg2EDU( temp );
                        break;
                    }
                }
            }
            /**
            for(DSAEDU curEDU : arg2EDU.getChildrenEDUS() )
            {
            }
             **/
        }
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


    private void getArg1Content(DSAConnective connWord, DSAEDU arg2EDU)
    {
        String wContent = connWord.getContent();
        int argConnArg  = Resource.ConnectiveArgNum.get(wContent)[0];
        int connArgArg  = Resource.ConnectiveArgNum.get(wContent)[1];

        if( argConnArg > connArgArg )
        {
            this.getArgConnArgContent(connWord, arg2EDU);
        }
        else
        {
            this.getConnArgArgContent(connWord, arg2EDU);
        }
    }


    /**查找类型为Arg-Conn-Arg的连词所涉及到的另一个EDU**/
    private DSAEDU getArgConnArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU   = null;
        DSAEDU parentEDU = null;

        //如果arg2EDU单独成为一个EDU，则arg1EDU为arg2EDU的兄弟EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            parentEDU = arg2EDU.getParentEDU();

            //查找对应的arg1EDU位置。
            for(int index = 0; index < parentEDU.getChildrenEDUS().size(); index++)
            {
                if(parentEDU.getChildrenEDUS().get(index) == arg2EDU && index > 0)
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index - 1);
                    break;
                }
            }

            //如果是Arg-Conn-Arg类型的没有找到，则肯定是因为arg2EDU是parent的第一个孩子
            if( arg1EDU == null )
            {
                //此时：如果因为arg2EDU就是根部的EDU了，那么直接返回null, 否则是父亲节点的直接右孩子
                if( arg2EDU.getParentEDU() != null )
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
                        if( curEDU == arg2EDU )
                        {
                            arg1EDU = prev; break;
                        }
                        else
                        {
                            prev = curEDU;
                        }
                    }
                    //如果上层也没有
                    if( arg1EDU == null ) arg1EDU = arg2EDU.getChildrenEDUS().get(1);
                }
            }
        }

        if( arg1EDU != null ) connWord.setArg1EDU(arg1EDU);
        return arg1EDU;
    }

    /***获取类型为ConnArgArg的arg1节点。**/
    private DSAEDU getConnArgArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU = null;
        DSAEDU parentEDU = null;

        //如果arg2EDU单独成为一个EDU，则arg1EDU为arg2EDU的兄弟EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            parentEDU = arg2EDU.getParentEDU();

            //查找对应的arg1EDU位置。
            for(int index = 0; index < parentEDU.getChildrenEDUS().size() - 1; index++)
            {
                if(parentEDU.getChildrenEDUS().get(index) == arg2EDU )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            //如果是Conn-Arg-Arg类型的没有找到，则肯定是因为arg2EDU是parent的最后一个孩子
            if( arg1EDU == null )
            {
                //此时：如果因为arg2EDU就是根部的EDU了，那么直接返回null, 否则是父亲节点的直接左孩子
                if( arg2EDU.getParentEDU() != null )
                {
                    System.out.println(connWord.getContent());
                    System.out.println(parentEDU.getChildrenEDUS().size());
                    arg1EDU = parentEDU.getChildrenEDUS().get(parentEDU.getChildrenEDUS().size() - 2);
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
                    if( arg1EDU == null ) arg1EDU = arg2EDU.getChildrenEDUS().get(arg2EDU.getChildrenEDUS().size() - 2);
                }
            }
        }

        if( arg1EDU != null ) connWord.setArg1EDU( arg1EDU );

        return arg1EDU;
    }

    //-------------------------------3: Sense Recognize---------------------------

    /***识别具体的显式关系类型**/
    private void recognizeExpRelationType(DSASentence sentence)
    {
        /**
        for( DSARelation candiatateRel : sentence.getRelations() )
        {
            if( candiatateRel.getDsaConnective() == null ) continue;

            String connWord = candiatateRel.getDsaConnective().getContent();
            String relNO    = Resource.connectiveRelationDict.get(connWord);

            if( relNO == null ) relNO = Constants.DefaultRelNO;

            candiatateRel.setRelNO( relNO );
        }
         **/
        //根据识别出来的显式连词来识别显式关系
        for(DSAConnective curWord : sentence.getConWords() )
        {
            if( curWord.getArg1EDU() == null || curWord.getArg2EDU() == null )
            {
                continue;
            }

            String wContent = curWord.getContent();
            DSAWordDictItem item = Resource.allWordsDict.get(wContent);

            double mostExpProbality     = item.getMostExpProbality();
            String mostExpProbalityType = item.getMostExpProbalityRelNO();

            mostExpProbalityType = util.convertOldRelIDToNew(mostExpProbalityType);

            curWord.setExpRelProbality(mostExpProbality);
            curWord.setExpRelType(mostExpProbalityType);
        }
    }


    /***识别可能存在的隐式句间关系**/
    private void recognizeImpRelationType(DSASentence sentence)
    {
        // 在基本的EDU的基础上开始进行两两匹配

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

        String test     = "我 听到 过 很多 解释, 但是 我 觉得 我 从没有 看到 过 清晰 的 分析 。";
        String multiple = "中国 北方 重要 经济 城市 天津 与 俄罗斯 联邦 的 经贸 交往 目前 正 稳步 发展 ， 并 呈现 出 新 的 特色 。";
        String simple   = "浦东 开发 开放 是 一 项 振兴 上海 ， 建设 现代化 经济 、 贸易 、 金融 中心 的 跨世纪 工程 ， 因此 大量 出现 的 是 以前 不 曾 遇到 过 的 新 情况 、 新 问题 。";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);

        boolean needSegment  = false;
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence, needSegment);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
    }
}

