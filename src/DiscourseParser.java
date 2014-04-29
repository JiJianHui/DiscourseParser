import common.Constants;
import common.LibSVMTest;
import common.util;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;
import entity.*;
import ltp.PhraseParser;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.dom4j.DocumentException;
import resource.Resource;
import recognize.word.MLVectorItem;

import java.io.IOException;
import java.util.*;

/**
 * ƪ�·�����������Ҫ��Ϊ�����Discourse Parser����ı�д�������Ե����Ͻ��б�д����
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2013-12-10 09:02
 * Email: jhji@ir.hit.edu.cn
 */
public class DiscourseParser
{
    private String text;    //����������
    private boolean needSegment; //�Ƿ���Ҫ�ִ�

    private ArrayList<DSASentence> sentences;   //���Ӽ���

    private LibSVMTest libsvmMode = null;   //����ʶ��ģ��

    private PhraseParser phraseParser;  //����ṹ����

    public DiscourseParser() throws DocumentException, IOException
    {
        needSegment = true;
        sentences   = new ArrayList<DSASentence>();

        System.out.println("[--Info--]Loading Resource: word and relation dictionary");

        //1: ���شʵ���Ϣ
        Resource.LoadExpConnectivesDict();

        //2: �Լ�������ָʾ�Ĺ�ϵ��Ϣ
        Resource.LoadWordRelDict();

        //3: ���ر�ע�õ�ѵ������
        Resource.LoadRawRecord();

        //4: ���ز������ʴʵ�
        Resource.LoadParallelWordDict();

        //5: ��������ʶ��svmģ��
        libsvmMode = new LibSVMTest();
        libsvmMode.loadModel();

        //6: ���ض���ṹ����
        this.phraseParser = new PhraseParser();

        //7: ��������Argλ��ͳ������
        Resource.LoadConnectiveArgs();
    }


    /**
     * ���һƪ���½��з�����������Ҫ��һƪ���½��з־䣬Ȼ���ÿ�����ӽ��д���
     * �������һƪ���µ����ݣ����ص��Ƿ�װ�õ�DSA����
     */
    private DSAParagraph parseRawFile(String fileContent, boolean needSegment) throws Exception
    {
        DSAParagraph paragraph = new DSAParagraph(fileContent);

        //0: �����¶�ȡ���õ������еľ��Ӽ���
        ArrayList<String> sentences = util.filtStringToSentence(fileContent);

        //1�������Ǿ��ڹ�ϵ���ж�
        for( int index = 0; index < sentences.size(); index++ )
        {
            String line = sentences.get(index);

            DSASentence dsaSentence = this.run(line, needSegment);

            dsaSentence.setId(index);

            paragraph.sentences.add(dsaSentence);
        }

        //2������ϵ��ʶ��

        return null;
    }

    public DSASentence run(String line, boolean needSegment) throws  IOException
    {
        boolean tempResult = true;
        DSASentence sentence = new DSASentence( line );

        //1: ���Ƚ���Ԥ�������еײ��NLP�����ִʡ����Ա�ע
        this.preProcess(sentence, needSegment);

        //2: ʶ�𵥸�����
        this.findConnWordWithML(sentence);

        //3: ʶ�������ʣ�����...����
        this.findParallelWord(sentence);

        //4: �����Ӱ��ն���ṹ��ֳɻ���EDU
        //this.findArgumentInLine(sentence);
        tempResult = this.findArgumentWithPhraseParser(sentence);

        //4.1��������־��ӽṹ�Ƿ���Sentence
        if( !tempResult ){ sentence.setIsCorrect(false); return sentence; }

        //5: ȷ��ÿ���������漰��������EDU
        this.matchConnectiveAndEDU(sentence);

        //6: ���ݻ�ȡ������Ϣ��ʶ����ʽ����ϵ
        this.recognizeExpRelationType(sentence);

        //7: ʶ����ܴ��ڵ���ʽ����ϵ
        this.recognizeImpRelationType(sentence);

        return sentence;
    }


    //-------------------------------0: �Ծ��ӽ��еײ㴦��--------------------------

    /**������ľ��ӽ��еײ��NLP������Ҫ�ǰ����˷ִʡ����Ա�ע�ȡ�**/
    private void preProcess(DSASentence sentence, boolean needSegment)
    {
        List<Term> words = null;

        if( !needSegment )
        {
            //����Ҫ�ִ�, �Ѿ��ֺôʵĽ������ô������Ҫ���Ա�ע�Ľ��
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
            //1�����зִ�
            words = NlpAnalysis.parse( util.removeAllBlank(sentence.getContent()) );
        }

        sentence.setContent( util.removeAllBlank(sentence.getContent()) );
        sentence.setAnsjWordTerms(words);
    }

