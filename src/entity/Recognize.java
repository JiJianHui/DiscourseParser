package entity;

import entity.train.SenseRecord;
import org.dom4j.*;

import java.util.HashSet;

/**
 * 主要的任务是：
 * 1：用来完成连词测试的准确性，训练语料为人工标记好的语料。
 * User: Ji JianHui
 * Time: 2014-02-21 21:49
 * Email: jhji@ir.hit.edu.cn
 */
public class Recognize
{
    //正确识别连词和错误识别联词的个数
    public Integer tt;
    public Integer ft;

    //正确识别为没有和错误识别为没有连词的个数
    public Integer tf;
    public Integer ff;

    public Double pRate;
    public Double rRate;
    public Double fRate;

    public HashSet<SenseRecord> rawRecords;


    public Recognize()
    {
        this.tt = 0;
        this.ft = 0;
        this.tf = 0;
        this.ff = 0;

        this.pRate = 0.0;
        this.rRate = 0.0;
        this.fRate = 0.0;
    }

    public void connectiveRecognize() throws DocumentException
    {

    }
}
