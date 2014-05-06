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
 * ʶ�����ʵ����࣬���ڷ���һ���������Ƿ���ں�ѡ���ʡ�
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
     * ������������ʽ���򷽷��Ĺ�����ʶ���׼ȷ��
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

            //��ȡ�þ����е����ʺ�ѡ���Թ���һ���ķ���
            this.findConnectiveWithRule(sentence);

            //����tt��ft��ֵ
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

                    System.out.println("-------û��ʶ�������--------");
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
        String content = "Ϊ ��� ��� ���� ���� �� ������ �� ���� �� ���� �� ���� ���� ���� ���� �� ��λ  ���� ���� �� ���� ����� �Ѿ� ��ʼ ���� ���� �� ���� �� �� ��ʩ �� ʹ δ�� ���� �� ��� ���� �� �� �� ���� �� ���������� ���������� �˴� �� �������� �� �� �ֱ� ���� �� ���������� �˴� �� �������� �� �� �ƻ� �� ͨ�� ���� ���� ͣ��λ �� ���� ���� ͣ�� ���� �� ���� ���� ��ҵ�� �� �̻� �� Ǳ�� �� ��ǿ �� �齭 ������ �� �˿� ���� ��ϵ �� �� �� ��ʩ �� �ﵽ Ԥ�� Ŀ�� ";
        DSASentence sentence = new DSASentence( content.trim() );
        this.findConnectiveWithRule(sentence);
    }

    /**
     * ��������ʽ���򷽷�ʶ��һ�������еľ���ϵ�����ڹ�ϵ,���ظþ����е�����
     * @param dsaSentence
     */
    public void findConnectiveWithRule(DSASentence dsaSentence) throws Exception
    {
        //��ȡ��ѡ��������Ϣ
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
     *�Է�װΪ��ľ��ӽ��д����������Ҫ����ȡ����Ӧ��������ͬʱ�ж��Ƿ�������ʡ�
     * ��Ҫ��Ҫָ���жϵķ�ʽ����Ϊ������rule����ML��������Ҫ����������
     * ����type���ж������������ǻ���ѧϰ��0: ���ڹ���  1�����ڻ���ѧϰ
     * Ŀǰ��û�н��и����εķ�����
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

                    //ʹ�ù��򷽷������ж��Ƿ�������
                    if( isConnectiveWithRule(wContent, wPosTag, wRelate, senseContent) )
                    {
                        DSAConnective dsaConnective = new DSAConnective(wContent,dsaSentence.getId());
                        dsaConnective.setPosTag(wPosTag);
                        dsaConnective.setDepencyTag(wRelate);

                        result.add(dsaConnective);
                    }

                    //ʹ�û���ѧϰ�������ж�
                }
            }

        }

        return result;
    }

    /**
     * ��������ʽ���򷽷��ж�һ�����ǲ�����Ч������
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