    //-------------------------------1: Connective Recognize----------------------

    /**
     * 2: ʶ�𵥸����ʼ��뵽��ѡ������
     * @param sentence
     * @throws IOException
     */
    private void findConnWordWithML(DSASentence sentence) throws IOException
    {
        int beginIndex = 0;
        String segmentResult = "";

        ArrayList<MLVectorItem> candidateTerms = new ArrayList<MLVectorItem>();

        //a: ��Ծ����е�ÿ���ʽ��г�ȡ����
        for( Term wordItem : sentence.getAnsjWordTerms() )
        {
            String wContent   = wordItem.getName().trim();
            MLVectorItem item = new MLVectorItem(wContent);

            segmentResult += wContent + " ";

            //2: ���˵�������
            if( !Resource.ExpConnWordDict.containsKey(wContent) ) continue;

            //3����ȡ��������
            String wNextPos = "w", wPrevPos = "w";
            Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

            if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
            if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

            item.setPos( wordItem.getNatrue().natureStr );
            item.setPrevPos(wPrevPos);  item.setNextPos(wNextPos);

            //4����ȡ�ô��ھ����еĵ�λ��
            beginIndex = sentence.getContent().indexOf(wContent, beginIndex);
            item.setPositionInLine( beginIndex );

            //6����ȡ�ô������ʴʵ��г��ֵĴ���,�Լ�������
            double occurTime = 0.0, ambiguity = 1.0;

            if( Resource.allWordsDict.containsKey(wContent) )
            {
                DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

                occurTime = wordDictItem.getExpNum();
                ambiguity = wordDictItem.getMostExpProbality();
            }

            item.setAmbiguity(ambiguity);
            item.setOccurInDict(occurTime);

            //5: ���ñ�ǩ����Ϊ��Ԥ�⣬�����������ñ�ǩ, Ĭ�ϲ�������
            if(occurTime < 3) item.setLabel( Constants.Labl_Not_ConnWord );
            else item.setLabel( Constants.Labl_is_ConnWord );

            candidateTerms.add(item);
        }

        //b: ʹ��SVMģ���ж�ÿ����ѡ�Ĵ��Ƿ�������
        for( MLVectorItem item : candidateTerms )
        {
            int label = libsvmMode.predict(item);

            if( label > 0 )
            {
                DSAConnective connective = new DSAConnective( item.getContent() );

                connective.setPosTag(item.getPos());
                connective.setPrevPosTag(item.getPrevPos());
                connective.setNextPosTag(item.getNextPos());
                connective.setPositionInLine(item.getPositionInLine());

                sentence.getConWords().add(connective);
            }
        }

        //���÷ִʺ�Ľ��
        sentence.setSegContent( segmentResult.trim() );
    }


