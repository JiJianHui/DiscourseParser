package entity.train;

//����ʵ��ƪ�¼���������ģ�͵�ͼģ��

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Li Jiaqi
 * Time: 2015-06-29 11:03
 * Email: jqli@ir.hit.edu.cn
 */
public class ImageGraph {

    private VNode[] mVexs;  // ��������
    private int nVertex;   //������Ŀ



    private int nRawCorpusTriple;       //ԭ����Ԫ����Ŀ



    private int nBackTriple;            //������Ԫ����Ŀ
    private double[][] dWeightMatrix;   //��Ȩ�ؾ���
//    private double [][] dRelationMatrix;  //����ϵ����

    //��
    private class ENode {
//        int ivex;       // �ñ���ָ��Ķ����λ��
        double dWeight;  //��Ȩ��
        ENode nextEdge;   // ָ����һ������ָ��
        String strDiscourseRelation;    //����ϵ���

    }

    // ����
    private class VNode {
        String strName;       //��Ԫ������
        ArrayList<WordVector> wordVectorArrayList;  //��Ԫ�������б�
        WordVector tripleWordVector;    //��Ԫ������
        double dWeight;     //����Ȩ��
    };

    /**
     *���캯������ʼ���ڵ�Ȩ�أ���ʼ�����ֱ�Ȩ��
     * ����ԭ����Ԫ��֮�䶼�бߣ�ԭ����Ԫ���뱳����Ԫ��֮�䶼�б�
     * ����������Ԫ��֮��û�б�
     * @param wordVectorHashMap
     */
    public ImageGraph(HashMap<String,ArrayList<WordVector> > wordVectorHashMap, int nRawCorpusTriple){

        this.setnVertex(wordVectorHashMap.size());
        mVexs = new VNode[nVertex];
        dWeightMatrix = new double[nVertex][nVertex];

        Iterator iterator = wordVectorHashMap.entrySet().iterator();
        int nVertex = this.getnVertex();

        //�������нڵ㣬���ڵ�Ȩ�ظ����ʼֵ
        int index = 0;
        for ( Map.Entry<String,ArrayList<WordVector>> entry : wordVectorHashMap.entrySet()){
           mVexs[index].dWeight =1;         //ÿ������Ȩ�س��Ի�Ϊ1
           mVexs[index].strName = entry.getKey();
           mVexs[index].wordVectorArrayList = entry.getValue();
        }

        this.setnRawCorpusTriple(nRawCorpusTriple);
        this.setnVertex(wordVectorHashMap.size());
        this.setnBackTriple(wordVectorHashMap.size() - nRawCorpusTriple);  //������Ԫ�����Ŀ = ȫ����Ԫ�����Ŀ - ԭ����Ԫ�����Ŀ

        //��Ȩ�ؾ��������ֵΪ0
        for(int i = 0; i < nVertex; i++){
            for (int j = 0; j < nVertex; j++){
                dWeightMatrix[0][0] = 0;
            }
        }


    }

    public int getnVertex() {
        return nVertex;
    }

    public void setnVertex(int nVertex) {
        this.nVertex = nVertex;
    }

    public void setdWeightMatrix(int i, int j,double x) {
        this.dWeightMatrix[i][j] = x;
    }

    public double getdWeightMatrix(int i, int j) {
        return dWeightMatrix[i][j];
    }

    public int getnRawCorpusTriple() {
        return nRawCorpusTriple;
    }

    public void setnRawCorpusTriple(int nRawCorpusTriple) {
        this.nRawCorpusTriple = nRawCorpusTriple;
    }

    public int getnBackTriple() {
        return nBackTriple;
    }

    public void setnBackTriple(int nBackTriple) {
        this.nBackTriple = nBackTriple;
    }


    public static void main(String args[]){

    }


}
