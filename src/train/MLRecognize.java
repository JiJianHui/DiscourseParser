package train;

import common.Constants;
import common.util;
import entity.DSAConnective;
import entity.DSAWordDictItem;
import entity.SenseRecord;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.dom4j.*;
import resource.Resource;

import java.io.IOException;
import java.util.*;

/**
 * 使用机器学习方法来训练一个连词识别模型,,目前针对的是p3句间关系
 * User: Ji JianHui
 * Time: 2014-03-05 19:45
 * Email: jhji@ir.hit.edu.cn
 */
public class MLRecognize
{
    private ArrayList<MLVectorItem> items;
    private ArrayList<MLVectorItem> filterItems;

    private int expInstances;
    private int impInstances;

    private ArrayList<ArrayList<String>> notLabeledWords = new ArrayList<ArrayList<String>>();

    public MLRecognize()
    {

    }

    /**
     * 从新版本的标注语料中抽取连词特征来构建libsvm训练数据。TrainData
     * 为了构建模型只需要运行一次即可。
     */
    public void extractFeatures() throws IOException, DocumentException
    {
        this.items = new ArrayList<MLVectorItem>();
        this.filterItems = new ArrayList<MLVectorItem>();


        String path = Constants.Libsvm_Train_Data_Path;

        //1：获取特征向量组
        //this.getLabeledTrainData( this.items );
        this.getLabeledTrainDataWithAnsj(this.items);

        //2：对抽取到的特征进行处理，过滤
        this.filterItem( this.items, this.filterItems );

        //3：将特征转换为libsvm要求的格式
        this.convertToLibsvmData(path, this.filterItems);

        int total = this.expInstances + this.impInstances;

        System.out.println( "Exp Instances: " + this.expInstances );
        System.out.println( "Ixp Instances: " + this.impInstances );
        System.out.println( "Total Instances: " + total );
    }

