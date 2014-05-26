package dataAnalysis;

import common.util;

/**
 * RawRecord主要是用来封装原始标注信息的，这样有助于方便程序的调用.[旧版本的Record]
 * User: Ji JianHui
 * Time: 2014-02-26 10:03
 * Email: jhji@ir.hit.edu.cn
 */
public class RawRecord
{
    private String fPath;
    private String annotation;

    private int arg1Beg;
    private int arg1End;

    private int arg2Beg;
    private int arg2End;

    private int lineBeg;
    private int lineEnd;

    private boolean isExplicit;

    private int impNum;
    private int expNum;

    private String relNO;
    private String connWord;

    public RawRecord(String annotation)
    {
        this.annotation = annotation;

        String[] items = annotation.split(" ");

        arg1Beg = Integer.valueOf( items[0] );
        arg1End = Integer.valueOf( items[1] );

        arg2Beg = Integer.valueOf( items[2] );
        arg2End = Integer.valueOf( items[3] );

        expNum  = Integer.valueOf( items[4] );
        impNum  = Integer.valueOf( items[5] );

        lineBeg = arg1Beg < arg2Beg? arg1Beg:arg2Beg;
        lineEnd = arg1End > arg2End? arg1End:arg2End;

        relNO   = util.getRelNO( items[items.length - 1] );

        relNO   = util.convertOldRelIDToNew(relNO);
    }


    /**
     * 判断和另一个标注是否是针对同一个句子进行标注，
     * 注意调用此方法之前，首先需要确定两个record是同一个原始语料的不同标注行。因为方法并没有判断是否是同一个原始文件。
     * 0: 不是同一个句子
     * 1: 句子边界完全相同
     * 2: 句子边界嵌套即当前record和rawRecord完全嵌套
     * 3: 两个标注句子部分嵌套
     * @param rawRecord
     * @return
     */
    public int isSameBoundaryWith(RawRecord rawRecord)
    {
        int type = 0;

        //完全不同的情况下，两个没有相同的
        if(this.lineEnd < rawRecord.lineBeg || this.lineBeg > rawRecord.lineEnd)
        {
            return 0;
        }

        //完全相同的句子边界
        if(this.lineBeg == rawRecord.lineBeg && this.lineEnd == rawRecord.lineEnd)
        {
            return 1;
        }

        //完全嵌套的情况
        if(this.lineBeg >= rawRecord.lineBeg && this.lineEnd <= rawRecord.lineEnd)
        {
            return 2;
        }
        if(rawRecord.lineBeg >= this.lineBeg && rawRecord.lineEnd <= this.lineEnd)
        {
            return 2;
        }

        //部分嵌套
        return 3;
    }

    public String getfPath() {
        return fPath;
    }

    public void setfPath(String fPath) {
        this.fPath = fPath;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public int getArg1Beg() {
        return arg1Beg;
    }

    public void setArg1Beg(int arg1Beg) {
        this.arg1Beg = arg1Beg;
    }

    public int getArg1End() {
        return arg1End;
    }

    public void setArg1End(int arg1End) {
        this.arg1End = arg1End;
    }

    public int getArg2Beg() {
        return arg2Beg;
    }

    public void setArg2Beg(int arg2Beg) {
        this.arg2Beg = arg2Beg;
    }

    public int getArg2End() {
        return arg2End;
    }

    public void setArg2End(int arg2End) {
        this.arg2End = arg2End;
    }

    public boolean isExplicit() {
        return isExplicit;
    }

    public void setExplicit(boolean explicit) {
        isExplicit = explicit;
    }

    public int getImpNum() {
        return impNum;
    }

    public void setImpNum(int impNum) {
        this.impNum = impNum;
    }

    public int getExpNum() {
        return expNum;
    }

    public void setExpNum(int expNum) {
        this.expNum = expNum;
    }

    public String getRelNO() {
        return relNO;
    }

    public void setRelNO(String relNO) {
        this.relNO = relNO;
    }
}
