package com.digiapt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String []args){

        List<Integer> inputs = Arrays.asList(2,4,5,7);

        analyseNumbers(inputs);
    }

    private static void analyseNumbers(List<Integer> inputs) {

        List<Integer> results = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

//        int max = null;
//        int min = null;

//        for (Integer input : inputs){
//            indexes.add(input);
//            System.out.println("Input ="+input);
//            if(!max && input>max){
//                max = input;
//                System.out.println("Max = "+max);
//            }
//            if(!min && input<min){
//                min = input;
//                System.out.println("Min = "+min);
//            }
//        }
//
//        for(int i= min+1 ; i<=max ;i++){
//            for (Integer index : indexes){
//                if (inputs.get(i)!=index){
//                    results.add(inputs.get(i));
//                }
//            }
//        }
//
//        System.out.println(results.toString());
    }
}
