package train.word;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.ansj.domain.Term;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.*;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * ʹ�û���ѧϰ������ѵ��һ������ʶ��ģ��,,Ŀǰ��Ե���p3����ϵ.��Ҫ��������ȡ��������ѵ������TrainData
 * User: Ji JianHui
 * Time: 2014-03-05 19:45
 * Email: jhji@ir.hit.edu.cn
 */
public class connFeatureExtract
{
    private ArrayList<ConnVectorItem> items;
    private ArrayList<ConnVectorItem> filterItems;


    private ArrayList<ArrayList<String>> notLabeledWords = new ArrayList<ArrayList<String>>();

    public connFeatureExtract() throws DocumentException, IOException
    {
        //������Դ
        Resource.LoadRawRecord();
        //Resource.LoadStopWords();
        Resource.LoadExpConnectivesDict();
        Resource.LoadWordRelDict();
        //Resource.LoadLtpXMLResultSentID();
    }

    /**
     * ���°汾�ı�ע�����г�ȡ��������������libsvmѵ�����ݡ�TrainData
     * Ϊ�˹���ģ��ֻ��Ҫ����һ�μ��ɡ�
     */
    public void extractFeatures() throws IOException, DocumentException
    {
        this.items       = new ArrayList<ConnVectorItem>();
        this.filterItems = new ArrayList<ConnVectorItem>();

        String path = Constants.Libsvm_Origin_Data_Path;

        //1����ȡ����������
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(this.items);

        //2���Գ�ȡ�����������д�������
        this.filterItem( this.items, this.filterItems );

        //3��������ת��ΪlibsvmҪ��ĸ�ʽ
        this.convertToLibsvmData( this.filterItems );

    }

    /**
     * ���ݱ�ע������������ʶ��ѵ������.�˷������ɵ���ԭʼ��ѵ�����ݣ���û�а��ջ���ѧϰ��Ҫ������ͳһ��ʽ�����ݡ�
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainDataWithAnsj(ArrayList<ConnVectorItem> datas)
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        ArrayList<SenseRecord> records= new ArrayList<SenseRecord>();
        records.addAll(Resource.Raw_Train_Annotation_p2);
        records.addAll(Resource.Raw_Train_Annotation_p3);

        int size = records.size();
        for(int index = 0; index < size; index++)
        {
            SenseRecord record = records.get(index);

            //0: Ԥ������м򵥹��˵�ƽ������
            String  connWord    = record.getConnective().trim();
            String  sentContent = record.getText().trim();

            if( connWord.contains(";") ) continue;
            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            //�ж��Ƿ���ͬһ��record,��ֹ����
            SenseRecord prev = index > 0 ? records.get(index - 1) : null;
            SenseRecord next = index < size-1 ? records.get(index + 1) : null;

            boolean isSameWithPrev = prev != null && this.isTheSameRecord(record, prev);
            boolean isSameWithNext = next != null && this.isTheSameRecord(record, next);

            String prevConn = (prev == null) ? null : prev.getConnective();
            String nextConn = (next == null) ? null : next.getConnective();

            //1���ִʺʹ��Ա�ע,ע��sentContent�Ƿֺôʵ�
            List<Term> words = util.getSegmentedSentencePosTag(sentContent);

            //2����ȡ��Ӧ��ÿ���ʵ�����
            int beginIndex = 0, periodIndex = -1;
            if( record.getfPath().contains(".p2") ) periodIndex = sentContent.indexOf("��");

            for( Term wordItem : words )
            {
                ConnVectorItem item = this.getFeatureLine(wordItem, sentContent);

                if( item == null ) continue;

                //����������ע�͵�������û������:���ʱ�־��λ��
                if( item.getContent().equalsIgnoreCase(connWord) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }

                beginIndex = beginIndex + wordItem.getName().length();
                if( periodIndex != -1 && beginIndex > periodIndex){
                    beginIndex = beginIndex - periodIndex;
                }
                item.setPositionInLine(beginIndex);

                //�ж��Ƿ��Ǳ�������˵�����
                if( prevConn != null && isSameWithPrev && item.getContent().equals(prevConn) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                if( nextConn != null && isSameWithNext && item.getContent().equals(nextConn) ){
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                //�жϸ�������Ϊ���ʺͲ���Ϊ���ʵı���
                Integer asConnNum    = Resource.allWordsDict.get(item.getContent()).getExpNum();
                Integer notAsConnNum = Resource.NotAsDiscourseWordDict.get(item.getContent());

                double ambugity = ( notAsConnNum == null )? 0 : notAsConnNum*1.0 / (notAsConnNum+asConnNum);

                if( ambugity < 0.03 && asConnNum > 100 ) item.setLabel( Constants.Labl_is_ConnWord );

                datas.add(item);
            }
        }
    }

    /**�ж�������¼�Ƿ���ͬһ����¼����Ҫ�Ǹ���������¼����ʶ��������**/
    public boolean isTheSameRecord(SenseRecord sour, SenseRecord dest)
    {
        boolean result  = false;
        String sourText = util.removeAllBlankAndProun( sour.getText() );
        String destText = util.removeAllBlankAndProun( dest.getText() );
        int    sourLen  = sourText.length(), destLen = destText.length();

        //ȷ��dest�������λ
        if( destLen < sourLen )
        {
            String temp = destText;
            destText    = sourText;
            sourText    = temp;
        }
        sourLen = sourText.length();

        //�ж��Ƿ���ͬһ����¼������ͬһ����¼��������¼
        if( destText.equalsIgnoreCase(sourText) ) {
            result = true;
        }else if( destText.contains(sourText) ){
            result = true;
        }else{
            //���������ı������ƶ�
            int sameNum = util.countSameCharatersNum(sourText, destText);
            if( Math.abs(sameNum - sourLen) < 3 ) result = true;
        }

        return result;
    }

