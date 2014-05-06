package entity.recognize;

import entity.DSAConnective;

/**
 * 篇章关系中分为句间关系和句内关系，CrossRelation封装了句间关系
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-04-28 10:52
 * Email: jhji@ir.hit.edu.cn
 */
public class DSACrossRelation
{
    public String relNO;
    public String relType;
    public Double probality;

    public Integer arg1SentID;
    public Integer arg2SentID;

    public String arg1Content;
    public String arg2Content;

    public String connective;
    public DSAConnective conn;

}
