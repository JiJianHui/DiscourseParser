package train.word;

import common.Constants;
import common.util;
import entity.recognize.DSAConnective;
import entity.recognize.DSASentence;
import entity.Recognize;
import entity.train.SenseRecord;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 识别连词的主类，用于分析一个句子中是否存在候选连词。
 * User: Ji JianHui
 * Time: 2014-02-23 22:55
 * Email: jhji@ir.hit.edu.cn
 */
public class ConnectiveRecognize
{
    private ArrayList<DSASentence> sentences;

    public ConnectiveRecognize() throws DocumentException, IOException
    {
        this.sentences = new ArrayList<DSASentence>();

        Resource.LoadRawRecord();
        Resource.LoadExpConnectivesDict();
        Resource.LoadLtpXMLResultSentID();
    }
    /**
     * 分析基于启发式规则方法的关联词识别的准确率
     * @throws Exception
     */
    public void connectiveRecognize() throws Exception
    {
        System.out.println("[--Info--] Begining to Recognize Connective Word in Sentence");

        Recognize wordRecognize = new Recognize();

        for(SenseRecord record : Resource.Raw_Train_Annotation_p3)
        {
            if( record.getType().equalsIgnoreCase(Constants.IMPLICIT) ) continue;

            DSASentence sentence = new DSASentence( record.getText() );
            sentences.add(sentence);

            //获取该句子中的连词候选，以供进一步的分析
            this.findConnectiveWithRule(sentence);

            //计算tt和ft的值
            if( sentence.getConWords().size() > 0 )
            {
                if( sentence.containWord( record.getConnective() ) )
                {
                    wordRecognize.tt++;
                }
                else
                {
                    wordRecognize.ft++;
                }
            }
            else
            {
                if( record.getConnective().length() > 0 && !record.getConnective().equalsIgnoreCase("NULL") )
                {
                    wordRecognize.ff++;

                    System.out.println("-------没有识别出连词--------");
                    System.out.println( sentence.getContent() );
                    System.out.println( sentence.getConnWordContent() );
                    System.out.println( record.getAnnotation() );
                    System.out.println( record.getConnective() );
                }
                else
                {
                    wordRecognize.tf++;
                }
            }

        }

        wordRecognize.pRate = wordRecognize.tt * 1.0 / (wordRecognize.tt + wordRecognize.ft);
        wordRecognize.rRate = wordRecognize.tt * 1.0 / (wordRecognize.tt + wordRecognize.ff);
        wordRecognize.fRate = (2 * wordRecognize.pRate * wordRecognize.rRate) / (wordRecognize.pRate + wordRecognize.rRate);

        System.out.println("tt: " + wordRecognize.tt);
        System.out.println("ft: " + wordRecognize.ft);

        System.out.println("tf: " + wordRecognize.tf);
        System.out.println("ff: " + wordRecognize.ff);

        System.out.println("P: " + wordRecognize.pRate );
        System.out.println("R: " + wordRecognize.rRate );
        System.out.println("F: " + wordRecognize.fRate );
    }

    public void test() throws Exception
    {
        String content = "为 提高 香港 国际 机场 的 竞争力 ， 保持 其 国际 及 区域 航空 运输 中心 的 地位  特区 政府 与 机场 管理局 已经 开始 着手 部署 、 推行 多 项 措施 ， 使 未来 １０ 年 香港 机场 客 、 货 运量 由 １９９９年 ２９００万 人次 和 １９７万 吨 ， 分别 增长 到 ８７００万 人次 和 ９００万 吨 ， 计划 将 通过 增加 机场 停机位 、 降低 航机 停泊 费用 、 发掘 机场 商业区 、 商机 和 潜力 ， 加强 与 珠江 三角洲 的 乘客 运输 联系 等 多 项 措施 ， 达到 预期 目标 ";
        DSASentence sentence = new DSASentence( content.trim() );
        this.findConnectiveWithRule(sentence);
    }

    /**
     * 按照启发式规则方法识别一个句子中的句间关系：句内关系,返回该句子中的连词
     * @param dsaSentence
     */
    public void findConnectiveWithRule(DSASentence dsaSentence) throws Exception
    {
        //获取候选关联词信息
        ArrayList<DSAConnective> curConnectives = this.getConnective(dsaSentence, Constants.ConnRecoganize_Rule);

        for( DSAConnective conWord: curConnectives )
        {
            if( Resource.ExpConnWordDict.containsKey(conWord.getContent()) )
            {
                dsaSentence.getConWords().add( conWord );
            }
        }

    }

    /**
     *对封装为类的句子进行处理分析，主要是提取出对应的特征。同时判断是否存在连词。
     * 主要需要指定判断的方式，因为不管是rule还是ML都首先需要分析特征。
     * 传入type来判断是依靠规则还是机器学习。0: 基于规则  1：基于机器学习
     * 目前还没有进行更深层次的分析。
     * @param dsaSentence
     */
    private ArrayList<DSAConnective> getConnective(DSASentence dsaSentence, int type) throws Exception
    {
        ArrayList<DSAConnective> result = new ArrayList<DSAConnective>();

        String senseContent   = dsaSentence.getContent().trim();
        Integer resultId      = Resource.Ltp_Xml_Result_SentID_P3.get( senseContent );

        if( resultId == null ) return result;

        String xmlPath   = Constants.Ltp_XML_Result_P3 + "\\sent" + resultId + ".xml";
        Document domObj  = util.parseXMLFileToDOM(xmlPath, "gbk");

        Element noteNode = domObj.getRootElement().element("note");

        if( noteNode.attribute("pos").getText().equalsIgnoreCase("n") )    return result;
        if( noteNode.attribute("parser").getText().equalsIgnoreCase("n") ) return result;

        Element paraNode  = domObj.getRootElement().element("doc").element("para");

        for(Iterator iterator = paraNode.elementIterator(); iterator.hasNext();)
        {
            Element sentNode  = (Element) iterator.next();

            for( Iterator ite = sentNode.elementIterator(); ite.hasNext(); )
            {
                Element wordNode = (Element)ite.next();

                if( wordNode.getName().equals("word") )
                {
                    String wContent = wordNode.attribute("cont").getText();
                    String wPosTag  = wordNode.attribute("pos").getText();
                    String wRelate  = wordNode.attribute("relate").getText();

                    //使用规则方法进行判断是否是连词
                    if( isConnectiveWithRule(wContent, wPosTag, wRelate, senseContent) )
                    {
                        DSAConnective dsaConnective = new DSAConnective(wContent,dsaSentence.getId());
                        dsaConnective.setPosTag(wPosTag);
                        dsaConnective.setDepencyTag(wRelate);

                        result.add(dsaConnective);
                    }

                    //使用机器学习方法来判断
                }
            }

        }

        return result;
    }

    /**
     * 基于启发式规则方法判断一个词是不是有效的连词
     */
    public boolean isConnectiveWithRule(String word, String posTag, String relateTag, String sentence)
    {
        boolean result = false;

        if( Resource.ExpConnWordDict.containsKey(word))
        {
            if( posTag.equals("c") )
            {
                if( relateTag.equals("LAD") || relateTag.equals("RAD") )
                    result = false;
                else
                    result = true;
            }
            /**
            else if(  posTag.equals("p") || posTag.equals("nt") )
            {
                if( relateTag.equals("adv") )
                {
                    result = true;
                }
            }
             **/
            else
            {
                result = true;
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception
    {
        ConnectiveRecognize connRecognize = new ConnectiveRecognize();
        connRecognize.connectiveRecognize();

        //connRecognize.test();
    }
}
