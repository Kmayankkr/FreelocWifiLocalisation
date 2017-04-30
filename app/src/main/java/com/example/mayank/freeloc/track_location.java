package com.example.mayank.freeloc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class track_location extends Activity {

    private Thread sendthread = null;
    private Thread recthread = null;;
    private Thread showthread = null;
    public static String sendString="Unknown";
    public static String recString="Unknown";
    public static InetAddress BADDRESS = null;
    public static  int PORT=4564;
    public boolean stopThread = false;


    Map<String, String> tracked = new HashMap<String, String>();
    ListView listView;
    ArrayList<String> allLocations = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    final Handler mHandler = new Handler(){

        public void handleMessage(Message msg) {
            Bundle b;
            String value, username, locat, temp[];
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
                temp = value.split(":");

                username = temp[0];
                locat = temp[1];

                //Toast.makeText(getApplicationContext(), username+locat, Toast.LENGTH_SHORT).show();

                tracked.put(username, locat);

                allLocations.clear();

                for (Map.Entry<String, String> entry : tracked.entrySet()) {
                    allLocations.add(entry.getKey() + " : " + entry.getValue());
                    //Toast.makeText(getApplicationContext(), entry.getKey() + " : " + entry.getValue(), Toast.LENGTH_SHORT).show();
                }

                if(adapter!=null)
                listView.setAdapter(adapter);
         /*       listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                        new AlertDialog.Builder(track_location.this)
                                .setTitle("Hello")
                                .setMessage("from " + listView.getItemAtPosition(position))
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                passengerInformationPopup(position);
                                            }
                                        })
                                .show();
                    }

                    public void passengerInformationPopup(int position) {



                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeFile(root+"/netapp/"+entry+".jpg", options);
                        mainText.setText("Recent - "+entry);
                        imageView.setImageBitmap(bitmap);





                        final Dialog dialog= new Dialog(getBaseContext());
                        dialog.setContentView(R.layout.passenger_details_dialog);
                        TextView title = (TextView) dialog.findViewById(R.id.textView3);
                        ImageView image= (ImageView) dialog.findViewById(R.id.dialogImage);
                        title.setText(listView.getItemAtPosition(position).toString());
                        image.setImageBitmap();
                        dialog.show();
                    }
                });*/
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





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_location);

        listView = (ListView) findViewById(R.id.tracked);


        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, allLocations);

        try {
            BADDRESS=getBroadcastAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }

        stopThread = false;

        recthread = new JobThread2(mHandler);
        recthread.start();

    }


    @Override
    protected void onStop() {
        recthread.interrupt();
        super.onStop();

    }

    @Override
    public void onBackPressed() {

        if(recthread.isAlive())
        {
            stopThread = true;
        }
        super.onBackPressed();
    }


    // receive data
    class JobThread2 extends Thread {

        private Handler hd;

        public JobThread2(Handler msgHandler) {
            //constructor
            //store a reference of the message handler
            hd = msgHandler;
        }

        public void run() {
            //do some work here
            int i = 0;
            DatagramSocket socket = null;

            try {
                //Keep a socket open to listen to all the UDP trafic that is destined for this port

                socket = new DatagramSocket(track_location.PORT, InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);
            } catch (Exception e) {
                e.printStackTrace();
                return;

            }


            while (!stopThread) {
                i++;

                String mess = "";



                try {


                    //while (true)
                    {
                        Log.i("log", "Ready to receive broadcast packets!");

                        //Receive a packet
                        byte[] recvBuf = new byte[150000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);



                        /* if (socket==null)
                        {

                            Log.i("AAAAAAAAAAAA", "BBBBBBBBBBBB");
                            try {
                                //Keep a socket open to listen to all the UDP trafic that is destined for this port

                                socket = new DatagramSocket(track_location.PORT, InetAddress.getByName("0.0.0.0"));
                                socket.setBroadcast(true);
                            } catch (Exception e) {
                                e.printStackTrace();

                            }


                        }*/
                        socket.setSoTimeout(1000);

                        try {
                            socket.receive(packet);
                            //Packet received
                            Log.i("log", "Packet received from: " + packet.getAddress().getHostAddress());
                            String data = new String(packet.getData()).trim();
                            Log.i("log", "Packet received; data: " + data);
                            mess = data;
                            Log.i("log", "mess=" + mess);


                            Log.i("log", "mess==" + mess);


                            //create the bundle
                            Bundle b = new Bundle(4);

                            //add integer data to the bundle, everyone with a key
                            //b.putString("data", Integer.toString(i) + " Received: " + mess + "|");
                            b.putString("data", mess);

                            //create a message from the message handler to send it back to the main UI
                            Message msg = hd.obtainMessage();

                            //specify the type of message
                            msg.what = 2;

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
                        catch (SocketTimeoutException e) {
                            // timeout exception.
                            Log.i("AAAAAAAAAAAA", "BBBBBBBBBBBB");
                        }





                    }
                } catch (IOException ex) {
                    Log.i("log", "Oops! " + ex.getMessage());
                }


            }
            socket.close();

            Log.i("XXXXXXXXXXXX", "YYYYYYYYYYYY");

        }
    }
}