    /**��ȡһ�������е�ĳһ���ض����ʵ�������**/
    public ConnVectorItem getFeatureLine(Term wordItem, String sentContent)
    {
        //1�����ÿ���ʣ������жϺʹ���
        String wContent = wordItem.getName().trim();

        //2: ���˵��������Լ��������ʳ��ֵĴ������й���
        if( !Resource.allWordsDict.containsKey(wContent) ) return null;

        ConnVectorItem item = new ConnVectorItem(wContent);

        //3�����ô�������
        Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

        item.setPos( wordItem.getNatrue().natureStr );
        item.setPrevPos( wPrevTerm == null ? "w" : wPrevTerm.getNatrue().natureStr );
        item.setNextPos( wNextTerm == null ? "w" : wNextTerm.getNatrue().natureStr );

        //������Ϊ���ʳ��ִ����Ͳ���Ϊ���ʳ��ִ���
        Integer connNum    = Resource.allWordsDict.get(wContent).getExpNum();
        Integer notConnNum = Resource.NotAsDiscourseWordDict.get(wContent);

        //if( connNum < 5 && notConnNum > 5 * connNum ) return null;
        item.setConnNum( connNum == null ? 0 : connNum);
        item.setNotConnNum( notConnNum == null ? 0 : notConnNum );

        //6���жϸô������ʴʵ��г��ֵĴ���,�Լ�������
        DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

        item.setOccurInDict( wordDictItem.getExpNum() );
        item.setAmbiguity( wordDictItem.getMostExpProbality() );

        return item;
    }

