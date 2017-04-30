package com.example.mayank.freeloc;

import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by mayank on 4/20/17.
 */

public class fingerprint implements Serializable{
    public String location = new String();
    public ArrayList<ArrayList<Pair<String, Integer>>> fprint = new ArrayList<ArrayList<Pair<String, Integer>>>();
    public ArrayList<ArrayList<String>> sonly = new ArrayList<ArrayList<String>>();
}
