package com.example.mayank.freeloc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class my_location extends Activity {


    private Thread sendthread = null;
    private Thread recthread = null;
    public static String sendString="Unknown";
    public static String recString="Unknown";
    public static InetAddress BADDRESS = null;
    public static  int PORT=4564;

    final Handler mHandler = new Handler(){

        public void handleMessage(Message msg) {
            Bundle b;
            String value;
            if(msg.what==1){

                b=msg.getData();
                value = b.getString("data");
                //TextView txt= (TextView)findViewById(R.id.textView);
                //txt.setText(b.getString("data"));
            }
            else if (msg.what==2)
            {
                b=msg.getData();
                value = b.getString("data");
                //TextView txt= (TextView)findViewById(R.id.textView2);
                //txt.setText(b.getString("data"));

            }
            super.handleMessage(msg);
        }
    };


    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }




    Button getLocation, populateData;
    ArrayList<ArrayList<String>> allFprint = new ArrayList<ArrayList<String>>();
    ArrayList<String> myFprint = new ArrayList<String>();
    ArrayList<String> wifiData= new ArrayList<String>();
    ArrayList<fingerprint> allLandmarks = new ArrayList<fingerprint>();
    fingerprint myLandmark;

    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;

    TextView mainText, wifiText;
    ImageView imageView;
    String username, finalloc;
    ListView listView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_my_location);


        listView = (ListView) findViewById(R.id.list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, wifiData){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view =super.getView(position, convertView, parent);

                TextView textView=(TextView) view.findViewById(android.R.id.text1);

            /*YOUR CHOICE OF COLOR*/
                textView.setTextColor(Color.WHITE);

                return view;
            }
        };;


        try {
            BADDRESS=getBroadcastAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // run background services

        sendthread =new JobThread(mHandler);
        sendthread.start();

        //recthread =new JobThread2(mHandler);
        //recthread.start();





        Bundle b = getIntent().getExtras();
        if(b != null)
            username = b.getString("NAME");

        Toast.makeText(getApplicationContext(), "Welcome "+username, Toast.LENGTH_SHORT).show();


        mainText = (TextView) findViewById(R.id.heading1);
        wifiText = (TextView) findViewById(R.id.heading2);
        imageView = (ImageView) findViewById(R.id.imageView);


        final String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(root + "/netapp/");
        File[] listOfFiles = folder.listFiles();
        int total = listOfFiles.length;


        for (int i = 0; i < total; i++) {
            if (listOfFiles[i].isFile()) {
                String fname = listOfFiles[i].getName();
                String[] piece = fname.split("\\.");


                if (piece[1].equalsIgnoreCase("data")) {
                    try {
                        FileInputStream in = new FileInputStream(root + "/netapp/" + fname);
                        ObjectInputStream input = new ObjectInputStream(in);
                        ArrayList<String> obj = (ArrayList<String>) input.readObject();

                        allLandmarks.add(getFingerprint(obj));
                        //Toast.makeText(getApplicationContext(), obj.get(0), Toast.LENGTH_SHORT).show();
                        allFprint.add(obj);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }



        //getFingerprint(fprint.get(0));

        //Toast.makeText(getApplicationContext(), String.valueOf(allFprint.size()), Toast.LENGTH_SHORT).show();


        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (mainWifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "Enabling Wi-Fi",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }

        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();

        //String chk = Integer.toString(fprint.size());

        getLocation = (Button) findViewById(R.id.button1);

        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //receiverWifi = new WifiReceiver();
                //registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                /*for(int reti=0;reti<10;reti++)
                {
                    mainWifi.startScan();


                }*/

                mainWifi.setWifiEnabled(false);

                mainWifi.setWifiEnabled(true);


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // Actions to do after 10 seconds

                        Map<String, Integer> locs = new HashMap<String, Integer>();

                        for(int iter=0;iter<20;iter++)
                        {
                            mainWifi.startScan();
                            myLandmark = getFingerprint(myFprint);
                            fingerprint nearest = getNearestLandmark();
                            String name = nearest.location;
                            if(locs.containsKey(name))
                                locs.put(name, locs.get(name)+1);
                            else
                                locs.put(name, 1);
                        }

                        int scoreloc = 0;

                        for (Map.Entry<String, Integer> entry : locs.entrySet())
                        {
                            Log.i("HHHHHHHHHHHH", entry.getKey()+" "+String.valueOf(entry.getValue()));

                            if(entry.getValue() > scoreloc)
                            {
                                scoreloc = entry.getValue();
                                finalloc = entry.getKey();
                            }
                        }

                        mainText.setText("Landmark - "+finalloc);
                        wifiText.setText("Wifi Data - "+finalloc);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeFile(root+"/netapp/"+finalloc+".jpg", options);
                        imageView.setImageBitmap(bitmap);

                        Toast.makeText(getApplicationContext(), finalloc, Toast.LENGTH_SHORT).show();

                        sendString = username+":"+finalloc;



                        Log.i("XXXXXXXXXXXX", String.valueOf(wifiData.size()));


                        listView.setAdapter(adapter);

                        Toast.makeText(getApplicationContext(), "Data broadcasted", Toast.LENGTH_SHORT).show();
                    }
                }, 2000);




            }
        });

    }

    public fingerprint getNearestLandmark()
    {
        int score, maxScore = 0;
        fingerprint nloc = new fingerprint();

        for (int i=0;i<allLandmarks.size();i++)
        {
            score = 0;

            for (int j=0;j<myLandmark.sonly.size();j++)
            {
                int k;
                for (k=0;k<allLandmarks.get(i).sonly.size();k++)
                {
                    if(allLandmarks.get(i).sonly.get(k).get(0).equalsIgnoreCase(myLandmark.sonly.get(j).get(0)))
                    {
                        break;
                    }
                }

                if(k==allLandmarks.get(i).sonly.size())
                {
                    continue;
                }

                for (int m=0;m<myLandmark.sonly.get(j).size();m++)
                {
                    boolean p = allLandmarks.get(i).sonly.get(k).contains(myLandmark.sonly.get(j).get(m));

                    if(p)
                        score++;
                }
            }

            if(score > maxScore)
            {
                maxScore = score;
                nloc = allLandmarks.get(i);
            }
        }

        return nloc;
    }



    public fingerprint getFingerprint(ArrayList<String> arr) {
        fingerprint obj = new fingerprint();

        ArrayList<Pair<String, Integer>> ap = new ArrayList<Pair<String, Integer>>();

        obj.location = arr.get(0);

        for (int i = 1; i < arr.size(); i++) {
            ap.add(new Pair<String, Integer>(arr.get(i+1), Integer.parseInt(arr.get(i + 2))));
            i = i + 2;
        }

        Collections.sort(ap, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> ap1, Pair<String, Integer> ap2)
            {
                return -(ap1.second.compareTo(ap2.second));
            }
        });

        int param = 5;

        for (int i = 0; i < ap.size(); i++) {
            ArrayList<Pair<String, Integer>> temp = new ArrayList<Pair<String, Integer>>();
            ArrayList<String> pmet = new ArrayList<String>();

            temp.add(new Pair<String, Integer>(ap.get(i).first, ap.get(i).second));
            pmet.add(ap.get(i).first);

            for (int j = i+1; j < ap.size(); j++) {
                if((ap.get(j).second+param) <= ap.get(i).second) {
                    temp.add(new Pair<String, Integer>(ap.get(j).first, ap.get(j).second));
                    pmet.add(ap.get(j).first);
                }
            }

            obj.fprint.add(temp);
            obj.sonly.add(pmet);

            //Log.i("New entry", temp.get(0).first+" : "+String.valueOf(temp.get(0).second));
            //Log.i("YYYYNew entry", pmet.get(0));

            for (int j = 1; j < temp.size(); j++) {
                //Log.i("Values", temp.get(j).first+" : "+String.valueOf(temp.get(j).second));
                //Log.i("YYYYValues", pmet.get(j));
            }
        }

        ap.clear();

        for (int i = 1; i < arr.size(); i++) {
            ap.add(new Pair<String, Integer>(arr.get(i), Integer.parseInt(arr.get(i + 2))));
            i = i + 2;
        }

        Collections.sort(ap, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> ap1, Pair<String, Integer> ap2)
            {
                return -(ap1.second.compareTo(ap2.second));
            }
        });

        wifiData.clear();

        for(int iter=0;iter<ap.size();iter++)
        {
            String temp = "SSID: "+ap.get(iter).first+", Level: "+ap.get(iter).second;

            wifiData.add(temp);
        }

        return obj;
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            wifiList = mainWifi.getScanResults();

            myFprint.clear();

            myFprint.add("Unknown");

            for(int i = 0; i < wifiList.size(); i++){

                myFprint.add(wifiList.get(i).SSID.toString());
                myFprint.add(wifiList.get(i).BSSID.toString());
                myFprint.add(String.valueOf(wifiList.get(i).level));
            }
        }
    }



    class JobThread extends Thread{


        private Handler hd;

        public JobThread(Handler msgHandler){
            //constructor
            //store a reference of the message handler
            hd = msgHandler;
        }
        public void run() {
            //do some work here
            int i=0;

            while(true)
            {
                i++;
                String mess=my_location.sendString;




                StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                try {
                    //Open a random port to send the package
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    byte[] sendData = mess.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, my_location.BADDRESS, my_location.PORT);
                    socket.send(sendPacket);
                    System.out.println(getClass().getName() + "Broadcast packet sent to: " + my_location.BADDRESS.getHostAddress());
                    Log.i("log", "Broadcast packet sent to: " + my_location.BADDRESS.getHostAddress()+"\ndata: "+mess);
                } catch (IOException e) {
                    Log.e("senderr", "IOException: " + e.getMessage());
                }



                //create the bundle
                Bundle b = new Bundle(4);

                //add integer data to the bundle, everyone with a key
                b.putString("data", Integer.toString(i)+" Sent: "+mess);

                //create a message from the message handler to send it back to the main UI
                Message msg = hd.obtainMessage();

                //specify the type of message
                msg.what = 1;

                //attach the bundle to the message
                msg.setData(b);

                //send the message back to main UI thread
                hd.sendMessage(msg);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }
            }

        }

    }

}
