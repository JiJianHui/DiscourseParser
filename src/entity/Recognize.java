package entity;

import entity.train.SenseRecord;
import org.dom4j.*;

import java.util.HashSet;

/**
 * ��Ҫ�������ǣ�
 * 1������������ʲ��Ե�׼ȷ�ԣ�ѵ������Ϊ�˹���Ǻõ����ϡ�
 * User: Ji JianHui
 * Time: 2014-02-21 21:49
 * Email: jhji@ir.hit.edu.cn
 */
public class Recognize
{
    //��ȷʶ�����ʺʹ���ʶ�����ʵĸ���
    public Integer tt;
    public Integer ft;

    //��ȷʶ��Ϊû�кʹ���ʶ��Ϊû�����ʵĸ���
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
