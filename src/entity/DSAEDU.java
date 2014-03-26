package entity;

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

    private Integer beginIndex; //°üÀ¨
    private Integer endIndex;   //²»°üÀ¨

    public DSAEDU(String content, String sentContent)
    {
        this.content = content;
        this.sentContent = sentContent;

        this.beginIndex = sentContent.indexOf(content);
        this.endIndex   = beginIndex + content.length();
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
}
