package com.example.horieryousuke.unlimitedrtc;

/**
 * Created by horieryousuke on 2017/07/21.
 */
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.Window;
import android.util.Log;

import io.skyway.Peer.*;
import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;



import static com.example.horieryousuke.unlimitedrtc.R.*;

public class MainFragment extends Fragment {
    //必要な変数諸々
    private static final String TAG = "fragment";
    private Peer           _peer;
    private DataConnection _data;
    private MediaConnection _media;
    private MediaStream _msLocal;
    private MediaStream _msRemote;

    private Handler _handler;

    private String   _id;
    private String[] _listPeerIds;
    private boolean  _bCalling;
    Canvas localcanvas;
    Canvas remotecanvas;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // 先ほどのレイアウトをここでViewとして作成します
        View view = inflater.inflate(layout.fragment_main, container, false);
        remotecanvas = (Canvas) view.findViewById(R.id.svPrimary);
        localcanvas = (Canvas) view.findViewById(id.svSecondary);

        return inflater.inflate(layout.fragment_main, container, false);
    }


    public void onStart() {

        super.onStart();
        Window wnd = getActivity().getWindow();
        wnd.addFlags(Window.FEATURE_NO_TITLE);

        getActivity().setContentView(layout.fragment_main);

        _handler = new Handler(Looper.getMainLooper());
        Context context = getActivity().getApplicationContext();
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},0);
            Log.d("fragment", "パーミッションを変更しました");

        }else{
            startLocalStream();
        }


        _bCalling = false;
        //ActivityのCreateの終了

    }
    void startLocalStream(){
        Navigator.initialize(_peer);
        MediaConstraints constraints = new MediaConstraints();
        _msLocal = Navigator.getUserMedia(constraints);

        localcanvas.addSrc(_msLocal, 0);

        Log.d("fragment", "startLocalStream が終わりました");

    }

    private void setPeerCallback(Peer peer){
        //Open
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {

                if (object instanceof String)
                {
                    _id = (String) object;

                    // updateUI();
                }
            }
        });

        //Call
        peer.on(Peer.PeerEventEnum.CALL, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                if (!(object instanceof MediaConnection))
                {
                    return;
                }

                _media = (MediaConnection) object;

                _media.answer(_msLocal);

                //setMediaCallback(_media);

                _bCalling = true;

                //updateUI();
            }
        });

        //Close
        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                //Log.d(TAG, "[On/Close]");
            }
        });

        //Disconnect
        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                //Log.d(TAG, "[On/Disconnected]");
            }
        });

        //Error
        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                PeerError error = (PeerError) object;

                //Log.d(TAG, "[On/Error]" + error);

                String strMessage = "" + error;
                String strLabel = getString(android.R.string.ok);

                //MessageDialogFragment dialog = new MessageDialogFragment();
                // dialog.setPositiveLabel(strLabel);
                //dialog.setMessage(strMessage);

                // dialog.show(getFragmentManager(), "error");
            }
        });
    }


    void unsetPeerCallback(Peer peer) {
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

                remotecanvas.addSrc(_msRemote, 0);
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


                remotecanvas.removeSrc(_msRemote, 0);

                _msRemote = null;

                _media = null;
                _bCalling = false;

                //updateUI();
            }
        });

        // !!!: MediaEvent/Error
        media.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback()
        {
            @Override
            public void onCallback(Object object)
            {
                PeerError error = (PeerError) object;

                //    Log.d(TAG, "[On/MediaError]" + error);

                String strMessage = "" + error;
                String strLabel = getString(android.R.string.ok);

                // MessageDialogFragment dialog = new MessageDialogFragment();
                // dialog.setPositiveLabel(strLabel);
                // dialog.setMessage(strMessage);

                // dialog.show(getFragmentManager(), "error");
            }
        });

        //////////////////////////////////////////////////////////////////////////////////
        ///////////////  END: Set SkyWay peer Media connection callback   ////////////////
        //////////////////////////////////////////////////////////////////////////////////
    }

    void unsetMediaCallback(MediaConnection media)
    {
        media.on(MediaConnection.MediaEventEnum.STREAM, null);
        media.on(MediaConnection.MediaEventEnum.CLOSE, null);
        media.on(MediaConnection.MediaEventEnum.ERROR, null);
    }




}
