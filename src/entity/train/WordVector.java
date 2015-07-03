package entity.train;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-06-10 11:21
 * Email: jhji@ir.hit.edu.cn
 */
public class WordVector
{
    public String wName;
    public Double[] wVector;

    public String getwTripleContent() {
        return wTripleContent;
    }

    public void setwTripleContent(String wTripleContent) {
        this.wTripleContent = wTripleContent;
    }

    public String wTripleContent;

    public WordVector(String line)
    {
        String[] lists = line.split(" ");
        int     length = lists.length;

        wName   = lists[0];
        wVector = new Double[length - 1];

        for( int i = 1; i < length; i++ )
        {
            wVector[i-1] = Double.valueOf( lists[i] );
        }
    }

    public WordVector(int nDimention, String line)
    {
        this.wVector = new Double[nDimention];

        String[] lists = line.split(" ");
        int     length = lists.length;

        wName   = lists[0];
        wVector = new Double[length - 1];

        for( int i = 1; i < length; i++ )
        {
            wVector[i-1] = Double.valueOf( lists[i] );
        }
    }

    public WordVector(int nDimention)
    {
        this.wName = "";
        this.wVector = new Double[nDimention];

        for( int index = 0; index < nDimention; index++ )
        {
            this.wVector[index] = 0.0;
        }
    }


    public WordVector()
    {
        this.wName = "";
        this.wVector = new Double[50];

        for( int index = 0; index < 50; index++ )
        {
            this.wVector[index] = 0.0;
        }
    }

    public void addOtheVector(WordVector other)
    {
        for(int i = 0; i < other.wVector.length; i++)
        {
            this.wVector[i] = this.wVector[i] + other.wVector[i];
        }
    }

    public void minusOtherVector(WordVector other)
    {
        for(int i = 0; i < other.wVector.length; i++)
        {
            this.wVector[i] = this.wVector[i] - other.wVector[i];
        }
    }
}
