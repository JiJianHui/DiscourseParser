package ImageModel;


import entity.*;
import entity.train.*;


/**
 * Created with IntelliJ IDEA.
 * User: Li Jiaqi
 * Time: 2015-06-26 09:45
 * Email: jqli@ir.hit.edu.cn
 */
public class ImaginationModel {


    public double cosSimi(WordVector wordVectorOne, WordVector wordVectorTwo)
    {
        double dCosSimilarity = 0;
        double dChild = 0; //
        double dSquaresSumOfOne = 0 ,dSquaresSumOfTwo = 0;

        for(int i = 0; i < 50; i++)
        {
            dChild += wordVectorOne.wVector[i] * wordVectorTwo.wVector[i];
            dSquaresSumOfOne += Math.sqrt(wordVectorOne.wVector[i] * wordVectorOne.wVector[i]) ;
            dSquaresSumOfTwo += Math.sqrt(wordVectorTwo.wVector[i] * wordVectorTwo.wVector[i]);
        }

        dCosSimilarity = dChild / (dSquaresSumOfOne * dSquaresSumOfTwo);

        return dCosSimilarity;
    }





    public static void main(String args[]){



//        System.out.println("Hello world");
    }
}
