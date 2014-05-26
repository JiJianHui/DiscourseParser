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
        ArrayList<ConnVectorItem> items = new ArrayList<ConnVectorItem>();
        ArrayList<ConnVectorItem> filterItems = new ArrayList<ConnVectorItem>();

        //1����ȡ����������
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(items);

        //2���Գ�ȡ�����������д�������
        this.filterItem(items, filterItems);

        //3��������ת��ΪlibsvmҪ��ĸ�ʽ
        this.convertToLibsvmData(filterItems);

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

            boolean isSameWithPrev = prev != null && util.isTheSameRecord(record, prev);
            boolean isSameWithNext = next != null && util.isTheSameRecord(record, next);

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
                Integer asConnNum    = Resource.AsDiscourseWordDict.get( item.getContent() );
                Integer notAsConnNum = Resource.NotAsDiscourseWordDict.get( item.getContent() );

                double ambugity = ( notAsConnNum == null )? 0 : notAsConnNum*1.0 / (notAsConnNum+asConnNum);

                if( ambugity < 0.00006 && asConnNum > 100 ) item.setLabel( Constants.Labl_is_ConnWord );

                datas.add(item);
            }
        }
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
        ArrayList<String> viewLines  = new ArrayList<String>();

        //�����ݲ������Ϊѵ�����ݺͲ�������
        int originNum   = items.size(), trainNum  = originNum / 5 * 4;
        boolean[] exist =  util.getRandomArrays(originNum,trainNum);

        int trainCorrectNum = 0, testCorrectNum = 0, allCorrectNum = 0;
        for(int index = 0; index < originNum; index++)
        {
            ConnVectorItem item  = items.get(index);

            int    type = item.getLabel();
            String line = item.toLineForLibSvmWithAnsj();

            if( type == Constants.Labl_is_ConnWord ) allCorrectNum++;

            if( exist[index] )
            {
                trainLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) trainCorrectNum++;
            }
            else
            {
                testLines.add(line);
                if( type == Constants.Labl_is_ConnWord ) testCorrectNum++;
            }

            //debug ���item��ʵ������
            String viewLine = item.toLineForView();
            viewLines.add(viewLine);
        }

        util.writeLinesToFile("data/word/wordItems.txt", viewLines);
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

    //------------------------------Process Inter or Cross Word----------------

    /**����ʹ��������p2��p3�еķֲ����ж�һ���������ھ��ڻ��Ǿ������**/
    public void predictJustUseDict() throws IOException,DocumentException
    {
        Resource.LoadRawRecord();
        Resource.LoadWordRelDict();
        Resource.LoadConnInP2AndP3();

        ArrayList<SenseRecord> records = new ArrayList<SenseRecord>();
        records.addAll( Resource.Raw_Train_Annotation_p3 );
        records.addAll( Resource.Raw_Train_Annotation_p2 );

        int interNum = 0, interRec = 0, interCorrect = 0;  //��һ��ʶ����
        int crossNum = 0, crossRec = 0, crossCorrect = 0;  //�ڶ���ʶ����

        for( SenseRecord record : records )
        {
            String conn   = record.getConnective();
            int posInLine = record.getConnBeginIndex();
            String sent   = (record.getConnArgIndex() == 1) ? record.getArg1():record.getArg2();

            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;
            if( !Resource.allWordsDict.containsKey(conn) ) continue;
            if( conn.contains(";") ) continue;

            //�Զ�ʶ����
            int result = markConnAsInterOrCross(conn, posInLine, sent);
            if( result == Constants.Label_Cross_ConnWord ) crossRec++;
            if( result == Constants.Label_Inter_ConnWord ) interRec++;

            //�޸�ʶ��׼ȷ����
            if( record.getfPath().endsWith(Constants.P2_Ending) )
            {
                crossNum++;
                if( result == Constants.Label_Cross_ConnWord ) crossCorrect++;
            }
            else
            {
                interNum++;
                if( result == Constants.Label_Inter_ConnWord ) interCorrect++;
            }
        }
        //����ÿһ���prf
        double pCross = crossCorrect * 1.0 / crossRec;
        double rCross = crossCorrect * 1.0 / crossNum;
        double fCross = 2 * pCross * rCross / (pCross + rCross);

        double pInter = interCorrect * 1.0 / interRec;
        double rInter = interCorrect * 1.0 / interNum;
        double fInter = 2 * pInter * rInter / (pInter + rInter);

        System.out.println("Cross: allNum: " + crossNum + " recNum: " + crossRec + " recCorrect: " + crossCorrect);
        System.out.println("Inter: allNum: " + interNum + " recNum: " + interRec + " recCorrect: " + interCorrect);
        System.out.format("Corss: P: %.2f R: %.2f F: %.2f\n", pCross, rCross, fCross);
        System.out.format("Inter: P: %.2f R: %.2f F: %.2f\n", pInter, rInter, fInter);
    }

    /**�ж�һ�������Ǿ�����ʻ��Ǿ�������.������һ���������ݣ����صĽ��Ϊ��
     * 0����������  1����������  2���������**/
    public int markConnAsInterOrCross(String conn, int positionInLine, String sentence)
    {
        if( !Resource.allWordsDict.containsKey(conn) ) return Constants.Labl_Not_ConnWord;
        if( conn.contains(";") ) return Constants.Label_Inter_ConnWord;

        //1: ��ȡ��������p2��p3�зֱ���ֵĴ���
        int numInP2 = 0, numInP3 = 0;
        if( Resource.ConnInP2AndP3.containsKey(conn) )
        {
            numInP2 = Resource.ConnInP2AndP3.get(conn)[0];
            numInP3 = Resource.ConnInP2AndP3.get(conn)[1];
        }

        //4���Ƿ�λ�ڵ�һ���̾���
        boolean inSentenceHead = true;
        String temp = sentence.substring(0, positionInLine);

        if( temp.contains("��") || temp.contains(",") ) inSentenceHead = false;

        //���������ֵ���������ж�һ�����ʵľ��;�������
        if( numInP2 > numInP3 && inSentenceHead ) return Constants.Label_Cross_ConnWord;

        return Constants.Label_Inter_ConnWord;
    }

    public static void main(String[] args) throws IOException, DocumentException
    {
        connFeatureExtract recognize = new connFeatureExtract();

        //1: ��ȡ�����ļ�����ѵ��--->.\Data\libsvmTrainData.txt
        recognize.extractFeatures();

        //recognize.saveNotLabeledWords();

        //2: ����������ʺ;������ʶ���Ч��
        //recognize.predictJustUseDict();
    }

}
