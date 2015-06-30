import common.util;
import entity.recognize.DSAParagraph;
import entity.recognize.DSASentence;
import entity.train.ImageGraph;
import entity.train.WordVector;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Li Jiaqi
 * Time: 2015-06-26 09:45
 * Email: jqli@ir.hit.edu.cn
 */
public class ImaginationModel {

    public  static ArrayList<String> rawCorpusList = new ArrayList<String>();
    public HashMap<String,ArrayList<WordVector> > rawWordVectorHashMap;
    public HashMap<String,ArrayList<WordVector> > backWordVectorHashMap;
    public HashMap<String,ArrayList<WordVector> > wordVectorHashMap;
    /**
     * �����������ƶ�
     * @param wordVectorOne
     * @param wordVectorTwo
     * @return
     */
    public double cosSimi(WordVector wordVectorOne, WordVector wordVectorTwo)
    {
        double dCosSimilarity = 0;  //Cosine similarity
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

    /**
     * ��������
     * @throws IOException
     */
    public void loadData() throws IOException
    {
        String fCorpusPath = "ImageModel/corpus";  //Corpus File
//        util.readFileToLines(fCorpusPath,rawCorpusList);
        util.readFileToLinesWithEncoding(fCorpusPath, rawCorpusList,"UTF-8");
        System.out.println("[--Info--] Loading Word Vector File from [" + fCorpusPath + "]" );
    }

    /**
     * ����ԭ����Ԫ������
     * @throws IOException
     */
    public void loadRawVector() throws IOException
    {
        String fVectorPath = "ImageModel/predications_lda.vec";  //Corpus File
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLinesWithEncoding(fVectorPath, lines,"UTF-8");

        for(int index = 0; index < lines.size(); index++)
        {
            String line = lines.get(index);
            if ( line.contains("file"))
            {
                String strFileName = line;
                ArrayList<WordVector> wordVectors = new ArrayList<WordVector>();

                line = lines.get(index + 1);
                for(int i = index + 1;!line.contains("file"); )
                {
                    WordVector wordVector = new WordVector(line);
                    wordVectors.add(wordVector);
                    line = lines.get( ++i );
                }
                rawWordVectorHashMap.put(strFileName,wordVectors);
                wordVectorHashMap.put(strFileName,wordVectors);
            }
            else continue;
        }
        System.out.println("[--Info--] Loading Raw Corpuse Word Vector File from [" + fVectorPath + "]" );
//        lines.clear();

    }

    /**
     * ���ر�����Ԫ��
     * @throws IOException
     */
    public void loadBackVector() throws IOException
    {
        String fVectorPath = "ImageModel/predications_lda.vec";  //Corpus File
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLinesWithEncoding(fVectorPath, lines,"UTF-8");

        for(int index = 0; index < lines.size(); index++)
        {
            String line = lines.get(index);
            if ( line.contains("file"))
            {
                String strFileName = line;
                ArrayList<WordVector> wordVectors = new ArrayList<WordVector>();

                line = lines.get(index + 1);
                for(int i = index + 1;!line.contains("file"); )
                {
                    WordVector wordVector = new WordVector(line);
                    wordVectors.add(wordVector);
                    line = lines.get( ++i );
                }
                backWordVectorHashMap.put(strFileName,wordVectors);
                wordVectorHashMap.put(strFileName,wordVectors);
            }
            else continue;
        }
        System.out.println("[--Info--] Loading Background Word Vector File from [" + fVectorPath + "]" );
    }

    /**
     * ����ԭ�����ݣ�
     * �������� \t ��Ԫ��1���� \t ��Ԫ��1���� \t ��Ԫ��2���� \t ��Ԫ��2���� ����
     * @throws IOException
     */
    public void LoadRawCorpus() throws IOException
    {
        String fVectorPath = "ImageModel/predications_lda.vec";  //Corpus File
        ArrayList<String> lines = new ArrayList<String>();     //����ԭ������
        util.readFileToLinesWithEncoding(fVectorPath, lines,"UTF-8");

        for (int i = 0; i < lines.size(); i++){
            String strArray[] = lines.get(i).split("\t");
            int nArraySize = strArray.length;      //ÿһ�еĴ�С

            rawCorpusList.add(strArray[0]);
        }

    }


    /**
     * ��ȡ�����е���ʽ����ϵ
     * @param dp    DiscourseParser
     * @param strSentence ��Ҫ�����ľ���
     * @param nIndexOfSentence ���ӱ��
     * @return ��ʽ����ϵ���
     * @throws IOException
     */
    public String getExplicitRelation(DiscourseParser dp, String strSentence, int nIndexOfSentence) throws IOException
    {
        String strRelation = "";

        DSASentence sentence = new DSASentence(strSentence);
        sentence.setSegContent(strSentence);
        sentence.setId(nIndexOfSentence);

        //1: ���Ƚ���Ԥ���������еײ��NLP�������ִʡ����Ա�ע
        dp.preProcess(sentence, false);
        //2: ʶ�𵥸����ʺ�ʶ�������ʣ�����...����
        dp.findConnWordWithML(sentence);

        //3: ��ȡ�������������ѵ��һ������ط�������C string��C's POS��C+prev
        String wPrev ="" , posOfConn = "" ;
        String wContent  = sentence.getConnWordContent();

        int indexOfConnective = 0;
        String arrayfWords[] = strSentence.split(" ");

        for(int nIndex = 0 ; nIndex < arrayfWords.length ; nIndex++){
            if(arrayfWords[nIndex].equalsIgnoreCase(wContent)){
                indexOfConnective = nIndex;
            }
        }

        try{
            wPrev = arrayfWords[indexOfConnective - 1];
            if (wPrev.isEmpty()){
                wPrev = arrayfWords[indexOfConnective - 2];
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            System.out.println(e.toString());
        }
        posOfConn = sentence.getAnsjWordTerms().get(indexOfConnective).getNatrue().natureStr;
        String strFeature = wContent +" " +  posOfConn +" "+ wContent + wPrev;

        //4��ʹ������ط��������з���
        strRelation =  dp.ClassifyViaMaximumEntrop("explicitModel.txt",strFeature);

        return strRelation;
    }


    public static void main(String args[]) throws IOException, DocumentException
    {
        ImaginationModel imaginationModel = new ImaginationModel();
        //�������ϡ�ԭ����Ԫ��������������Ԫ������
        imaginationModel.loadData();
        imaginationModel.loadRawVector();
        imaginationModel.loadBackVector();

        int nRawCorpusTriple = imaginationModel.rawWordVectorHashMap.size();        //ԭ����Ԫ��ĸ���
        int nBachCorpusTriple = imaginationModel.backWordVectorHashMap.size();      //������Ԫ��ĸ���
        int nAllTriple = imaginationModel.wordVectorHashMap.size();                 //ȫ����Ԫ��ĸ���

        //����DiscourseParser
        DiscourseParser dp = new DiscourseParser();

        //��ÿ�����Ͻ��д�������ȡ�����ϵ
        for(String line: rawCorpusList){

            DSAParagraph paragraph = new DSAParagraph(line);
            ArrayList<String> sentences = util.filtStringToSentence(line);

            int nIndexOfSentence = 0;
            Boolean nArrayOfRelation[] = new Boolean[sentences.size()];

            int nIndexOfState = 0;
            for (Boolean bool: nArrayOfRelation){
                nArrayOfRelation[nIndexOfState++] = false;
            }

            for (String strSentence: sentences){
                String strRelation = imaginationModel.getExplicitRelation(dp,strSentence,nIndexOfSentence);
                nArrayOfRelation[nIndexOfSentence] = true;
            }

        }

        //��ͼ
        ImageGraph imageGraph = new ImageGraph(imaginationModel.wordVectorHashMap, imaginationModel.rawWordVectorHashMap.size());


//        Iterator entryIterator = imaginationModel.wordVectorHashMap.entrySet().iterator();

        //�����������ƶȣ�������ͼ�ı߸���Ȩ��
        for ( Map.Entry<String,ArrayList<WordVector>> entry : imaginationModel.wordVectorHashMap.entrySet()){
            for (int i = 0; i < nAllTriple ; i++){
                for (int j = 0; j < nAllTriple; j++){
                    if (i >= imageGraph.getnRawCorpusTriple() && j >= imageGraph.getnRawCorpusTriple() ){
                        continue;
                    }
                    double x = imaginationModel.cosSimi(entry.getValue().get(i),entry.getValue().get(j));
                    imageGraph.setdWeightMatrix(i,j,x);
                }
            }
        }

        //���򱳾���Ԫ��


        //���ݾ���ϵ���޸ı�Ȩ��


        //�ı�������֤


    }
}