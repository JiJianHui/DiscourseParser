package train.word;

import common.Constants;
import common.util;
import entity.train.DSAWordDictItem;
import entity.train.SenseRecord;
import org.ansj.domain.Term;
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

    private int expInstances;
    private int impInstances;

    private ArrayList<ArrayList<String>> notLabeledWords = new ArrayList<ArrayList<String>>();

    public connFeatureExtract() throws DocumentException, IOException
    {
        //������Դ
        Resource.LoadRawRecord();
        Resource.LoadStopWords();
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
        this.items = new ArrayList<ConnVectorItem>();
        this.filterItems = new ArrayList<ConnVectorItem>();


        String path = Constants.Libsvm_Origin_Data_Path;

        //1����ȡ����������
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(this.items);

        //2���Գ�ȡ�����������д�������
        this.filterItem( this.items, this.filterItems );

        //3��������ת��ΪlibsvmҪ��ĸ�ʽ
        this.convertToLibsvmData(path, this.filterItems);

        int total = this.expInstances + this.impInstances;

        System.out.println( "Exp Instances: " + this.expInstances );
        System.out.println( "Ixp Instances: " + this.impInstances );
        System.out.println( "Total Instances: " + total );
    }

    /**
     * ���ݱ�ע������������ʶ��ѵ������.�˷������ɵ���ԭʼ��ѵ�����ݣ���û�а��ջ���ѧϰ��Ҫ������ͳһ��ʽ�����ݡ�
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainDataWithAnsj(ArrayList<ConnVectorItem> datas)
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        //����һ�������г��ֶ�����ʵ����
        int findIndex = 0; //Ϊ�˱�֤�ٶȣ���Ҫ��ǰһ��ƥ���λ�ÿ�ʼ����
        ArrayList<SenseRecord> handledRecord = new ArrayList<SenseRecord>();

        //ansj�ִʽ��
        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            //�ü�¼�����ͺ����е�����
            String  connWord    = record.getConnective().trim();
            String  sentContent = record.getText().trim();
            boolean isExplicit  = record.getType().equalsIgnoreCase(Constants.EXPLICIT);

            List<Term> words   = ToAnalysis.parse( sentContent );

            ArrayList<String> temp = new ArrayList<String>();
            temp.add(sentContent);

            int beginIndex = 0;
            for( Term wordItem : words )
            {
                //1�����ÿ���ʣ������жϺʹ���
                String wContent   = wordItem.getName().trim();
                ConnVectorItem item = new ConnVectorItem(wContent);

                //2: ���˵�������
                if( !Resource.ExpConnWordDict.containsKey(wContent) ) continue;

                //�������ʳ��ֵĴ������й���
                int connNum    = Resource.ExpConnWordDict.get(wContent);
                int notConnNum = Resource.NotAsDiscourseWordDict.get(wContent);

                if( connNum < 5 && notConnNum > 5*connNum ) continue;
                if( connNum < 5 ) continue;

                //3�����ô�������
                String wNextPos = "w", wPrevPos = "w";
                Term wNextTerm  = wordItem.getTo(), wPrevTerm  = wordItem.getFrom();

                if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
                if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

                item.setPos( wordItem.getNatrue().natureStr );
                item.setPrevPos(wPrevPos);
                item.setNextPos(wNextPos);

                //4���жϸô��ھ����еĵ�λ��
                beginIndex = sentContent.indexOf(wContent, beginIndex);
                item.setPositionInLine( beginIndex );

                //5���жϸô������������Ǹ�����. Ŀǰ�������ɵ����ʵ������������Ժ�ʹ��ģ��ʶ��
                if( connWord.equalsIgnoreCase(wContent) )
                {
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                else
                {
                    ArrayList<String> tempwords = new ArrayList<String>();
                    tempwords.add("��");

                    if( tempwords.contains(wContent.trim()) ) temp.add(wContent);
                }

                //6���жϸô������ʴʵ��г��ֵĴ���,�Լ�������
                double occurTime = 0.0, ambiguity = 1.0;

                if( Resource.allWordsDict.containsKey(wContent) )
                {
                    DSAWordDictItem wordDictItem = Resource.allWordsDict.get(wContent);

                    occurTime = wordDictItem.getExpNum();
                    ambiguity = wordDictItem.getMostExpProbality();
                }

                item.setAmbiguity(ambiguity);
                item.setOccurInDict(occurTime);

                datas.add(item);
            }

            if(temp.size() > 1) this.notLabeledWords.add(temp);
        }
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
                asConnWords.put(wContent, num + 1);
            }
            else
            {
                if( notAsConnWords.containsKey(wContent) ) num = notAsConnWords.get(wContent);
                notAsConnWords.put(wContent, num + 1);
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

            if( notAsConnWords.containsKey(connWord) )
            {
                line += "\t" + notAsConnWords.get(connWord);
            }
            else
            {
                line += "\t0";
            }

            lines.add(line);
        }
        String fPath = "./data/wordAndNotWord.txt";
        util.writeLinesToFile(fPath, lines);

    }

    /**
     * ��ԭʼѵ������ת��Ϊlibsvm��ʽ������д�뵽�ļ��С�
     * @param fPath
     * @throws IOException
     */
    public void convertToLibsvmData(String fPath, ArrayList<ConnVectorItem> items) throws IOException
    {
        System.out.println("[--Info--]: Convert Data to Libsvm Format Data: " + fPath );

        ArrayList<String> lines = new ArrayList<String>();

        int expInstances = 0, impInstances = 0;

        for(ConnVectorItem item : items)
        {
            //String line = item.toLineForLibSvm();
            String line = item.toLineForLibSvmWithAnsj();
            lines.add( line );
            int label = item.getLabel();

            if( label == Constants.Labl_is_ConnWord ){
                this.expInstances++;
            }
            else{
                this.impInstances++;
            }
        }

        util.writeLinesToFile(fPath, lines);

        //�����ݲ��Ϊѵ�����ݺͲ�������
        int originNum = lines.size();
        int trainNum  = originNum /5 * 4;

        ArrayList<String> trainLines = new ArrayList<String>( lines.subList(0, trainNum) );
        ArrayList<String> testLines  = new ArrayList<String>( lines.subList(trainNum, originNum) );

        util.writeLinesToFile(Constants.Libsvm_Train_Data_Path, trainLines);
        util.writeLinesToFile(Constants.Libsvm_Test_Data_Path, testLines);
    }


    public void tainSVMModel()
    {
        //1: �����ݲ��ΪTrainData��TestData
        //2: �����ݽ�������

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
