package com.nyzg.common;

public class Main {
    public static void main(String[] args) {
        LinearHashMap<Integer,String> linearHashMap=new LinearHashMap<>();
        linearHashMap.put(1,"1");
        linearHashMap.put(2,"2");
        linearHashMap.put(0,"0");
        linearHashMap.remove(2);
        linearHashMap.put(6,"6");
        linearHashMap.put(4,"4");
        linearHashMap.put(3,"3");
        linearHashMap.put(5,"5");
        for(int i=0;i<6;++i){
            System.out.println(linearHashMap.getByPosition(i));
        }
        linearHashMap.remove(4);
        linearHashMap.remove(0);
        linearHashMap.put(6,"hello world");
        System.out.println("--------------");
        for(int i=0;i<6;++i){
            System.out.println(linearHashMap.getByPosition(i));
        }
        linearHashMap.clear();
        System.out.println("--------------");
        for(int i=0;i<6;++i){
            System.out.println(linearHashMap.getByPosition(i));
        }
    }
}
