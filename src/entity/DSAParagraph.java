package entity;

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
    public ArrayList<DSACrossRelation> crossRelations;//����ϵ����

    public DSAParagraph(String content)
    {
        this.paraContent = content;
        this.sentences   = new ArrayList<DSASentence>();
        this.crossRelations = new ArrayList<DSACrossRelation>();
    }



}
