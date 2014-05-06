package entity.recognize;

import entity.DSAConnective;

/**
 * 每个关系标号只能有一个对应的Relation. InterRelation封装的是句内关系。
 * User: Ji JianHui
 * Time: 2014-02-20 11:15
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAInterRelation
{
    private String relNO;
    private String relType;
    private Double probality;

    private DSAConnective dsaConnective;

    public String arg1Content;
    public String arg2Content;

    public Integer sentID;

    public DSAInterRelation(String relType, String relNO)
    {
        this.relNO   = relNO;
        this.relType = relType;
        this.probality = 0.0;

        this.dsaConnective = null;
        this.arg1Content   = null;
        this.arg2Content   = null;
    }

    public DSAInterRelation()
    {
        this.dsaConnective = null;

        this.arg1Content = null;
        this.arg2Content = null;
    }

    public String getRelNO() {
        return relNO;
    }

    public void setRelNO(String relNO) {
        this.relNO = relNO;
    }

    public String getRelType() {
        return relType;
    }

    public void setRelType(String relType) {
        this.relType = relType;
    }

    public DSAConnective getDsaConnective() {
        return dsaConnective;
    }

    public void setDsaConnective(DSAConnective dsaConnective) {
        this.dsaConnective = dsaConnective;
    }

    public String getArg1Content() {
        return arg1Content;
    }

    public void setArg1Content(String arg1Content) {
        this.arg1Content = arg1Content;
    }

    public String getArg2Content() {
        return arg2Content;
    }

    public void setArg2Content(String arg2Content) {
        this.arg2Content = arg2Content;
    }

    public Integer getSentID() {
        return sentID;
    }

    public void setSentID(Integer sentID) {
        this.sentID = sentID;
    }

    public Double getProbality() {
        return probality;
    }

    public void setProbality(Double probality) {
        this.probality = probality;
    }
}
