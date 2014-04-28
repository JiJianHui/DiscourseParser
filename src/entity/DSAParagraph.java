package entity;

import java.util.ArrayList;

/**
 * 为了对一段文本中的句间关系进行封装，使用DSAParagraph进行封装。
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-28 10:46
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAParagraph
{
    public String paraContent;

    public ArrayList<DSASentence> sentences;
    public ArrayList<DSACrossRelation> crossRelations;//句间关系集合

    public DSAParagraph(String content)
    {
        this.paraContent = content;
        this.sentences   = new ArrayList<DSASentence>();
        this.crossRelations = new ArrayList<DSACrossRelation>();
    }



}
