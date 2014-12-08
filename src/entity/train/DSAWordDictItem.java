package entity.train;

import common.Constants;
import common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 每个word只能出现一次，即不同的word的content是不同的。但是里面的信息却包含了它所能指示的句间关系。
 * 这个是保存了所有关联词信息的总体信息.
 * User: Ji JianHui
 * Time: 2014-02-20 11:12
 * Email: jhji@ir.hit.edu.cn
 */
public class DSAWordDictItem
{
    private String content;
    private Integer expNum;
    private Integer impNum;

    private boolean isParallelWord; //是否是并列关联词：虽然...但是

    private HashMap<String, Integer> expRelations;
    private HashMap<String, Integer> impRelations;

    private double mostExpProbality;
    private double mostImpProbality;
    private String mostExpProbalityRelNO;
    private String mostImpProbalityRelNO;

    /**
     * 构造方法1：主要用于程序自动生成item使用
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
     * 从已经计算好的词典文件中加载，每一行都表示一个完整的词典条目。exp:只是用来和默认的构造方法进行区别。
     * 然后	3	1	2	1	1	[X]1-2-1	1	[Y]1-2-1	2
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
     * 设定该连词指示概率最大的关系编号
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


    //更新关系列表
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
     * 将一个词典条目转换为一行字符串表示，为后续写入文件做准备。写入格式如下：
     * 格式为：然后	3	1	2	1	1	[X]1-2-1	1	[Y]1-2-1	2
     * @return
     */
    public String toLine()
    {
        Integer allNum = this.expNum + this.impNum;

        String result = content + "\t" + allNum + "\t" + expNum + "\t" + impNum;
        result += "\t" + this.expRelations.size() + "\t" + this.impRelations.size();

        //加载expRelation
        ArrayList<Map.Entry<String,Integer>> sortedExpRelations =  util.sortHashMap(this.expRelations,false);

        for( Map.Entry<String, Integer> entry : sortedExpRelations )
        {
            result += "\t" + entry.getKey() + "\t" + entry.getValue();
        }

        //加载impRelation
        ArrayList<Map.Entry<String,Integer>> sortedImpRelations =  util.sortHashMap(this.expRelations,false);

        for( Map.Entry<String, Integer> entry : sortedImpRelations )
        {
            result += "\t" + entry.getKey() + "\t" + entry.getValue();
        }

        return result;
    }

    /**
     * 与另一条词条进行合并，最终返回一个新的词条。
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
     * 使用另一条item来更新自己的数据
     * @param other
     */
    public void updateWithItem(DSAWordDictItem other)
    {
        if( !this.content.equalsIgnoreCase(other.getContent()) ) return;

        //因为已经在addNewExpRel里面完成了下述动作，因此此处不再需要手动更改
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
