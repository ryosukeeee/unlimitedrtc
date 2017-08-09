package com.example.horieryousuke.unlimitedrtc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Map;

import io.skyway.Peer.*;
import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Peer            _peer;
    private MediaConnection _media;
    private DataConnection _data;

    private MediaStream _msLocal;
    private MediaStream _msRemote;

    private Handler _handler;

    private String   _id;

    private String[] _listPeerIds;
    private boolean  _bCalling;
    private Boolean  _bConnecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Window wnd = getWindow();
        wnd.addFlags(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        _handler = new Handler(Looper.getMainLooper());
        Context context = getApplicationContext();


        //////////////////////////////////////////////////////////////////////
        //////////////////  START: Initialize SkyWay Peer ////////////////////
        //////////////////////////////////////////////////////////////////////

        // Please check this page. >> https://skyway.io/ds/
        PeerOption options = new PeerOption();

        //Enter your API Key.
        options.key = "7d8b3b19-79b9-4caa-87f9-daf98dcf73c4";
        //Enter your registered Domain.
        options.domain = "localhost";


        // SKWPeer has many options. Please check the document. >> http://nttcom.github.io/skyway/docs/

        _peer = new Peer(context, options);
        setPeerCallback(_peer);

        //request permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},0);
        }else{
            startLocalStream();
        }


        _bCalling = false;
        _bConnecting = false;

        Button btnAction = (Button) findViewById(R.id.btnAction);
        btnAction.setEnabled(true);
        btnAction.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                v.setEnabled(false);

                if (!_bCalling)
                {
                    listingPeers();
                }
                else
                {
                    closing();
                }

                v.setEnabled(true);
            }
        });


        //////////////////////////////////////////////////////////////////////
        ////////////////// END: Initialize SkyWay Peer ///////////////////////
        //////////////////////////////////////////////////////////////////////
        // コードからFragmentを追加
        // Fragmentを作成
        //if (savedInstanceState == null) {
        //    BluetoothFragment fragment = new BluetoothFragment();
        //    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //     transaction.add(R.id.container, fragment);
        //    // commitで反映
        //    transaction.commit();
        //}
    }

    void startLocalStream(){
        Navigator.initialize(_peer);
        MediaConstraints constraints = new MediaConstraints();
        _msLocal = Navigator.getUserMedia(constraints);

        Canvas canvas = (Canvas) findViewById(R.id.svSecondary);
        canvas.addSrc(_msLocal, 0);
    }

    void calling(String strPeerId)
    {
        //////////////////////////////////////////////////////////////////////
        ////////////////// START: Calling SkyWay Peer   //////////////////////
        //////////////////////////////////////////////////////////////////////

        if (null == _peer)
        {
            return;
        }

        if (null != _media)
        {
            _media.close();
            _media = null;
        }

        CallOption option = new CallOption();

        _media = _peer.call(strPeerId, _msLocal, option);

        if (null != _media)
        {
            setMediaCallback(_media);

            _bCalling = true;
        }

        //////////////////////////////////////////////////////////////////////
        /////////////////// END: Calling SkyWay Peer   ///////////////////////
        //////////////////////////////////////////////////////////////////////


        updateUI();
    }

    void connecting(String strPeerId)
    {
        if (null == _peer)
        {
            return;
        }

        if (null != _data)
        {
            _data.close();
            _data = null;
        }

        //////////////////////////////////////////////////////////////////////
        ///////////////  START: Connecting Peer   ////////////////////////////
        //////////////////////////////////////////////////////////////////////

        // connect option
        ConnectOption option = new ConnectOption();
        option.metadata = "data connection";
        option.label = "chat";
        option.serialization = DataConnection.SerializationEnum.BINARY;


        // connect
        _data = _peer.connect(strPeerId, option);

        if (null != _data) {
            setDataCallback(_data);
        }
        //////////////////////////////////////////////////////////////////////
        ////////////////  END: Connecting Peer   /////////////////////////////
        //////////////////////////////////////////////////////////////////////
    }

    //////////Start:Set Peer callback////////////////
    ////////////////////////////////////////////////
    private void setPeerCallback(Peer peer)
    {
        //////////////////////////////////////////////////////////////////////////////////
        ///////////////////// START: Set SkyWay peer callback   //////////////////////////
        //////////////////////////////////////////////////////////////////////////////////

        // !!!: Event/Open
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                Log.d(TAG, "[On/Open]");

                if (object instanceof String)
                {
                    _id = (String) object;
                    Log.d(TAG, "ID:" + _id);

                    updateUI();
                }
            }
        });

        // !!!: Event/Call
        peer.on(Peer.PeerEventEnum.CALL, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                Log.d(TAG, "[On/Call]");
                if (!(object instanceof MediaConnection))
                {
                    return;
                }

                _media = (MediaConnection) object;

                _media.answer(_msLocal);

                setMediaCallback(_media);

                _bCalling = true;

                updateUI();
            }
        });

        // !!!: Event/Close
        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                Log.d(TAG, "[On/Close]");
            }
        });

        // !!!: Event/Disconnected
        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                Log.d(TAG, "[On/Disconnected]");
            }
        });

        // !!!: Event/Error
        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                PeerError error = (PeerError) object;

                Log.d(TAG, "[On/Error]" + error);

                String strMessage = "" + error;
                String strLabel = getString(android.R.string.ok);


            }
        });

        //////////////////////////////////////////////////////////////////////////////////
        /////////////////////// END: Set SkyWay peer callback   //////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
    }

    //Unset peer callback
    void unsetPeerCallback(Peer peer)
    {
        peer.on(Peer.PeerEventEnum.OPEN, null);
        peer.on(Peer.PeerEventEnum.CONNECTION, null);
        peer.on(Peer.PeerEventEnum.CALL, null);
        peer.on(Peer.PeerEventEnum.CLOSE, null);
        peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
        peer.on(Peer.PeerEventEnum.ERROR, null);
    }

    void setMediaCallback(MediaConnection media)
    {
        //////////////////////////////////////////////////////////////////////////////////
        //////////////  START: Set SkyWay peer Media connection callback   ///////////////
        //////////////////////////////////////////////////////////////////////////////////

        // !!!: MediaEvent/Stream
        media.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                _msRemote = (MediaStream) object;

                Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
                canvas.addSrc(_msRemote, 0);
            }
        });

        // !!!: MediaEvent/Close
        media.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                if (null == _msRemote)
                {
                    return;
                }

                Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
                canvas.removeSrc(_msRemote, 0);

                _msRemote = null;

                _media = null;
                _bCalling = false;

                updateUI();
            }
        });

        // !!!: MediaEvent/Error
        media.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                PeerError error = (PeerError) object;

                Log.d(TAG, "[On/MediaError]" + error);

                String strMessage = "" + error;
                String strLabel = getString(android.R.string.ok);


            }
        });

        //////////////////////////////////////////////////////////////////////////////////
        ///////////////  END: Set SkyWay peer Media connection callback   ////////////////
        //////////////////////////////////////////////////////////////////////////////////
    }

    //Unset media connection event callback.
    void unsetMediaCallback(MediaConnection media)
    {
        media.on(MediaConnection.MediaEventEnum.STREAM, null);
        media.on(MediaConnection.MediaEventEnum.CLOSE, null);
        media.on(MediaConnection.MediaEventEnum.ERROR, null);
    }


    void setDataCallback(DataConnection data)
    {
        // !!!: DataEvent/Open
        data.on(DataConnection.DataEventEnum.OPEN, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                // TODO: DataEvent/OPEN
                connected();
                System.out.println("media connection is opened ");
            }
        });

        // !!!: DataEvent/Data
        data.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                String strValue = null;

                if (object instanceof String) {
                    // TODO: Receive String object
                    strValue = (String) object;
                } else if (object instanceof Double) {
                    Double doubleValue = (Double) object;

                    strValue = doubleValue.toString();
                } else if (object instanceof ArrayList) {
                    // TODO: receive Array list object
                    ArrayList arrayValue = (ArrayList) object;

                    StringBuilder sbResult = new StringBuilder();

                    for (Object item : arrayValue) {
                        sbResult.append(item.toString());
                        sbResult.append("\n");
                    }

                    strValue = sbResult.toString();
                } else if (object instanceof Map) {
                    // TODO: receive Map object
                    Map mapValue = (Map) object;

                    StringBuilder sbResult = new StringBuilder();

                    Object[] objKeys = mapValue.keySet().toArray();
                    for (Object objKey : objKeys) {
                        Object objValue = mapValue.get(objKey);

                        sbResult.append(objKey.toString());
                        sbResult.append(" = ");
                        sbResult.append(objValue.toString());
                        sbResult.append("\n");
                    }

                    strValue = sbResult.toString();

                    updateUI();
                    strValue = "Received Image.(Type:byte[])";
                }
            }
        });

        // !!!: DataEvent/Close
        data.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                // TODO: DataEvent/CLOSE
                _data = null;
                disconnected();
            }
        });

        // !!!: DataEvent/Error
        data.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object object) {
                // TODO: DataEvent/ERROR
                PeerError error = (PeerError) object;

                Log.d(TAG, "[On/DataError]" + error);

                String strMessage = error.message;
                String strLabel = getString(android.R.string.ok);


            }
        });
    }
    //////////////////////////////////////////////////////////////////////////////////
    /////////////////  END: Set SkyWay peer Data connection callback   ///////////////
    //////////////////////////////////////////////////////////////////////////////////

    void unsetDataCallback(DataConnection data)
    {
        data.on(DataConnection.DataEventEnum.OPEN, null);
        data.on(DataConnection.DataEventEnum.DATA, null);
        data.on(DataConnection.DataEventEnum.CLOSE, null);
        data.on(DataConnection.DataEventEnum.ERROR, null);
    }


    // Listing all peers
    void listingPeers()
    {
        if ((null == _peer) || (null == _id) || (0 == _id.length()))
        {
            return;
        }

        _peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object object) {
                if (!(object instanceof JSONArray)) {
                    return;
                }

                JSONArray peers = (JSONArray) object;

                StringBuilder sbItems = new StringBuilder();
                for (int i = 0; peers.length() > i; i++) {
                    String strValue = "";
                    try {
                        strValue = peers.getString(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (0 == _id.compareToIgnoreCase(strValue)) {
                        continue;
                    }

                    if (0 < sbItems.length()) {
                        sbItems.append(",");
                    }

                    sbItems.append(strValue);
                }

                String strItems = sbItems.toString();
                _listPeerIds = strItems.split(",");

                if ((null != _listPeerIds) && (0 < _listPeerIds.length)) {
                    calling(_listPeerIds[0]);
                    connecting(_listPeerIds[0]);
                }
            }
        });

    }



    /**
     * Closing connection.
     */
    void closing()
    {
        if (false == _bCalling)
        {
            return;
        }

        _bCalling = false;

        if (null != _media)
        {
            _media.close();
        }
        if (false == _bConnecting)
        {
            return;
        }

        _bConnecting = false;

        if (null != _data)
        {
            _data.close();
        }
    }

    void connected()
    {
        _bConnecting = true;

        updateUI();
    }

    void disconnected()
    {
        _bConnecting = false;

        updateUI();
    }

    void updateUI()
    {
        _handler.post(new Runnable() {
            @Override
            public void run() {
                Button btnAction = (Button) findViewById(R.id.btnAction);
                if (null != btnAction) {
                    if (false == _bCalling) {
                        btnAction.setText("Calling");
                    } else {
                        btnAction.setText("Hang up");
                    }
                }

                TextView tvOwnId = (TextView) findViewById(R.id.tvOwnId);
                if (null != tvOwnId) {
                    if (null == _id) {
                        tvOwnId.setText("");
                    } else {
                        tvOwnId.setText(_id);
                    }
                }
            }
        });
    }

    /**
     * Destroy Peer object.
     */
    private void destroyPeer()
    {
        closing();

        if (null != _msRemote)
        {
            Canvas canvas = (Canvas) findViewById(R.id.svPrimary);
            canvas.removeSrc(_msRemote, 0);

            _msRemote.close();

            _msRemote = null;
        }

        if (null != _msLocal)
        {
            Canvas canvas = (Canvas) findViewById(R.id.svSecondary);
            canvas.removeSrc(_msLocal, 0);

            _msLocal.close();

            _msLocal = null;
        }

        if (null != _media)
        {
            if (_media.isOpen)
            {
                _media.close();
            }

            unsetMediaCallback(_media);

            _media = null;
        }

        Navigator.terminate();

        if (null != _peer)
        {
            unsetPeerCallback(_peer);

            if (false == _peer.isDisconnected)
            {
                _peer.disconnect();
            }

            if (false == _peer.isDestroyed)
            {
                _peer.destroy();
            }

            _peer = null;
        }
    }



    @Override
    protected void onStart()
    {
        super.onStart();

        // Disable Sleep and Screen Lock
        Window wnd = getWindow();
        wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Set volume control stream type to WebRTC audio.
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    protected void onPause()
    {
        // Set default volume control stream type.
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        super.onPause();
    }

    @Override
    protected void onStop()
    {
        // Enable Sleep and Screen Lock
        Window wnd = getWindow();
        wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        destroyPeer();

        _listPeerIds = null;
        _handler = null;

        super.onDestroy();
    }
}
