package jiaqi;

import common.Constants;
import common.util;
import entity.train.SenseRecord;
import org.dom4j.DocumentException;
import resource.Resource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-09-28 12:32
 * Email: jhji@ir.hit.edu.cn
 */
public class Jacky {

    public static void main(String[] args) throws DocumentException, IOException {
        Resource.LoadRawRecord();
        System.out.println(Resource.Raw_Train_Annotation.size());

        ArrayList results = new ArrayList();
        for(SenseRecord sRecord:Resource.Raw_Train_Annotation){
            if(sRecord.getType().equals(Constants.IMPLICIT)){
                //System.out.println(sRecord.getRelNO()+sRecord.getArg1()+sRecord.getArg2());
                String str = sRecord.getRelNO()+sRecord.getArg1()+" | "+sRecord.getArg2();
                results.add(str);

            }
        }
        util.writeLinesToFile("D:\\output.txt",results);
    }

}


