package entity;

/**
 * 每个关系标号只能有一个对应的Relation
 * User: Ji JianHui
 * Time: 2014-02-20 11:15
 * Email: jhji@ir.hit.edu.cn
 */
public class DSARelation
{
    private String relNO;
    private Integer relType;

    private DSAConnective dsaConnective;

    private DSAArgument arg1;
    private DSAArgument arg2;


    public DSARelation(Integer relType, String relNO)
    {
        this.relNO   = relNO;
        this.relType = relType;

        this.dsaConnective = null;
        this.arg1 = null;
        this.arg2 = null;
    }

    public DSARelation()
    {
        this.dsaConnective = null;

        this.arg1 = null;
        this.arg2 = null;
    }

    public String getRelNO() {
        return relNO;
    }

    public void setRelNO(String relNO) {
        this.relNO = relNO;
    }

    public Integer getRelType() {
        return relType;
    }

    public void setRelType(Integer relType) {
        this.relType = relType;
    }

    public DSAConnective getDsaConnective() {
        return dsaConnective;
    }

    public void setDsaConnective(DSAConnective dsaConnective) {
        this.dsaConnective = dsaConnective;
    }

    public DSAArgument getArg1() {
        return arg1;
    }

    public void setArg1(DSAArgument arg1) {
        this.arg1 = arg1;
    }

    public DSAArgument getArg2() {
        return arg2;
    }

    public void setArg2(DSAArgument arg2) {
        this.arg2 = arg2;
    }
}
