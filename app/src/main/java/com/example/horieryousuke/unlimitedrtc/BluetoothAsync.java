package com.example.horieryousuke.unlimitedrtc;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.os.AsyncTask;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothAsync {
    private static final String TAG = "BluetoothTask";

    /* Bluetooth UUID */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice = null;
    private BluetoothSocket bluetoothSocket;
    private InputStream btIn;
    private OutputStream btOut;
    private final String DEVICE_NAME = "RNBT-C8A0";
    private MainActivity mainActivity;

    public BluetoothAsync(MainActivity activity){
        mainActivity = activity;
//
    }

    /**
     * Bluetoothの初期化。
     */
    public void init() {
        // BTアダプタ取得。取れなければBT未実装デバイス。
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            System.out.println("This device is not implement Bluetooth.");
            return;
        }
        // BTが設定で有効になっているかチェック。
        if (!bluetoothAdapter.isEnabled()) {
            // TODO: ユーザに許可を求める処理。
            System.out.println("This device is disabled Bluetooth.");
            return;
        }
        System.out.println("init is called");

    }


    /**
     * 非同期で指定されたデバイスの接続を開始する。
     * - 選択ダイアログから選択されたデバイスを設定される。
     */
    public void doConnect() {
        System.out.println("doConnect is called");
        Set< BluetoothDevice > devices = bluetoothAdapter.getBondedDevices();
        for ( BluetoothDevice device : devices){

            if(device.getName().equals(DEVICE_NAME)){
                bluetoothDevice = device;
            }
        }


        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            new ConnectTask().execute();
            System.out.println("doConnect result try"+ bluetoothDevice);
        } catch (IOException e) {
            Log.e(TAG,e.toString(),e);
            System.out.println("doConnect result catch"+e.toString());
        }
    }

    /**
     * 非同期でBluetoothの接続を閉じる。
     */
    public void doClose() {
        new CloseTask().execute();
    }

    /**
     * 非同期でメッセージの送受信を行う。
     * @param msg 送信メッセージ.
     */
    public void doSend(String msg) {
        new SendTask().execute(msg);
    }

    /**
     * Bluetoothと接続を開始する非同期タスク。
     * - 時間がかかる場合があるのでProcessDialogを表示する。
     * - 双方向のストリームを開くところまで。
     */
    private class ConnectTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected void onPreExecute() {
            System.out.println("Connect Bluetooth Device.");
        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                bluetoothSocket.connect();
                System.out.println("bluetoothSocket check"+bluetoothSocket);
                btIn = bluetoothSocket.getInputStream();
                btOut = bluetoothSocket.getOutputStream();
            } catch (Throwable t) {
                doClose();
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                System.out.println(result.toString());
            } else {

            }
        }
    }

    /**
     * Bluetoothと接続を終了する非同期タスク。
     * - 不要かも知れないが念のため非同期にしている。
     */
    private class CloseTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            try {
                try{btOut.close();}catch(Throwable t){/*ignore*/}
                try{btIn.close();}catch(Throwable t){/*ignore*/}
                bluetoothSocket.close();
            } catch (Throwable t) {
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                System.out.println(result.toString());
            }
        }
    }

    /**
     * サーバとメッセージの送受信を行う非同期タスク。
     * - 英小文字の文字列を送ると英大文字で戻ってくる。
     * - 戻ってきた文字列を下段のTextViewに反映する。
     */
    private class SendTask extends AsyncTask<String, Void, Object> {

        @Override
        protected Object doInBackground(String... params) {
            System.out.println("SendTask is called");
            try {
                btOut.write(params[0].getBytes());
                btOut.flush();

                byte[] buff = new byte[512];
                int len = btIn.read(buff); // TODO:ループして読み込み
                String readMsg = new String(buff, 0, len);
                System.out.println("bluetooth result"+readMsg);
                mainActivity._senddata= readMsg;
                return new String(buff, 0, len);
            } catch (Throwable t) {
                System.out.println("catch is called");
                doClose();
                return t;
            }
        }

        @Override
        protected void onPostExecute(Object result) {

        }
    }
}
