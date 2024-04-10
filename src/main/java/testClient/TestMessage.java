package com.webservice.hi;

import java.util.ArrayList;

public class TestMessage {
    private int result;
    public TestMessage(String testName, String expected, String received){
        System.out.println(testName);

         if(expected.equals("true")){
            expected = "Success";
        }
        if(expected.equals("false")){
            expected = "Failure";
        }
        System.out.println("Expected: "+expected);
        System.out.println("Received: "+received);
        if(expected.equals(received)){
            result=1;
        }else{
            result=0;
        }

    }

    public TestMessage(String testName, String expected, Boolean received){
        this(testName,expected,String.valueOf(received));
    }
    public TestMessage(String testName, String[] expectedSplit, String received){
        System.out.println(testName);
        String[] receivedSplit = received.split(",");
        System.out.print("Expected: ");
        for(int i=0;i< expectedSplit.length;i++){
            System.out.print(expectedSplit[i]+" ");
        }
        System.out.println();

        System.out.print("Received: ");
        for (int i=0;i<receivedSplit.length;i++){
            System.out.print(receivedSplit[i]);
        }
        System.out.println();
        boolean found=false;
        result=1;
        if(expectedSplit.length!=receivedSplit.length){
            result=0;
        }
        for(String s1 : expectedSplit){
            found=false;
            for(String s2 : receivedSplit){
                if(s1.equals(s2)){
                    found=true;
                    break;
                }
            }
            if(!found){
                result=0;
                break;
            }
        }
    }
    public int getResult(){
        return result;
    }
}
