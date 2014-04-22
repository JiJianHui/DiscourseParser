package entity;

/**
 * 该记录保存了新版本下的所有的训练数据，每个数据作为一条Record来使用。
 * User: Ji JianHui
 * Time: 2014-02-21 22:33
 * Email: jhji@ir.hit.edu.cn
 */
public class SenseRecord
{
    private Long  id;
    private String fPath;

    private String type;
    private String relNO;

    private String text;
    private String connective;
    private int connBeginIndex;

    private String arg1;
    private String arg2;

    private String annotation;

    public SenseRecord(String type, String relNO)
    {
        this.type  = type;
        this.relNO = relNO;
        this.connBeginIndex = 0;
    }

    //------------Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelNO() {
        return relNO;
    }

    public void setRelNO(String relNO) {
        this.relNO = relNO;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getConnective() {
        return connective;
    }

    public void setConnective(String connective) {
        this.connective = connective;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getfPath() {
        return fPath;
    }

    public void setfPath(String fPath) {
        this.fPath = fPath;
    }

    public int getConnBeginIndex() {
        return connBeginIndex;
    }

    public void setConnBeginIndex(int connBeginIndex) {
        this.connBeginIndex = connBeginIndex;
    }
}