    /**
     * ���ݱ�ע������������ʶ��ѵ������.�˷������ɵ���ԭʼ��ѵ�����ݣ���û�а��ջ���ѧϰ��Ҫ������ͳһ��ʽ�����ݡ�
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainData(ArrayList<ConnVectorItem> datas) throws DocumentException, IOException
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        //LTP�ִʽ������
        //���ÿ����¼������ltp�������,����ltp����ȡ�����Լ�
        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            //���Ҷ�Ӧ��ltp�������������ļ�λ��
            String content   = record.getText().trim();
            Integer resultId = Resource.Ltp_Xml_Result_SentID_P3.get( content );

            if( resultId == null ) continue;

            String xmlPath = Constants.Ltp_XML_Result_P3 + "\\sent" + resultId + ".xml";

            //�ü�¼�����ͺ����е�����
            boolean  isExplicit = record.getType().equalsIgnoreCase(Constants.EXPLICIT);
            String   connWord   = record.getConnective();
            //String[] connWords  = record.getConnective().split( Constants.Parallel_Word_Seperator );

            //-------------------------------------------------------
            //����ltp���, ��ÿ���ʶ����Ϊһ��item��������
            Document domObj  = util.parseXMLFileToDOM(xmlPath, "gbk");
            Element rootNode = domObj.getRootElement();
            Element noteNode = rootNode.element("note");

            if( noteNode.attribute("pos").getText().equalsIgnoreCase("n") )    continue ;
            if( noteNode.attribute("parser").getText().equalsIgnoreCase("n") ) continue;

            Element paraNode  = rootNode.element("doc").element("para");

            //-----------------------------------------------
            //���ÿ���ʣ���ȡ��Ϣ����װΪһ������
            for(Iterator iterator = paraNode.elementIterator(); iterator.hasNext();)
            {
                Element sentNode    = (Element) iterator.next();
                String  sentContent = sentNode.attribute("cont").getText();

                int        beginIndex = 0;     //Ϊ�˲���һ�����ڸþ����е�λ�ã�ʹ��beginIndex����ֹͬ���Ĵ�
                String       prevPos  = "wp";
                ConnVectorItem prevItem = null; //����������һ��������nextPos��

                for( Iterator ite = sentNode.elementIterator(); ite.hasNext(); )
                {
                    Element wordNode = (Element)ite.next();
                    String nodeName  = wordNode.getName();

                    //���ֵ�ÿ���ʶ�������
                    if( nodeName.equals("word") )
                    {
                        String wContent = wordNode.attribute("cont").getText();
                        String wPosTag  = wordNode.attribute("pos").getText();
                        String wRelate  = wordNode.attribute("relate").getText();

                        ConnVectorItem item = new ConnVectorItem(wContent);

                        //3�����ô�������
                        item.setPos(wPosTag);
                        item.setRelateTag(wRelate);
                        item.setPrevPos(prevPos);
                        prevPos = wPosTag;

                        //������һ��item��nextPos�Լ������nextPos
                        if( prevItem != null ) prevItem.setNextPos(wPosTag);
                        prevItem = item;

                        //4���жϸô������������Ǹ�����. Ŀǰ�������ɵ����ʵ������������Ժ�ʹ��ģ��ʶ��
                        if( isExplicit && connWord.equalsIgnoreCase(wContent) )
                        {
                            item.setLabel( Constants.Labl_is_ConnWord );
                        }

                        //5���жϸô��ھ����еĵ�λ��
                        beginIndex = sentContent.indexOf(wContent, beginIndex);
                        item.setPositionInLine( beginIndex );

                        //6���жϸô������ʴʵ��г��ֵĴ���,�Լ�������
                        double occurTime = 0.0, ambiguity = 1.0;

                        if( Resource.allWordsDict.containsKey(wContent) )
                        {
                            DSAWordDictItem wordItem = Resource.allWordsDict.get(wContent);

                            occurTime = wordItem.getExpNum();
                            ambiguity = wordItem.getMostExpProbality();
                        }

                        item.setAmbiguity(ambiguity);
                        item.setOccurInDict(occurTime);

                        datas.add(item);
                    }
                }
                //�������һ��item��nextPosTage
                prevItem.setNextPos("wp");
            }
        }
    }

    public void filterItem(ArrayList<ConnVectorItem> datas, ArrayList<ConnVectorItem> results) throws IOException
    {
        HashMap<String, Integer> asConnWords    = new HashMap<String, Integer>();
        HashMap<String, Integer> notAsConnWords = new HashMap<String, Integer>();

        for(ConnVectorItem item : datas)
        {
            String wContent = item.getContent();

            int num = 0;
            if( item.getLabel() == Constants.Labl_is_ConnWord )
            {
                if( asConnWords.containsKey(wContent) ) num = asConnWords.get(wContent);
                asConnWords.put( wContent, num + 1 );
            }
            else
            {
                if( notAsConnWords.containsKey(wContent) ) num = notAsConnWords.get(wContent);
                notAsConnWords.put( wContent, num + 1 );
            }
            //1������ͣ�ô�
            //if( Resource.Stop_Words.contains(wContent) && !Resource.ExpConnectivesDict.contains(wContent) )

            //2�����˵�����Ϊnh, ni��nl�ȵȵĴ�
            //if( Constants.Ignore_PosTags.contains(wPosTag) ) continue;

            results.add(item);
        }

        ArrayList<Map.Entry<String,Integer>> sortWords = util.sortHashMap(asConnWords, false);

        //����ÿ��������Ϊ���ʳ��ֺͲ���Ϊ���ʳ��ֵĽ��
        ArrayList<String> lines = new ArrayList<String>();
        for(Map.Entry<String, Integer> entry : sortWords)
        {
            String connWord = entry.getKey();
            String line     = connWord + "\t" + entry.getValue();

            if( notAsConnWords.containsKey(connWord) ){
                line += "\t" + notAsConnWords.get(connWord);
            }else{
                line += "\t0";
            }

            lines.add(line);
        }

        String fPath = "./data/wordAndNotWord.txt";
        util.writeLinesToFile(fPath, lines);
    }

    /**
     * ��ԭʼѵ������ת��Ϊlibsvm��ʽ������д�뵽�ļ��С�
     * @throws IOException
     */
    public void convertToLibsvmData(ArrayList<ConnVectorItem> items) throws IOException
    {
        ArrayList<String> trainLines = new ArrayList<String>();
        ArrayList<String> testLines  = new ArrayList<String>();

        //�����ݲ������Ϊѵ�����ݺͲ�������
        int originNum   = items.size(), trainNum  = originNum / 5 * 4;
        boolean[] exist =  util.getRandomArrays(originNum,trainNum);

        int trainCorrectNum = 0, testCorrectNum = 0, allCorrectNum = 0;
        for(int index = 0; index < originNum; index++)
        {
            ConnVectorItem item  = items.get(index);
            String line = item.toLineForLibSvmWithAnsj();

            int type = item.getLabel();
            if( type == Constants.Labl_is_ConnWord ) allCorrectNum++;

            if( exist[index] ){
                trainLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) trainCorrectNum++;
            }
            else{
                testLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) testCorrectNum++;
            }
        }
        util.writeLinesToFile("data/word/wordTrainData.txt", trainLines);
        util.writeLinesToFile("data/word/wordTestData.txt", testLines);

        System.out.println("All Num: " + originNum + " Correct: " + allCorrectNum);
        System.out.println("Train Num: " + trainNum + " Correct: " + trainCorrectNum);
        System.out.println("Test Num: " + (originNum - trainNum) + " Correct: " + testCorrectNum);
    }


    /**����û����Ϊ���ʵĴʱ�**/
    public void saveNotLabeledWords() throws IOException
    {
        String fPath = "./notLabeledWords.txt";
        ArrayList<String> lines = new ArrayList<String>();

        for(ArrayList<String> curWords:this.notLabeledWords)
        {
            String line = "";
            for(String temp:curWords)
            {
                line = line + temp + "\t";
                break;
            }
            lines.add(line.trim());
        }
        util.writeLinesToFile(fPath, lines);
    }


    public static void main(String[] args) throws IOException, DocumentException
    {
        connFeatureExtract recognize = new connFeatureExtract();

        //1: ��ȡ�����ļ�����ѵ��--->.\Data\libsvmTrainData.txt
        recognize.extractFeatures();

        //recognize.saveNotLabeledWords();
    }

}
