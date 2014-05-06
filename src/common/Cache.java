package common;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Ji JianHui
 * Time: 2014-05-05 16:16
 * Email: jhji@ir.hit.edu.cn
 */
public class Cache
{
    private HashMap<String, String> memory;

    public Cache(){
        memory = new HashMap<String, String>();
    }

    public void put(String key, String value){
        this.memory.put(key, value);
    }
    public String get(String key){
        return this.memory.get(key);
    }
}
