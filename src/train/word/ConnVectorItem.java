package train.word;

import common.Constants;

/**
 * ��ʹ�û���ѧϰ����ѵ��ʱ��������Ҫ����ѵ����ע���������ɱ�ע���ݡ�
 * ������ǲ���һ��������װÿ����ע�е��������ϡ�
 *
 * ÿһ�����ʾ��һ����ע�������ݡ�
 * User: Ji JianHui
 * Time: 2014-03-05 20:14
 * Email: jhji@ir.hit.edu.cn
 */
public class ConnVectorItem
{
    private String content; //�ü�¼��ʵ�ʵĴʵ�����

    private int    label; //��ע������Ƿ�������

    private String pos;     //��ѡ�Ĵ����ǹ̶���
    private String prevPos;
    private String nextPos;
    private String relateTag;
    private String prev2Pos;
    private String ssOrPs;         //������ͣ�SS��PS
    private String prev1Content;
    private String prev2Content;
    private int syntacticCategory; //������ʾ䷨���ԣ�0Ϊ�������ʣ�1Ϊ��������
    private int leftSiblings;       //���ֵܸ���
    private int rightSiblings;      //���ֵܸ���

    public void setLeftSiblings(int leftSiblings) {
        this.leftSiblings = leftSiblings;
    }

    public void setRightSiblings(int rightSiblings) {
        this.rightSiblings = rightSiblings;
    }

    public int getLeftSiblings() {
        return leftSiblings;
    }

    public int getRightSiblings() {
        return rightSiblings;
    }

    public int getSyntacticCategory() {
        return syntacticCategory;
    }

    public void setSyntacticCategory(int syntacticCategory) {
        this.syntacticCategory = syntacticCategory;
    }

    public String getPrev1Content() {
        return prev1Content;
    }

    public void setPrev1Content(String prev1Content) {
        this.prev1Content = prev1Content;
    }

    public void setPrev2Content(String prev2Content) {
        this.prev2Content = prev2Content;
    }

    public String getPrev2Content() {
        return prev2Content;
    }


    //��Ϊ��������һ������ʵ��ֵ����˲�����������������
    //private double[] posFeatures;
    //private double[] nextPosFeatures;
    //private double[] prevPosFeatures;


    private double length;     //���ʱ���ĳ���
    private double positionInLine;  //�ھ����е�λ����Ϣ


    private double ambiguity;       //�����������ʴʵ���ָʾ��ϵ������ȡ�ָʾ���Ĺ�ϵ�ĸ���
    private double occurInDict;    //�����ʴʵ��г��ֵĴ���,���û�г��֣���Ĭ��Ϊ0

    private int connNum;    //�ô���Ϊ���ʵĸ���
    private int notConnNum; //�ôʲ���Ϊ���ʵĸ���

    public ConnVectorItem(String content)
    {
        this.content         = content;

        this.label           = Constants.Labl_Not_ConnWord;

        //this.posFeatures     = new double[Constants.posTagsNum];
        //this.nextPosFeatures = new double[Constants.posTagsNum];
       // this.prevPosFeatures = new double[Constants.posTagsNum];
        this.ssOrPs = new String("SS");     //SS is more possible��

        this.length          = 0;
        this.positionInLine  = -1;

        this.ambiguity       = 1.0;
        this.occurInDict     = 0.0;

        this.connNum         = 0;
        this.notConnNum      = 0;
    }

    /**
     * ���ñ�ע����ת��ΪLibSvmҪ��ĸ�ʽ����, libsvmҪ������ݸ�ʽ����
     * label index1:value1 index2:value2 ...
     * ���ж��ڷ�����˵labelΪ���ʶ��ָ�����ݵ�����
     * Index�Ǵ�1��ʼ����Ȼ����value��ÿһά������ֵ��
     * �����ǣ�pos��prevPos��nextPos
     * @return
     */
    public String toLineForLibSvm()
    {
        //��ȡpos, prevPos��nextPos�����ά��
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

        //Ѱ��������������ά��
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
        //��ȡpos, prevPos��nextPos�����ά��
        int posIndex  = 0, prevIndex    = 0, nextIndex = 0;
        int prev2Index = 0;
        int connIndex = 0, notConnIndex = 0;

        if( pos == null )     pos = "w";
        if( prevPos == null ) prevPos = "w";
        if( prev2Pos == null)  prev2Pos = "w";
        if( nextPos == null ) nextPos = "w";

        if( prevPos.equalsIgnoreCase("null") ) prevPos = "w";
        if( nextPos.equalsIgnoreCase("null") ) nextPos = "w";
        if( prev2Pos.equalsIgnoreCase("null") )   prev2Pos = "w";

        for( int index = 0; index < Constants.ansjPosTagsNum; index++ )
        {
            String curPos = Constants.ansjPosTags[index];

            if( pos.equalsIgnoreCase(curPos) )     posIndex = index + 1;
            if( prevPos.equalsIgnoreCase(curPos) ) prevIndex = index + 1;
            if( nextPos.equalsIgnoreCase(curPos) ) nextIndex = index + 1;
            if( prev2Pos.equalsIgnoreCase(curPos) ) prev2Index = index + 1;
        }


        String line = String.valueOf( this.label );

        line += " 1:" + this.content.length();
        line += " 2:" + this.positionInLine;
        line += " 3:" + this.ambiguity;
        line += " 4:" + this.occurInDict;
        line += " 5:" + this.connNum;
        line += " 6:" + this.notConnNum;

        posIndex  = 7 + posIndex;
        prevIndex = 8 + Constants.ansjPosTagsNum + prevIndex;
        nextIndex = 9 + Constants.ansjPosTagsNum * 2 + nextIndex;

        line += " " + posIndex  + ":1";
        line += " " + prevIndex + ":1";
        line += " " + nextIndex + ":1";

        return line;
    }

    /***��Item�����ݴ�ӡ�������Է�����**/
    public String toLineForView()
    {
        String line = String.valueOf( this.label );

        line += " 0:" + this.content;
        line += " posInLine:" + this.positionInLine;
        line += " ambiguity:" + this.ambiguity;
        line += " occurInDict:" + this.occurInDict;
        line += " connNum:" + this.connNum;
        line += " NotConnNum:" + this.notConnNum;

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


    public void setPrev2Pos(String prevPos) {
        this.prev2Pos = prevPos;
    }


    public String getPrev2Pos() {
        return prev2Pos;
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

    public int getConnNum() {
        return connNum;
    }

    public void setConnNum(int connNum) {
        this.connNum = connNum;
    }

    public int getNotConnNum() {
        return notConnNum;
    }

    public void setNotConnNum(int notConnNum) {
        this.notConnNum = notConnNum;
    }

    public void setSsOrPs(String ssOrPs){
        this.ssOrPs = ssOrPs;
    }

    public String getSSOrPS(){
        return ssOrPs;
    }

}
