package entity;

import common.Constants;
import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;

/**
 * 为了保存在实际识别时遇到的每一个候选的连词
 * User: Ji JianHui
 * Time: 2014-02-21 10:49
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAConnective
{
    private int sentID;//属于的句子
    private String content;     //连词的实际内容

    //连词识别相关特征
    private String posTag;
    private String prevPosTag;
    private String nextPosTag;
    private double positionInLine;  //在实际的没有分词的内容下的位置
    private String depencyTag;  //依存分析的标签

    private Boolean isConnective;   //是否是连词，因为在判断的过程中会不断的清除
    private Boolean isInterConnective; //是否是句内连词,默认是true
    private Boolean isParallelWord; //是否是并列关联词：虽然...但是

    //确定连词链接的两个EDU相关的变量
    private DSAArgument arg1;
    private DSAArgument arg2;

    private Tree connNode;              //在句法分析树中的连词节点，注意这是去除了泛化之后的节点(沿着单链上升：不过--AD---ADVP)
    private ArrayList<Tree> arg2Nodes; //主要是因为arg2可能是由多个并列在同一级的Tree Node组成。
    private ArrayList<Tree> arg1Nodes; //

    private DSAEDU arg2EDU;
    private DSAEDU arg1EDU;

    private Integer depth;              //同EDU的层次一样，每个连词也有对应的depth.

    private String expRelType;
    private double expRelProbality;

    private int beginIndex1;    //针对并列连词使用的两个值
    private int beginIndex2;

    public DSAConnective(String content, int sentID)
    {
        this.content    = content;
        this.sentID     = sentID;

        this.posTag     = null;

        this.depencyTag = null;

        if( this.content.contains(Constants.Parallel_Word_Seperator) )
        {
            this.isParallelWord = true;
        }
        else this.isParallelWord = false;

        this.isConnective = true;
        this.isInterConnective = true;

        this.connNode  = null;
        this.arg1Nodes = new ArrayList<Tree>();
        this.arg2Nodes = new ArrayList<Tree>();

        this.arg1EDU   = null;
        this.arg2EDU   = null;
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

    public ArrayList<Tree> getArg2Nodes() {
        return arg2Nodes;
    }

    public void setArg2Nodes(ArrayList<Tree> arg2Nodes) {
        this.arg2Nodes = arg2Nodes;
    }

    public ArrayList<Tree> getArg1Nodes() {
        return arg1Nodes;
    }

    public void setArg1Nodes(ArrayList<Tree> arg1Nodes) {
        this.arg1Nodes = arg1Nodes;
    }

    public Tree getConnNode() {
        return connNode;
    }

    public void setConnNode(Tree connNode)
    {
        this.connNode = connNode;
    }

    public DSAEDU getArg2EDU() {
        return arg2EDU;
    }

    public void setArg2EDU(DSAEDU arg2EDU) {
        this.arg2EDU = arg2EDU;
    }

    public DSAEDU getArg1EDU() {
        return arg1EDU;
    }

    public void setArg1EDU(DSAEDU arg1EDU) {
        this.arg1EDU = arg1EDU;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getExpRelType() {
        return expRelType;
    }

    public void setExpRelType(String expRelType) {
        this.expRelType = expRelType;
    }

    public double getExpRelProbality() {
        return expRelProbality;
    }

    public void setExpRelProbality(double expRelProbality) {
        this.expRelProbality = expRelProbality;
    }

    public Boolean getInterConnective() {
        return isInterConnective;
    }

    public void setInterConnective(Boolean interConnective) {
        isInterConnective = interConnective;
    }

    public Boolean getIsConnective() {
        return isConnective;
    }

    public void setIsConnective(Boolean connective) {
        isConnective = connective;
    }


    public int getBeginIndex1() {
        return beginIndex1;
    }

    public void setBeginIndex1(int beginIndex1) {
        this.beginIndex1 = beginIndex1;
    }

    public int getBeginIndex2() {
        return beginIndex2;
    }

    public void setBeginIndex2(int beginIndex2) {
        this.beginIndex2 = beginIndex2;
    }

    public int getSentID() {
        return sentID;
    }

    public void setSentID(int sentID) {
        this.sentID = sentID;
    }
}
