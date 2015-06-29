package entity.train;

//用于实现篇章级语义联想模型的图模型

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

    private VNode[] mVexs;  // 顶点数组
    private int nVertex;   //顶点数目
    private double[][] dWeightMatrix;   //边权重矩阵

    //边
    private class ENode {
        int ivex;       // 该边所指向的顶点的位置
        double dWeight;  //结点权重
        ENode nextEdge; // 指向下一条弧的指针
    }

    // 顶点
    private class VNode {
        String strName;
        ArrayList<WordVector> wordVectorArrayList;
        double dWeight;     //顶点权重
    };

    //构造函数，初始化节点权重
    public ImageGraph(HashMap<String,ArrayList<WordVector> > wordVectorHashMap){

        this.setnVertex(wordVectorHashMap.size());
        mVexs = new VNode[nVertex];
        dWeightMatrix = new double[nVertex][nVertex];

        Iterator iterator = wordVectorHashMap.entrySet().iterator();
        int nVertex = this.getnVertex();

        //遍历所有节点，给节点权重赋予初始值
        int index = 0;
        for ( Map.Entry<String,ArrayList<WordVector>> entry : wordVectorHashMap.entrySet()){
           mVexs[index].dWeight =1;         //每个结点的权重初试化为1
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
