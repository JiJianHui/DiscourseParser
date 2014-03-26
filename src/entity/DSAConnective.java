package entity;

import common.Constants;

/**
 * Ϊ�˱�����ʵ��ʶ��ʱ������ÿһ����ѡ������
 * User: Ji JianHui
 * Time: 2014-02-21 10:49
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAConnective
{
    private String content;     //���ʵ�ʵ������

    private String posTag;
    private String prevPosTag;
    private String nextPosTag;

    private double positionInLine;

    private String depencyTag;  //��������ı�ǩ

    private Boolean isParallelWord; //�Ƿ��ǲ��й����ʣ���Ȼ...����

    private String upWord;          //��λ��
    private String sameWord;        //ͬ���

    private String parserTag;        //����ṹ������ǩ
    private String prevParserTag;
    private String nextParserTag;


    private DSAArgument arg1;
    private DSAArgument arg2;


    public DSAConnective(String content)
    {
        this.content    = content;
        this.posTag     = null;

        this.depencyTag = null;

        if( this.content.contains(Constants.Parallel_Word_Seperator) )
        {
            this.isParallelWord = true;
        }
        else this.isParallelWord = false;
    }


    public String getStringContent()
    {
        String result = "content: " + this.content;
        result +=  "\t" + "pos: " + this.posTag;
        result += "\t" + "dependcyTag: " + this.depencyTag;

        return result;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public String getDepencyTag() {
        return depencyTag;
    }

    public void setDepencyTag(String depencyTag) {
        this.depencyTag = depencyTag;
    }

    public Boolean getParallelWord() {
        return isParallelWord;
    }

    public void setParallelWord(Boolean parallelWord) {
        isParallelWord = parallelWord;
    }

    public String getPrevPosTag() {
        return prevPosTag;
    }

    public void setPrevPosTag(String prevPosTag) {
        this.prevPosTag = prevPosTag;
    }

    public String getNextPosTag() {
        return nextPosTag;
    }

    public void setNextPosTag(String nextPosTag) {
        this.nextPosTag = nextPosTag;
    }

    public double getPositionInLine() {
        return positionInLine;
    }

    public void setPositionInLine(double positionInLine) {
        this.positionInLine = positionInLine;
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
