package com.example;

public class Sample {

    public String hello(String str, long num1, double num2) {
        Long num3 = 0L;
        num3 += num1;
        String result = "hello " + str;
        return result;
    }
    
    public String ttt(String[] names) {
        for(int i = 0; i < names.length; ++i) {
            
        }
        
        return "";
    }

    public int test2(int a) {
        try {
            if (a < 0) {
                throw new IllegalArgumentException("a is less than 0");
            }
            return a;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return 0;
        }
    }
}