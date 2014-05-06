package entity.recognize;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-03-24 20:55
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAEDU
{
    private String content;
    private String sentContent;

    private Integer beginIndex; //����
    private Integer endIndex;   //������

    private Tree root;  //��EDU�ڶ���ṹ���еĸ�

    private DSAEDU parentEDU;               //����EDU�ڵ�
    private ArrayList<DSAEDU> childrenEDUS; //��ֱ�ӵĺ��ӽڵ�
    private ArrayList<Tree> childrenTree;  //�����˸ýڵ��������п��ܵĺ��ӽڵ�

    private int depth;  //��edu��λ�ڵ����,Ĭ���Ǵ�-1��ʼ������Root-IP,IP��λ�ڵ�depth����λ��0


    public DSAEDU(String content, String sentContent)
    {
        this.content      = content;
        this.sentContent  = sentContent;

        this.beginIndex   = sentContent.indexOf(content);
        this.endIndex     = beginIndex + content.length();

        this.root         = null;
        this.childrenEDUS = new ArrayList<DSAEDU>();
        this.depth        = -1;
    }

    public DSAEDU()
    {
        this.content     = null;
        this.sentContent = null;

        this.beginIndex  = -1;
        this.endIndex    = -1;

        root             = null;
        childrenEDUS     = new ArrayList<DSAEDU>();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getBeginIndex() {
        return beginIndex;
    }

    public void setBeginIndex(Integer beginIndex) {
        this.beginIndex = beginIndex;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public String getSentContent() {
        return sentContent;
    }

    public void setSentContent(String sentContent) {
        this.sentContent = sentContent;
    }

    public Tree getRoot() {
        return root;
    }

    public void setRoot(Tree root) {
        this.root = root;
    }

    public ArrayList<DSAEDU> getChildrenEDUS() {
        return childrenEDUS;
    }

    public void setChildrenEDUS(ArrayList<DSAEDU> childrenEDUS) {
        this.childrenEDUS = childrenEDUS;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public DSAEDU getParentEDU() {
        return parentEDU;
    }

    public void setParentEDU(DSAEDU parentEDU) {
        this.parentEDU = parentEDU;
    }
}
