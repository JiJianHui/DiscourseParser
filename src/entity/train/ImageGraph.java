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
    private double[][] dWeightMatrix;   //��Ȩ�ؾ���

    //��
    private class ENode {
        int ivex;       // �ñ���ָ��Ķ����λ��
        double dWeight;  //���Ȩ��
        ENode nextEdge; // ָ����һ������ָ��
    }

    // ����
    private class VNode {
        String strName;
        ArrayList<WordVector> wordVectorArrayList;
        double dWeight;     //����Ȩ��
    };

    //���캯������ʼ���ڵ�Ȩ��
    public ImageGraph(HashMap<String,ArrayList<WordVector> > wordVectorHashMap){

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


    public static void main(String args[]){

    }


}