    /**
     * 根据标注语料生成连词识别训练数据.此方法生成的是原始的训练数据，并没有按照机器学习的要求生成统一格式的数据。
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainDataWithAnsj(ArrayList<MLVectorItem> datas) throws DocumentException, IOException
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        //加载资源
        Resource.LoadRawRecord();
        Resource.LoadStopWords();
        Resource.LoadExpConnectivesDict();
        Resource.LoadWordRelDict();
        Resource.LoadLtpXMLResultSentID();

        //ansj分词结果
        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            //该记录的类型和其中的连词
            String  connWord    = record.getConnective().trim();
            String  sentContent = record.getText().trim();
            boolean isExplicit  = record.getType().equalsIgnoreCase(Constants.EXPLICIT);

            List<Term> words   = ToAnalysis.parse( sentContent );

            ArrayList<String> temp = new ArrayList<String>();
            temp.add(sentContent);

            int beginIndex = 0;
            for( Term wordItem : words )
            {
                //1：针对每个词，进行判断和处理
                String wContent   = wordItem.getName().trim();
                MLVectorItem item = new MLVectorItem(wContent);

                //2: 过滤掉噪音词
                if( !Resource.ExpConnectivesDict.contains(wContent) ) continue;

                //3：设置词性特征
                String wNextPos = "w";
                String wPrevPos = "w";
                Term wNextTerm  = wordItem.getTo();
                Term wPrevTerm  = wordItem.getFrom();

                if( wPrevTerm != null ) wPrevPos = wPrevTerm.getNatrue().natureStr;
                if( wNextTerm != null ) wNextPos = wNextTerm.getNatrue().natureStr;

                item.setPos( wordItem.getNatrue().natureStr );
                item.setPrevPos(wPrevPos);
                item.setNextPos(wNextPos);

                //4：判断该词在句子中的的位置
                beginIndex = sentContent.indexOf(wContent, beginIndex);
                item.setPositionInLine( beginIndex );

                //5：判断该词是正样本还是负样本. 目前多个词组成的连词当做负样本，以后使用模板识别
                if( connWord.equalsIgnoreCase(wContent) )
                {
                    item.setLabel( Constants.Labl_is_ConnWord );
                }
                else
                {
                    ArrayList<String> tempwords = new ArrayList<String>();
                    tempwords.add("更");

                    if( tempwords.contains(wContent.trim()) ) temp.add(wContent);
                }

                //6：判断该词在连词词典中出现的次数,以及歧义性
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
     * 根据标注语料生成连词识别训练数据.此方法生成的是原始的训练数据，并没有按照机器学习的要求生成统一格式的数据。
     * @throws DocumentException
     * @throws IOException
     */
    public void getLabeledTrainData(ArrayList<MLVectorItem> datas) throws DocumentException, IOException
    {
        System.out.println("[--Info--]: Get Labeled Train Instances From Sense Record..." );

        //加载资源
        Resource.LoadRawRecord();
        Resource.LoadStopWords();
        Resource.LoadExpConnectivesDict();
        Resource.LoadWordRelDict();
        Resource.LoadLtpXMLResultSentID();

        //LTP分词结果处理
        //针对每条记录，加载ltp分析结果,根据ltp来抽取词性以及
        for(SenseRecord record:Resource.Raw_Train_Annotation_p3)
        {
            //查找对应的ltp分析结果保存的文件位置
            String content   = record.getText().trim();
            Integer resultId = Resource.Ltp_Xml_Result_SentID_P3.get( content );

            if( resultId == null ) continue;

            String xmlPath = Constants.Ltp_XML_Result_P3 + "\\sent" + resultId + ".xml";

            //该记录的类型和其中的连词
            boolean  isExplicit = record.getType().equalsIgnoreCase(Constants.EXPLICIT);
            String   connWord   = record.getConnective();
            //String[] connWords  = record.getConnective().split( Constants.Parallel_Word_Seperator );

            //-------------------------------------------------------
            //分析ltp结果, 将每个词都打包为一个item，样本。
            Document domObj  = util.parseXMLFileToDOM(xmlPath, "gbk");
            Element rootNode = domObj.getRootElement();
            Element noteNode = rootNode.element("note");

            if( noteNode.attribute("pos").getText().equalsIgnoreCase("n") )    continue ;
            if( noteNode.attribute("parser").getText().equalsIgnoreCase("n") ) continue;

            Element paraNode  = rootNode.element("doc").element("para");

            //-----------------------------------------------
            //针对每个词，抽取信息来封装为一个样本
            for(Iterator iterator = paraNode.elementIterator(); iterator.hasNext();)
            {
                Element sentNode    = (Element) iterator.next();
                String  sentContent = sentNode.attribute("cont").getText();

                int        beginIndex = 0;     //为了查找一个词在该句子中的位置，使用beginIndex来防止同名的词
                String       prevPos  = "wp";
                MLVectorItem prevItem = null; //用于设置上一个词条的nextPos。

                for( Iterator ite = sentNode.elementIterator(); ite.hasNext(); )
                {
                    Element wordNode = (Element)ite.next();
                    String nodeName  = wordNode.getName();

                    //出现的每个词都是样本
                    if( nodeName.equals("word") )
                    {
                        String wContent = wordNode.attribute("cont").getText();
                        String wPosTag  = wordNode.attribute("pos").getText();
                        String wRelate  = wordNode.attribute("relate").getText();

                        MLVectorItem item = new MLVectorItem(wContent);

                        //3：设置词性特征
                        item.setPos(wPosTag);
                        item.setRelateTag(wRelate);
                        item.setPrevPos(prevPos);
                        prevPos = wPosTag;

                        //设置上一个item的nextPos以及自身的nextPos
                        if( prevItem != null ) prevItem.setNextPos(wPosTag);
                        prevItem = item;

                        //4：判断该词是正样本还是负样本. 目前多个词组成的连词当做负样本，以后使用模板识别
                        if( isExplicit && connWord.equalsIgnoreCase(wContent) )
                        {
                            item.setLabel( Constants.Labl_is_ConnWord );
                        }

                        //5：判断该词在句子中的的位置
                        beginIndex = sentContent.indexOf(wContent, beginIndex);
                        item.setPositionInLine( beginIndex );

                        //6：判断该词在连词词典中出现的次数,以及歧义性
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
                //设置最后一个item的nextPosTage
                prevItem.setNextPos("wp");
            }
        }
    }

    public void filterItem(ArrayList<MLVectorItem> datas, ArrayList<MLVectorItem> results) throws IOException
    {
        HashMap<String, Integer> asConnWords    = new HashMap<String, Integer>();
        HashMap<String, Integer> notAsConnWords = new HashMap<String, Integer>();

        for(MLVectorItem item : datas)
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
            //1：过滤停用词
            //if( Resource.Stop_Words.contains(wContent) && !Resource.ExpConnectivesDict.contains(wContent) )

            //2：过滤掉词性为nh, ni和nl等等的词
            //if( Constants.Ignore_PosTags.contains(wPosTag) ) continue;

            results.add(item);
        }

        ArrayList<Map.Entry<String,Integer>> sortWords = util.sortHashMap(asConnWords);

        //计算每个连词作为连词出现和不作为连词出现的结果
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
     * 将原始训练数据转换为libsvm格式并将它写入到文件中。
     * @param fPath
     * @throws IOException
     */
    public void convertToLibsvmData(String fPath, ArrayList<MLVectorItem> items) throws IOException
    {
        System.out.println("[--Info--]: Convert Data to Libsvm Format Data: " + fPath );

        ArrayList<String> lines = new ArrayList<String>();

        int expInstances = 0, impInstances = 0;

        for(MLVectorItem item : items)
        {
            //String line = item.toLineForLibSvm();
            String line = item.toLineForLibSvmWithAnsj();
            lines.add( line );

            int label = item.getLabel();
            if( label == Constants.Labl_is_ConnWord )
                this.expInstances++;
            else
                this.impInstances++;
        }

        util.writeLinesToFile(fPath, lines);
    }


    public void tainSVMModel()
    {
        //1: 将数据拆分为TrainData和TestData
        //2: 将数据进行缩放

    }

    /**保存没有作为连词的词表**/
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
        MLRecognize recognize = new MLRecognize();

        //1: 抽取特征文件进行训练--->.\Data\libsvmTrainData.txt
        recognize.extractFeatures();

        //recognize.saveNotLabeledWords();
    }

}
