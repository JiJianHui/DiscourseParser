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

    public ArrayList<String> rawCorpusList;
    public HashMap<String,ArrayList<WordVector> > rawWordVectorHashMap[];
    public ArrayList<WordVector> backWordVectorHashMap;
    public HashMap<String,ArrayList<WordVector> > wordVectorHashMap;
    public HashMap<String,ArrayList<String>> rawCorpusHashMap;  //<文件名,该文件中的句子>
    public ImageGraph imageGraph[];

    /**
     * 构造函数，完成一些初始化
     */
    public ImaginationModel(int nFileNumber)
    {
        rawCorpusList = new ArrayList<String>();
        rawWordVectorHashMap = new HashMap[nFileNumber];
        backWordVectorHashMap = new ArrayList<WordVector>();
        wordVectorHashMap = new HashMap<String, ArrayList<WordVector>>();
        rawCorpusHashMap = new HashMap<String, ArrayList<String>>();
        imageGraph = new ImageGraph[nFileNumber];
    }

    /**
     * 计算余弦相似度
     * @param wordVectorOne
     * @param wordVectorTwo
     * @return
     */
    public double cosSimi(WordVector wordVectorOne, WordVector wordVectorTwo,int nDimention)
    {
        double dCosSimilarity = 0;  //Cosine similarity
        double dChild = 0; //
        double dSquaresSumOfOne = 0 ,dSquaresSumOfTwo = 0;

        for(int i = 0; i < nDimention; i++)
        {
            dChild += wordVectorOne.wVector[i] * wordVectorTwo.wVector[i];
            dSquaresSumOfOne += Math.sqrt(wordVectorOne.wVector[i] * wordVectorOne.wVector[i]) ;
            dSquaresSumOfTwo += Math.sqrt(wordVectorTwo.wVector[i] * wordVectorTwo.wVector[i]);
        }

        dCosSimilarity = dChild / (dSquaresSumOfOne * dSquaresSumOfTwo);

        return dCosSimilarity;
    }

    /**
     * 加载背景三元组
     * @throws IOException
     */
    public void loadBackVector() throws IOException
    {
        String fVectorPath = "ImageModel/bk_2000.vec";  //Corpus File
        ArrayList<String> lines = new ArrayList<String>();
        util.readFileToLinesWithEncoding(fVectorPath, lines,"UTF-8");

        int nDimention = Integer.valueOf(lines.get(0) );    //记录维度信息
        for(int index = 1; index < lines.size(); index++)
        {
            String line = lines.get(index);
            WordVector wordVector = new WordVector(nDimention,line);
            this.backWordVectorHashMap.add(wordVector);
        }
        System.out.println("[--Info--] Loading Background Word Vector File from [" + fVectorPath + "]" );
    }

    /**
     * 加载原文数据；
     * 句子内容 \t 三元组1内容 \t 三元组1向量 \t 三元组2内容 \t 三元组2向量 ……
     * @throws IOException
     */
    public void loadRawCorpus() throws IOException
    {
        String fVectorPath = "ImageModel/discourse_triples.vec";  //Corpus File
        ArrayList<String> lines = new ArrayList<String>();     //加载原文数据-
        util.readFileToLinesWithEncoding(fVectorPath, lines,"UTF-8");

        int nDimention = Integer.valueOf(lines.get(0) );        //保存维度信息

        int nNumberOfFile = 0;
        for (int i = 1; i < lines.size(); i++){

            String line = lines.get(i);

            if ( line.contains("file"))
            {
                String strFileName = line;
                ArrayList<WordVector> wordVectors = new ArrayList<WordVector>(nDimention);

                rawWordVectorHashMap[nNumberOfFile] = new HashMap<String,ArrayList<WordVector>>();

                line = lines.get(i + 1);
                ArrayList<String> arrayListDiscourse = new ArrayList<String>();
                for(int index = i + 1;!line.contains("file"); )
                {
                    String strArray[] = line.split("\t");
                    String strCorpusSentence = strArray[0];     //语料句子的内容
                    rawCorpusList.add(strCorpusSentence);

                    String strTriple[], strWordVector="";

                    int nTriple = (strArray.length - 1) / 2;     //获取句中三元组的个数
                    strTriple = new String[nTriple];

                    for(int j = 1; j <= nTriple; j++){

                        strTriple[j - 1] = strArray [2 * j - 1] ;      //三元组的内容
                        strWordVector = strArray [2 * j];

                        WordVector wordVector = new WordVector(nDimention,strWordVector);
                        wordVector.setwTripleContent(strTriple[j - 1]);
                        wordVectors.add(wordVector);
                    }

                    rawWordVectorHashMap[nNumberOfFile++].put(strFileName,wordVectors);

                    index++;
                    if (index == lines.size())  break;
                    line = lines.get( index );
                }
                rawCorpusHashMap.put(strFileName,arrayListDiscourse);
                wordVectorHashMap.put(strFileName,wordVectors);
            }
            else continue;

        }
    }


    /**
     * 获取句子中的显式句间关系
     * @param dp    DiscourseParser
     * @param strSentence 需要处理的句子
     * @param nIndexOfSentence 句子编号
     * @return 显式句间关系编号
     * @throws IOException
     */
    public String getExplicitRelation(DiscourseParser dp, String strSentence, int nIndexOfSentence) throws IOException
    {
        String strRelation = "";

        DSASentence sentence = new DSASentence(strSentence);
        sentence.setSegContent(strSentence);
        sentence.setId(nIndexOfSentence);

        //1: 首先进行预处理，进行底层的NLP处理：分词、词性标注
        dp.preProcess(sentence, true);
        //2: 识别单个连词和识别并列连词：不仅...而且
        dp.findConnWordWithML(sentence);

        //3: 抽取相关特征，用于训练一个最大熵分类器：C string、C's POS、C+prev
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

        //4：使用最大熵分类器进行分类
        strRelation =  dp.ClassifyViaMaximumEntrop("explicitModel.txt",strFeature);

        return strRelation;
    }


    public static void main(String args[]) throws IOException, DocumentException
    {
        ImaginationModel imaginationModel = new ImaginationModel(900);
        //加载语料、原文三元组向量、背景三元组向量
        imaginationModel.loadRawCorpus();
        imaginationModel.loadBackVector();

        int nRawCorpusTriple = imaginationModel.rawWordVectorHashMap.length;        //原文三元组的个数
        int nBachCorpusTriple = imaginationModel.backWordVectorHashMap.size();      //背景三元组的个数

        //加载DiscourseParser
        DiscourseParser dp = new DiscourseParser();

        int  EXPLICIT_WEIGHT = 2;
        //对每条语料进行处理，获取其句间关系
        for(Map.Entry<String,ArrayList<String>> entry : imaginationModel.rawCorpusHashMap.entrySet()){

            ArrayList<String> arrayListSentences = entry.getValue();

            int nIndexOfSentence = 0;
            int nSentences = arrayListSentences.size();
            int nArrayOfRelation[] = new int[nSentences];


            int nIndexOfState = 0;
            for (int i = 0; i < arrayListSentences.size(); i++){
                nArrayOfRelation[nIndexOfState++] = 0;
            }

            for(int index = 0; index < arrayListSentences.size() - 1; index++){
                DSASentence curSentence  = new DSASentence(arrayListSentences.get(index));
                DSASentence nextSentence = new DSASentence(arrayListSentences.get(index + 1));

                String strExplicitRelation = dp.getCrossExpRel(curSentence, nextSentence);
                if( !strExplicitRelation.isEmpty() ) nArrayOfRelation[nIndexOfSentence] = EXPLICIT_WEIGHT;     //如果
            }

        }

        //建图，每篇文件建一个图
        ImageGraph imageGraph[] = new ImageGraph[nRawCorpusTriple];

        for (int i = 0; i < imaginationModel.rawWordVectorHashMap.length; i++){
            HashMap<String,ArrayList<WordVector>> wordVectorGrap = imaginationModel.rawWordVectorHashMap[i];
            imageGraph[i] = new ImageGraph(wordVectorGrap);
        }

        //计算余弦相似度，给原文三元组的无向图的边赋予权重
        int nIndexOfRawVector = 0;
//        for ( Map.Entry<String,ArrayList<WordVector>> entry : imaginationModel.rawWordVectorHashMap.entrySet()){
//
//            imageGraph[nIndexOfRawVector] = new ImageGraph();
//            for (int i = 0; i < nNumberOfRawCorpusTriple ; i++){
//                for (int j = 0; j < nNumberOfRawCorpusTriple; j++){
//
//                    if (i >= imageGraph.getnRawCorpusTriple() && j >= imageGraph.getnRawCorpusTriple() ){
//                        continue;
//                    }
//                    WordVector wordVectorOne = entry.getValue().get(i);
//                    WordVector wordVectorTwo = entry.getValue().get(j);
//                    double x = imaginationModel.cosSimi(wordVectorOne,wordVectorTwo,20);
//                    imageGraph.setdWeightMatrix(i,j,x);
//                }
//            }


        }

        //计算与背景三元组的相似度，排序背景三元组
//        for ( Map.Entry<String,ArrayList<WordVector>> entry : imaginationModel.rawWordVectorHashMap.entrySet()){
//
//            for(WordVector backWordVector: imaginationModel.backWordVectorHashMap){
////                WordVector rawWordVector = entry.getValue();
//
//            }
//
//        }


        //根据句间关系，修改边权重
        double dDiscourseWeight = 0;




        //文本分类验证



}
