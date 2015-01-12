package entity.train;

import common.Constants;
import common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ÿ��wordֻ�ܳ���һ�Σ�����ͬ��word��content�ǲ�ͬ�ġ������������Ϣȴ������������ָʾ�ľ���ϵ��
 * ����Ǳ��������й�������Ϣ��������Ϣ.
 * User: Ji JianHui
 * Time: 2014-02-20 11:12
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAWordDictItem
{
    private String content;
    private Integer expNum;
    private Integer impNum;

    private boolean isParallelWord; //�Ƿ��ǲ��й����ʣ���Ȼ...����

    private HashMap<String, Integer> expRelations;
    private HashMap<String, Integer> impRelations;

    private double mostExpProbality;
    private double mostImpProbality;
    private String mostExpProbalityRelNO;
    private String mostImpProbalityRelNO;

    /**
     * ���췽��1����Ҫ���ڳ����Զ�����itemʹ��
     * @param content
     */
    public DSAWordDictItem(String content)
    {
        this.content = content;

        this.expNum  = 0;
        this.impNum  = 0;

        this.mostExpProbalityRelNO = Constants.DefaultRelNO;
        this.mostImpProbalityRelNO = Constants.DefaultRelNO;

        this.expRelations = new HashMap<String, Integer>();
        this.impRelations = new HashMap<String, Integer>();

        this.isParallelWord = content.contains(";");
    }

    /**
     * ���Ѿ�����õĴʵ��ļ��м��أ�ÿһ�ж���ʾһ�������Ĵʵ���Ŀ��exp:ֻ��������Ĭ�ϵĹ��췽����������
     * Ȼ��	3	1	2	1	1	[X]1-2-1	1	[Y]1-2-1	2
     * @param line
     */
    public DSAWordDictItem(String line, boolean exp)
    {
        String[] lists  = line.trim().split("\t");

        this.content  = lists[0].trim().replace("...", ";");
        this.isParallelWord = content.contains(";");

        this.expNum = Integer.valueOf(lists[2]);
        this.impNum = Integer.valueOf(lists[3]);

        this.expRelations = new HashMap<String, Integer>();
        this.impRelations = new HashMap<String, Integer>();

        for(int index = 6; index < lists.length; index += 2)
        {
            String relNO = lists[index];

            if( relNO.startsWith("[X]") )
            {
                relNO = relNO.substring(3);
                this.expRelations.put( relNO, Integer.valueOf(lists[index+1]) );
            }
            else
            {
                relNO = relNO.substring(3);
                this.impRelations.put( relNO, Integer.valueOf(lists[index+1]) );
            }
        }
        this.setMostExpProbalityRelNO();
        this.setMostImpProbalityRelNO();
    }

    /**
     * �趨������ָʾ�������Ĺ�ϵ���
     */
    public void setMostExpProbalityRelNO()
    {
        Integer max  = 0;
        String relNO = Constants.DefaultRelNO;

        for( Map.Entry<String, Integer> entry: expRelations.entrySet() )
        {
            if( entry.getValue() > max )
            {
                max = entry.getValue();
                relNO = entry.getKey();
            }
        }

        this.mostExpProbalityRelNO = relNO;

        if( this.expNum == 0 )
            return;
        else
            this.mostExpProbality = (max * 1.0) / (this.expNum * 1.0);
    }

    public void setMostImpProbalityRelNO()
    {
        Integer max = 0;
        String relNO = Constants.DefaultRelNO;

        for( Map.Entry<String, Integer> entry: impRelations.entrySet() )
        {
            if( entry.getValue() > max )
            {
                max = entry.getValue();
                relNO = entry.getKey();
            }
        }
        this.mostImpProbalityRelNO = relNO;
        this.mostImpProbality = (max * 1.0) / (this.impNum * 1.0);
    }


    //���¹�ϵ�б�
    public void addNewExpRel(String relNO, int num)
    {
        if( this.expRelations.containsKey(relNO) )
        {
            num = num + this.expRelations.get(relNO);
        }

        this.expNum += num;
        this.expRelations.put(relNO, num);
    }

    public void addNewImpRel(String relNO, int num)
    {
        if( this.impRelations.containsKey(relNO) )
        {
            num = num + this.impRelations.get(relNO);
        }

        this.impNum += num;
        this.impRelations.put(relNO, num);
    }


    /**
     * ��һ���ʵ���Ŀת��Ϊһ���ַ�����ʾ��Ϊ����д���ļ���׼����д���ʽ���£�
     * ��ʽΪ��Ȼ��	3	1	2	1	1	[X]1-2-1	1	[Y]1-2-1	2
     * @return
     */
    public String toLine()
    {
        Integer allNum = this.expNum + this.impNum;

        String result = content + "\t" + allNum + "\t" + expNum + "\t" + impNum;
        result += "\t" + this.expRelations.size() + "\t" + this.impRelations.size();

        //����expRelation
        ArrayList<Map.Entry<String,Integer>> sortedExpRelations =  util.sortHashMap(this.expRelations,false);

        for( Map.Entry<String, Integer> entry : sortedExpRelations )
        {
            result += "\t" + entry.getKey() + "\t" + entry.getValue();
        }

        //����impRelation
        ArrayList<Map.Entry<String,Integer>> sortedImpRelations =  util.sortHashMap(this.expRelations,false);

        for( Map.Entry<String, Integer> entry : sortedImpRelations )
        {
            result += "\t" + entry.getKey() + "\t" + entry.getValue();
        }

        return result;
    }

    /**
     * ����һ���������кϲ������շ���һ���µĴ�����
     * @return
     */
    public DSAWordDictItem meregeItem(DSAWordDictItem other)
    {
        DSAWordDictItem mergedItem = new DSAWordDictItem( this.content );

        mergedItem.expRelations.putAll(this.expRelations);
        mergedItem.setExpNum(this.expNum);
        mergedItem.impRelations.putAll(this.impRelations);
        mergedItem.setImpNum(this.impNum);

        Iterator expIte = other.getExpRelations().entrySet().iterator();
        Iterator impIte = other.getExpRelations().entrySet().iterator();

        while( expIte.hasNext() )
        {
            Map.Entry entry = (Map.Entry) expIte.next();

            String    relNO = (String) entry.getKey();
            Integer   num   = (Integer) entry.getValue();

            mergedItem.addNewExpRel(relNO, num);
        }

        while( impIte.hasNext() )
        {
            Map.Entry entry = (Map.Entry) impIte.next();

            String    relNO = (String) entry.getKey();
            Integer   num   = (Integer) entry.getValue();

            mergedItem.addNewImpRel(relNO, num);
        }

        mergedItem.setMostExpProbalityRelNO();
        mergedItem.setMostImpProbalityRelNO();

        return mergedItem;
    }

    /**
     * ʹ����һ��item�������Լ�������
     * @param other
     */
    public void updateWithItem(DSAWordDictItem other)
    {
        if( !this.content.equalsIgnoreCase(other.getContent()) ) return;

        //��Ϊ�Ѿ���addNewExpRel���������������������˴˴�������Ҫ�ֶ�����
        //this.expNum += other.getExpNum();
        //this.impNum += other.getImpNum();

        Iterator ite = other.getExpRelations().entrySet().iterator();

        while( ite.hasNext() )
        {
            Map.Entry entry = (Map.Entry) ite.next();
            this.addNewExpRel(( String)entry.getKey(), (Integer)entry.getValue() );
        }

        ite = other.getImpRelations().entrySet().iterator();

        while( ite.hasNext() )
        {
            Map.Entry entry = (Map.Entry) ite.next();
            this.addNewImpRel( (String)entry.getKey(), (Integer)entry.getValue() );
        }

        this.setMostExpProbalityRelNO();
        this.setMostImpProbalityRelNO();
    }

    //getter and setter
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getExpNum() {
        return expNum;
    }

    public void setExpNum(Integer expNum) {
        this.expNum = expNum;
    }

    public Integer getImpNum() {
        return impNum;
    }

    public void setImpNum(Integer impNum) {
        this.impNum = impNum;
    }

    public Integer getExpKind() {

        return this.expRelations.size();
    }


    public Integer getImpKind() {
        return this.impRelations.size();
    }


    public HashMap<String, Integer> getExpRelations() {
        return expRelations;
    }

    public void setExpRelations(HashMap<String, Integer> expRelations) {
        this.expRelations = expRelations;
    }

    public HashMap<String, Integer> getImpRelations() {
        return impRelations;
    }

    public void setImpRelations(HashMap<String, Integer> impRelations) {
        this.impRelations = impRelations;
    }

    public String getMostExpProbalityRelNO() {
        return mostExpProbalityRelNO;
    }

    public void setMostExpProbalityRelNO(String mostExpProbalityRelNO) {
        this.mostExpProbalityRelNO = mostExpProbalityRelNO;
    }

    public String getMostImpProbalityRelNO() {
        return mostImpProbalityRelNO;
    }

    public void setMostImpProbalityRelNO(String mostImpProbalityRelNO) {
        this.mostImpProbalityRelNO = mostImpProbalityRelNO;
    }


    public double getMostExpProbality() {
        return mostExpProbality;
    }


    public double getMostImpProbality() {
        return mostImpProbality;
    }

    public boolean isParallelWord() {
        return isParallelWord;
    }
}
