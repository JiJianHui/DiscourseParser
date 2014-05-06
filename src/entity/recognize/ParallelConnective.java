package entity.recognize;

import entity.DSAArgument;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-03-24 17:04
 * Email: jhji@ir.hit.edu.cn
 */
public class ParallelConnective
{
    private String content;

    private Integer beginIndex1;
    private Integer beginIndex2;

    private DSAArgument arg1;
    private DSAArgument arg2;

    public ParallelConnective(String content)
    {
        beginIndex2  = 0;
        beginIndex1  = 0;
        this.content = content;
    }

    public ParallelConnective(){};

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getBeginIndex1() {
        return beginIndex1;
    }

    public void setBeginIndex1(Integer beginIndex1) {
        this.beginIndex1 = beginIndex1;
    }

    public Integer getBeginIndex2() {
        return beginIndex2;
    }

    public void setBeginIndex2(Integer beginIndex2) {
        this.beginIndex2 = beginIndex2;
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
