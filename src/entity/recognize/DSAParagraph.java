package entity.recognize;

import common.Constants;
import resource.Resource;

import java.util.ArrayList;

/**
 * Ϊ�˶�һ���ı��еľ���ϵ���з�װ��ʹ��DSAParagraph���з�װ��
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-28 10:46
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAParagraph
{
    public String paraContent;

    public ArrayList<DSASentence> sentences;

    public ArrayList<DSAInterRelation>      interRelations;//���ڹ�ϵ����
    public ArrayList<DSACrossRelation> crossRelations;//����ϵ����

    public DSAParagraph(String content)
    {
        this.paraContent = content;
        this.sentences   = new ArrayList<DSASentence>();

        this.interRelations = new ArrayList<DSAInterRelation>();
        this.crossRelations = new ArrayList<DSACrossRelation>();
    }

    /**�������ʾΪXML�ַ�������ʽ**/
    public String toXML()
    {
        String result = "<?xml version=\"1.0\" encoding=\"gb2312\" ?><doc>";
        result = result + "<para>";

        //1�����ڹ�ϵ
        result = result + "<InterSentenceSense>";

        for(int index = 0; index < this.sentences.size(); index++)
        {
            DSASentence curSentence = this.sentences.get(index);

            if( !curSentence.isCorrect() ) continue;

            result = result + "<Sentence id=\"" + index + "\" content=\""+curSentence.getContent()+"\">";

            for(DSAInterRelation relation : curSentence.getRelations() )
            {
                String type = relation.getRelType(), NO = relation.getRelNO();
                result = result + "<InterSense id=\"" + curSentence.getId() + "\"";
                result = result + " type=\"" + type + "\" NO=\"" + NO + "\"";
                result = result + " content=\""+ Resource.senseLists.get(NO) + "\">";

                result = result + "<arg1>" + relation.getArg1Content() + "</arg1>";
                result = result + "<arg2>" + relation.getArg2Content() + "</arg2>";

                DSAConnective conn = relation.getDsaConnective();

                if( type.equalsIgnoreCase(Constants.IMPLICIT) )
                {
                    result = result + "<connective beginIndex=\"-1\">";
                }
                else
                {
                    result = result + "<connective beginIndex=\"" + conn.getPositionInLine() + "\">";
                    result = result + conn.getContent();
                }
                result = result + "</connective>";
                result = result + "</InterSense>";
            }
            result = result + "</Sentence>";
        }

        result = result + "</InterSentenceSense>";


        //2������ϵ
        result = result + "<CrossSentenceSense>";

        for( DSACrossRelation crossRelation:this.crossRelations )
        {
            result = result + "<CrossSense type=\"" + crossRelation.relType
                    + "\" NO=\"" + crossRelation.relNO + "\"";
            result = result + " content=\""+ Resource.senseLists.get(crossRelation.relNO) + "\">";

            result = result + "<arg1 sentID=\"" + crossRelation.arg1SentID + "\">"
                    + crossRelation.arg1Content + "</arg1>";

            result = result + "<arg2 sentID=\"" + crossRelation.arg2SentID + "\">"
                    + crossRelation.arg2Content + "</arg2>";

            if(crossRelation.relType.equalsIgnoreCase(Constants.IMPLICIT))
            {
                result = result + "<connective sentID=\"-1\" beginIndex=\"-1\">";
            }
            else{
                result = result + "<connective sentID=\"" + crossRelation.conn.getSentID() + "\""
                         + " beginIndex=\"" + crossRelation.conn.getPositionInLine() + "\">"
                         + crossRelation.conn.getContent();
            }
            result = result + "</connective>";
            result = result + "</CrossSense>";
        }

        result = result + "</CrossSentenceSense>";

        result = result + "</para>";
        result = result + "</doc>";

        return result;
    }
}
