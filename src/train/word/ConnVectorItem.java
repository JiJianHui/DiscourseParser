package train.word;

import common.Constants;

/**
 * 在使用机器学习方法训练时候首先需要根据训练标注语料来生成标注数据。
 * 因此我们采用一个类来封装每条标注行的特征集合。
 *
 * 每一个类表示了一个标注向量数据。
 * User: Ji JianHui
 * Time: 2014-03-05 20:14
 * Email: jhji@ir.hit.edu.cn
 */
public class ConnVectorItem
{
    private String content; //该记录的实际的词的内容

    private int    label; //标注结果，是否是连词

    private String pos;     //可选的词性是固定的
    private String prevPos;
    private String nextPos;
    private String relateTag;

    private String neTag;

    //因为特征都是一个个的实数值，因此采用下面的数组来存放
    //private double[] posFeatures;
    //private double[] nextPosFeatures;
    //private double[] prevPosFeatures;


    private double length;     //连词本身的长度
    private double positionInLine;  //在句子中的位置信息


    private double ambiguity;       //该连词在连词词典中指示关系的歧义度。指示最大的关系的概率
    private double occurInDict;    //在连词词典中出现的次数,如果没有出现，则默认为0

    public ConnVectorItem(String content)
    {
        this.content         = content;

        this.label           = Constants.Labl_Not_ConnWord;

        //this.posFeatures     = new double[Constants.posTagsNum];
        //this.nextPosFeatures = new double[Constants.posTagsNum];
       // this.prevPosFeatures = new double[Constants.posTagsNum];

        this.length          = 0;
        this.positionInLine  = -1;

        this.ambiguity       = 1.0;
        this.occurInDict     = 0.0;
    }

    /**
     * 将该标注样例转换为LibSvm要求的格式保存, libsvm要求的数据格式如下
     * label index1:value1 index2:value2 ...
     * 其中对于分类来说label为类标识，指定数据的种类
     * Index是从1开始的自然数，value是每一维的特征值。
     * 特征是：pos，prevPos，nextPos
     * @return
     */
    public String toLineForLibSvm()
    {
        //获取pos, prevPos和nextPos的相对维数
        int posIndex = 0, prevIndex = 0, nextIndex = 0;

        if( pos == null )     pos = "wp";
        if( prevPos == null ) prevPos = "wp";
        if( nextPos == null ) nextPos = "wp";
        if( relateTag == null ) relateTag = "ADV";

        for( int index = 0; index < Constants.posTagsNum; index++ )
        {
            String curPos = Constants.posTags[index];
            if( pos.equalsIgnoreCase(curPos) ) posIndex = index + 1;
            if( prevPos.equalsIgnoreCase(curPos) ) prevIndex = index + 1;
            if( nextPos.equalsIgnoreCase(curPos) ) nextIndex = index + 1;
        }

        //寻找依存分析的相对维数
        int relateIndex = 0;

        for( int index = 0; index < Constants.relateTagsNum; index++ )
        {
            String curRelate = Constants.relateTags[index];
            if( relateTag.equalsIgnoreCase(curRelate) )
            {
                relateIndex++;
                break;
            }
        }

        String line = String.valueOf( this.label );

        line += " 1:" + this.content.length();
        line += " 2:" + this.positionInLine;
        line += " 3:" + this.ambiguity;
        line += " 4:" + this.occurInDict;

        posIndex  = posIndex  + 5;
        prevIndex = prevIndex + 6 + Constants.posTagsNum;
        nextIndex = nextIndex + 7 + Constants.posTagsNum + Constants.posTagsNum;

        relateIndex = relateIndex + 8 + 3* Constants.posTagsNum;

        line += " " + posIndex  + ":1";
        line += " " + prevIndex + ":1";
        line += " " + nextIndex + ":1";
        line += " " + relateIndex + ":1";

        return line;
    }

    public String toLineForLibSvmWithAnsj()
    {
        //获取pos, prevPos和nextPos的相对维数
        int posIndex = 0, prevIndex = 0, nextIndex = 0;

        if( pos == null )     pos = "w";
        if( prevPos == null ) prevPos = "w";
        if( nextPos == null ) nextPos = "w";

        if( prevPos.equalsIgnoreCase("null") ) prevPos = "w";
        if( nextPos.equalsIgnoreCase("null") ) nextPos = "w";

        for( int index = 0; index < Constants.ansjPosTagsNum; index++ )
        {
            String curPos = Constants.ansjPosTags[index];

            if( pos.equalsIgnoreCase(curPos) )     posIndex = index + 1;
            if( prevPos.equalsIgnoreCase(curPos) ) prevIndex = index + 1;
            if( nextPos.equalsIgnoreCase(curPos) ) nextIndex = index + 1;
        }


        String line = String.valueOf( this.label );

        line += " 1:" + this.content.length();
        line += " 2:" + this.positionInLine;
        line += " 3:" + this.ambiguity;
        line += " 4:" + this.occurInDict;

        posIndex  = posIndex  + 5;
        prevIndex = prevIndex + 6 + Constants.ansjPosTagsNum;
        nextIndex = nextIndex + 7 + Constants.ansjPosTagsNum + Constants.ansjPosTagsNum;

        line += " " + posIndex  + ":1";
        line += " " + prevIndex + ":1";
        line += " " + nextIndex + ":1";

        return line;
    }

    //getter and setter

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

//    public double[] getPosFeatures() {
//        return posFeatures;
//    }
//
//    public void setPosFeatures(double[] posFeatures) {
//        this.posFeatures = posFeatures;
//    }
//
//    public double[] getNextPosFeatures() {
//        return nextPosFeatures;
//    }
//
//    public void setNextPosFeatures(double[] nextPosFeatures) {
//        this.nextPosFeatures = nextPosFeatures;
//    }
//
//    public double[] getPrevPosFeatures() {
//        return prevPosFeatures;
//    }
//
//    public void setPrevPosFeatures(double[] prevPosFeatures) {
//        this.prevPosFeatures = prevPosFeatures;
//    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getPositionInLine() {
        return positionInLine;
    }

    public void setPositionInLine(double positionInLine) {
        this.positionInLine = positionInLine;
    }

    public double getAmbiguity() {
        return ambiguity;
    }

    public void setAmbiguity(double ambiguity) {
        this.ambiguity = ambiguity;
    }

    public double getOccurInDict() {
        return occurInDict;
    }

    public void setOccurInDict(double occurInDict) {
        this.occurInDict = occurInDict;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getPrevPos() {
        return prevPos;
    }

    public void setPrevPos(String prevPos) {
        this.prevPos = prevPos;
    }

    public String getNextPos() {
        return nextPos;
    }

    public void setNextPos(String nextPos) {
        this.nextPos = nextPos;
    }

    public String getRelateTag() {
        return relateTag;
    }

    public void setRelateTag(String relateTag) {
        this.relateTag = relateTag;
    }


}