    /**
     * 3: ����һ�������еĲ������ʣ����磺����...����,
     * Ŀǰ��ʶ�𷽷�ֻ��ʶ����һ����������Ϊ���ܵĲ������ʡ������ճ��ִ����Ĵ�С���ж�
     * @param sentence
     */
    private void findParallelWord(DSASentence sentence)
    {
        int occurTimes      = -1;
        String sentContent  = sentence.getSegContent();

        ParallelConnective parallelConnective = null;

        for(Map.Entry<String, Integer> entry:Resource.ExpParallelWordDict.entrySet())
        {
            String parallelWord = entry.getKey();
            Integer numInDict = entry.getValue();

            //if( numInDict < 2 ) continue;

            //0: �жϴʵ��е�һ�����������Ƿ�������˸þ�����
            if( util.isParallelWordInSentence(parallelWord, sentContent) )
            {
                //1: ����������ʵ��������ֹ�ظ��϶�
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

                //2: ������ѡ���������б�. ֻ��ȡ���ִ������Ĳ�������
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
     * ʶ��һ����ϵ�漰����Argument�����Կ��ǵ���ʹ��argument�ı�ע��Ϣ������ѵ����
     * ���絫�ǵ�argument1һ����λ�ڵ��Ǻ��棬��argument2һ��λ��ǰ�档
     * ��ʽ�ıȽϺ��жϣ�ֱ���������
     * Ŀǰ��Ϊ����ǰ��Ķ�����argument
     * @param sentence
     */
    private void findArgumentInLine(DSASentence sentence)
    {
        //0: �����Ӳ��Ϊ�������嵥Ԫ
        sentence.seperateSentenceToEDU();

        //1��ȷ����ʽ���ʵ�argument����Ҫ�������ڣ�����ǰ�滹���ں��档
        ArrayList<DSAConnective> singleConnWords = sentence.getConWords();

        for(DSAConnective curWord:singleConnWords)
        {
            String word   = curWord.getContent();
            int posInLine = (int) curWord.getPositionInLine();

            DSAArgument arg1 = null, arg2 = null;

            int index  = 0;
            boolean isSoloWord = false;
            DSAEDU curEDU = null;

            //ȷ��arg1�������������ʵ�argument
            for( index = 0; index < sentence.getEdus().size(); index++ )
            {
                curEDU = sentence.getEdus().get(index);

                if( curEDU.getContent().indexOf(word)!= -1 )
                {
                    if( posInLine >= curEDU.getBeginIndex() && posInLine < curEDU.getEndIndex() )
                    {
                        //��Ҫ����������Ϊһ��������EDUʱ������
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

            //ȷ��arg2�������ʵ�����һ��argument,,�Լ��ж�EDUλ�ھ��׵����
            if( arg1 != null )
            {
                if( isSoloWord )
                    curEDU = sentence.getEdus().get( index == 0 ? 2: index - 1);
                else
                    curEDU = sentence.getEdus().get( index ==0 ? 1 : index - 1 );
                arg2 = new DSAArgument( curEDU.getContent(), sentence.getContent() );
            }

            //���ɶ�Ӧ�ľ���ϵ,����ϵ�ı�Ż���Ҫ��һ�������ж�
            DSAInterRelation relation = new DSAInterRelation();
            relation.setArg1Content(arg1.getContent());
            relation.setArg2Content(arg2.getContent());
            relation.setDsaConnective(curWord);
            relation.setRelType(Constants.ExpRelationType);

            sentence.getRelations().add(relation);
        }
    }


    /**
     * ���ݶ���ṹ����������ȡ��Ӧ�Ķ���ṹ�����Դ�����ȡ����EDU
     * @param sentence
     */
    public boolean findArgumentWithPhraseParser(DSASentence sentence)
    {
        Tree phraseResult = this.phraseParser.parseLine( sentence.getSegContent() );

        //1: �ݹ�Ĺ�������EDU����Ҫ�ǻ�ȡ�������ֵ�EDU������
        DSAEDU rootEDU = new DSAEDU(sentence.getContent(), sentence.getContent());
        rootEDU.setRoot(phraseResult.firstChild());

        boolean result = getEDUsFromRoot(rootEDU);

        if( result == false ) return false; //Ϊfalse��ʾ�þ��ӵĶ���ṹ�������ָþ��Ӳ��ǺϷ��ľ��ӡ�

        //2: ����ͷ�������EDU����Ҫ�ǽ�����EDU���Ͻ��з���, �������EDU����
        this.clearDuplictEDUs(rootEDU);

        //3: ��ȡ����EDU��Ϣ���з�װ����Ҫ����ȡ��EDU�����ݵ���Ϣ
        this.getEDUContent(rootEDU);

        //����EDU��Ϣ
        sentence.setRootEDU(rootEDU);

        //4: �������������ӵ�����EDU
        //this.matchConnectiveAndEDU(sentence);

        return  true;
    }


    /**
     * ��������ʼ���ݹ�Ĺ���EDU����ÿ������ֻ�ܰ���ֱ�Ӻ��ӽڵ��EDU��һֱ�ݹ���ȥ��
     * �˴λ�ȡ����ֻ�Ǽ򵥵�ÿ��EDU�����νṹ�е�Root��������Ҫ�ٴ���
     **/
    private boolean getEDUsFromRoot(DSAEDU rootEDU)
    {
        boolean  result = false;

        //�ݹ鴦��������ӽڵ�
        for( Tree child : rootEDU.getRoot().children() )
        {
            //�����ǵ��������ĺ��ӽڵ㣺�����ڣ���->Pron->N֮���
            if( child.depth() == child.size() - 1 ) continue;

            DSAEDU curEDU = new DSAEDU();
            curEDU.setRoot(child);
            curEDU.setParentEDU(rootEDU);
            curEDU.setDepth( rootEDU.getDepth() + 1 );

            boolean temp = getEDUsFromRoot( curEDU );

            if( temp ) rootEDU.getChildrenEDUS().add(curEDU);

            result = result || temp;
        }

        //�жϵ�ǰ�ڵ��Ƿ񹹳���һ���������EDU,�ݹ���ֹ����
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
     * �Ի���EDU���������˲�����Ҫ��Ϊ����������EDU����Ҫ�ǽ��������кϲ�����ֻ��һ��EDU��root�������Ϸ���
     */
    private void clearDuplictEDUs(DSAEDU rootEDU)
    {
        /**
        //�Ժ��ӽڵ����edu����
        for(DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            this.clearDuplictEDUs( childEDU );
        }

       //����ýڵ�ֻ��һ������EDU�ڵ㣬��ô���Խ��ú��ӽڵ�EDU���Ϸ�������ȡ���ú��ӽڵ��EDU����
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

        //ֻ��Ҷ�ӽڵ��EDU��������
        if(rootEDU.getChildrenEDUS().size() == 1)
        {
            if(rootEDU.getChildrenEDUS().get(0).getChildrenEDUS().size() == 0)
            {
                rootEDU.getChildrenEDUS().clear();
            }
        }
    }


    /**
     * ���ÿ��EDU�ڵ㣬��ȡ�ýڵ���������ַ���ֵ����������EDU�ͷ�EDU�ڵ��ֵ��
     */
    private String getEDUContent(DSAEDU rootEDU)
    {
        //���ȼ�����ڵ��content
        Tree   root    = rootEDU.getRoot();
        String content = getRootContent(root);

        rootEDU.setContent(content);

        //����������ӽڵ��contentֵ
        for( DSAEDU childEDU : rootEDU.getChildrenEDUS() )
        {
            String temp = getEDUContent(childEDU);
        }

        return content;
    }

    /**�ڶ���ṹ���л�ȡ��root�ڵ��µ��ַ���ֵ��**/
    private String getRootContent(Tree root)
    {
        String content = "";

        for( Label curWord : root.yield() ) content += curWord.value() + " ";

        if( content.length() > 1 ) content = content.substring(0, content.length() - 1 );

        return content;
    }


    /***
     * Ѱ����ʽ����������������EDU
     * @param sentence
     */
    private void matchConnectiveAndEDU(DSASentence sentence)
    {
        DSAEDU rootEDU = sentence.getRootEDU();

        ArrayList<DSAConnective> conns = sentence.getConWords();
        List<Tree> phraseLeaves = rootEDU.getRoot().getLeaves();

        //���ÿ�����ʶ���Ҫ���д���
        for( DSAConnective curWord : conns )
        {
            Tree curWordNode = null;
            String  wContent = curWord.getContent();

            //a: ���Ҹô��ڶ���ṹ�����������ڵĽڵ�
            int index = 0;
            for(Tree curNode : phraseLeaves)
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

            //b: ��connNode����ȥ����������,To-Do�������������� Modified Connective:����ԭ���ǣ�
            while( curWordNode.parent(rootEDU.getRoot()) != null )
            {
                if(curWordNode.parent(rootEDU.getRoot()).children().length == 1)
                {
                    curWordNode = curWordNode.parent(rootEDU.getRoot());
                }
                else break;
            }
            curWord.setConnNode(curWordNode);

            //c: ȷ����������������EDU������ֱ�������ĵ�һ��EDU�ڵ���Ϊ���ڵ�����������EDU
            Tree parentNode = curWordNode.parent( rootEDU.getRoot() );

            int num = 0, error = 0;
            while( parentNode != null && !this.isAEDURoot(parentNode, rootEDU) )
            {
                parentNode = parentNode.parent(rootEDU.getRoot());
                if( parentNode.nodeString().equalsIgnoreCase("np") || num++ > 3 ) {
                    error = 1;break;
                }
            }

            //���������û�и������κ�һ����Ч��EDU��
            if( error == 1 || parentNode == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s ParentNode In ");
                System.out.print(sentence.getContent());
                continue;
            }

            DSAEDU arg2EDU = this.findTreeEDU(parentNode, rootEDU);

            if( arg2EDU == null )
            {
                System.out.print("[--Error--] Not Found " + wContent + "'s EDU In ");
                System.out.print(sentence.getContent());
                continue;
            }

            DSAInterRelation interRelation = new DSAInterRelation();

            //d: ȷ��arg2���������ݣ���arg2EDU��ʼ�����ҵ���������
            String arg2Content = this.getArg2Content(curWord, arg2EDU);

            //e: ȷ��arg1���������ݣ�����ͱȽϱ����ˡ�
            String arg1Content = this.getArg1Content(curWord, arg2EDU);
        }

        Iterator<DSAConnective> iter = conns.iterator();

        while( iter.hasNext() )
        {
            DSAConnective cur = iter.next();

            if(cur.getArg1EDU() == null || cur.getArg2EDU() == null )
            {
                iter.remove();
            }
        }
    }

    /**��ȡһ�������ڶ���ṹ������������EDU�ڵ㡣���շ��ص��Ǹ�������ֱ��������EDU�ڵ�**/
    private DSAEDU getConnectiveNode(DSAConnective curWord, DSAEDU rootEDU)
    {
        //a: ���Ҹô��ڶ���ṹ�����������ڵĽڵ�
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

        if( curWordNode == null ) return null;

        //b: ��connNode����ȥ����������,To-Do�������������� Modified Connective:����ԭ���ǣ�
        while( curWordNode.parent(rootEDU.getRoot()) != null )
        {
            if(curWordNode.parent(rootEDU.getRoot()).children().length == 1)
            {
                curWordNode = curWordNode.parent(rootEDU.getRoot());
            }
            else break;
        }

        //c: ȷ����������������EDU������ֱ�������ĵ�һ��EDU�ڵ���Ϊ���ڵ�����������EDU
        int num = 0, error = 0;
        Tree parentNode = curWordNode.parent( rootEDU.getRoot() );

        while( parentNode != null && !this.isAEDURoot(parentNode, rootEDU) )
        {
            parentNode = parentNode.parent(rootEDU.getRoot());
            if( parentNode.nodeString().equalsIgnoreCase("np") || num++ > 3 ) {
                error = 1;break;
            }
        }

        //���������û�и������κ�һ����Ч��EDU��
        if( error == 1 || parentNode == null )
        {
            System.out.print("[--Error--] Not Found " + wContent + "'s ParentNode In ");
            System.out.print(rootEDU.getContent());
            return null;
        }

        return this.findTreeEDU(parentNode, rootEDU);
    }

    /***
     * ȷ��һ�����ʵ�Arg2��Χ����ֱ���ϲ�EDU���Һ��ӽڵ㼯��. ���ص���arg2���ַ������ݣ����տո�ָ�
     * @param connWord������
     * @param arg2EDU������������ֱ���ϲ�EDU
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
            //����arg2�Ŀ�ʼ��
            boolean find  = false;
            Tree    root  = arg2EDU.getRoot();

            for( Tree curChild : root.children() )
            {
                //�жϵ�ǰ���ӽڵ��Ƿ��ǿ�ʼ�ڵ�
                if( !find )
                {
                    if( curChild == connWord.getConnNode() ){
                        find = true;
                    }
                }
                else
                {
                    //�ӿ�ʼ�����ұߵĶ�����arg2,ֱ�����ұߵ�EDU,������������ŵ�ѭ�����棬��arg2�����������ʽڵ�
                    connWord.getArg2Nodes().add(curChild);

                    arg2Content += this.getRootContent(curChild) + " ";

                    //��������һ��EDU��ʱ���˳���
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

    /*** �ж�һ���䷨�������еĽڵ��Ƿ���EDU�ڵ㡣*/
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

    /**����һ��Tree root ��λ�ڵ�EDU�ڵ�**/
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
     * ��������ΪArg-Conn-Arg���������漰������һ��EDU��
     * ���ص���ͬһArg2EDU�ڵ��µ��ֵܽڵ㣬������Arg2���ڵ��е��ֵܽڵ�
     **/
    private String getArgConnArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //���arg2EDU������Ϊһ��EDU����arg1EDUΪarg2EDU���ֵ�EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU = arg2EDU.getParentEDU();
            int parentChildSize = parentEDU.getChildrenEDUS().size();

            //���Ҷ�Ӧ��arg1EDUλ�á�
            for(int index = 0; index < parentChildSize; index++)
            {
                if(parentEDU.getChildrenEDUS().get(index) == arg2EDU && index > 0)
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index - 1);
                    break;
                }
            }
            //û���ҵ�arg1EDU�����Ϊ��arg2EDUΪparent�ĵ�һ�� || arg2EDU��parentֻ��һ��
            //�����Arg-Conn-Arg���͵�û���ҵ�����϶�����Ϊarg2EDU��parent�ĵ�һ������
            if( arg1EDU == null )
            {
                //��ʱ�������Ϊarg2EDU���Ǹ�����EDU�ˣ���ôֱ�ӷ���null, �����Ǹ��׽ڵ��ֱ���Һ���
                if( arg2EDU.getParentEDU() != null && parentChildSize > 1 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(1);
                }
            }
        }
        else
        {
            //���arg2EDU�����edu��ֹһ��������Arg-Conn-Arg�����£�arg1EDU�����������
            //1��λ��arg2EDU�е�һ������EDU�С�
            //2: λ��arg2EDU���׽ڵ��е�һ��
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
                //����һ����г�ȡһ��
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
                    //����ϲ�Ҳû��
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

    /***��ȡ����ΪConnArgArg��arg1�ڵ㡣**/
    private String getConnArgArgContent(DSAConnective connWord, DSAEDU arg2EDU)
    {
        DSAEDU arg1EDU     = null;
        String arg1Content = "";

        //���arg2EDU������Ϊһ��EDU����arg1EDUΪarg2EDU���ֵ�EDU
        if( arg2EDU.getChildrenEDUS().size() <= 1 )
        {
            DSAEDU parentEDU    = arg2EDU.getParentEDU();
            int parentChildSize = parentEDU.getChildrenEDUS().size() - 1;

            //���Ҷ�Ӧ��arg1EDUλ�á�
            for( int index = 0; index < parentChildSize; index++ )
            {
                if( parentEDU.getChildrenEDUS().get(index) == arg2EDU )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(index + 1);
                    break;
                }
            }

            //�����Conn-Arg-Arg���͵�û���ҵ�����϶�����Ϊarg2EDU��parent�����һ������
            if( arg1EDU == null )
            {
                //��ʱ�������Ϊarg2EDU���Ǹ�����EDU�ˣ���ôֱ�ӷ���null, �����Ǹ��׽ڵ��ֱ������
                if( arg2EDU.getParentEDU() != null && parentChildSize >= 2 )
                {
                    arg1EDU = parentEDU.getChildrenEDUS().get(parentChildSize - 2);
                }
            }
        }
        else
        {
            //���arg2EDU�����edu��ֹһ��������Conn-Arg-Arg�����£�arg1EDU�����������
            //1��λ��arg2EDU�е�һ������EDU�С�
            //2: λ��arg2EDU���׽ڵ��е�һ��
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
                //����һ����г�ȡһ��
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
                    //����ϲ�Ҳû��. ����ΪcurWord��arg2EDU�Ǹ���arg2EDU�����ұߵĺ���
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

    //-------------------------------3: Sense Recognize---------------------------

    /***ʶ��������ʽ��ϵ����**/
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
        //����ʶ���������ʽ������ʶ����ʽ��ϵ
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


    /***ʶ����ܴ��ڵ���ʽ����ϵ**/
    private void recognizeImpRelationType(DSASentence sentence)
    {
        // �ڻ�����EDU�Ļ����Ͽ�ʼ��������ƥ��
        DSAEDU rootEDU = sentence.getRootEDU();

        //��ȡ����EDU


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

        String test     = "�� ���� �� �ܶ� ����, ���� �� ���� �� ��û�� ���� �� ���� �� ���� ��";
        String multiple = "�й� ���� ��Ҫ ���� ���� ��� �� ����˹ ���� �� ��ó ���� Ŀǰ �� �Ȳ� ��չ �� �� ���� �� �� �� ��ɫ ��";
        String simple   = "�ֶ� ���� ���� �� һ �� ���� �Ϻ� �� ���� �ִ��� ���� �� ó�� �� ���� ���� �� ������ ���� �� ��� ���� ���� �� �� ��ǰ �� �� ���� �� �� �� ��� �� �� ���� ��";
        //DSASentence dsaSentence = new DSASentence(test);
        //discourseParser.analysisSentenceConnective(dsaSentence);
        //discourseParser.run(test);

        boolean needSegment  = false;
        DSASentence sentence = new DSASentence(simple);

        discourseParser.findConnWordWithML(sentence);
        discourseParser.findArgumentWithPhraseParser(sentence);

        System.out.println(sentence.getConWords().get(0).getArg2EDU().getContent());
    }
}

