/*
 * Copyright (c) 2014, University Of Massachusetts Lowell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Massachusetts Lowell nor the names
 * from of its contributors may be used to endorse or promote products
 * derived this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * Author: Jordan Allspaw <jallspaw@cs.uml.edu>
*/

package edu.uml.tango.tango_root.peanut_stream;

import android.content.res.Resources;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import java.nio.ByteBuffer;
import android.util.Log;

import edu.uml.TangoAPI;

public class PeanutStream extends RosFragmentActivity implements RateWatcher.RateUpdater{
    private PositionPublisher posePub;
    private DepthPublisher depthPub;
    private static final String TAG = "TangoJNIActivity";
    public PeanutStream() {
        super("peanut_stream", "peanut_stream");
    }

    RateWatcher mRateWatcher = new RateWatcher(this);
    private Handler mHandler = new Handler();
    TangoAPI mTangoAPI;

    // generic specific textbox event handling
    private void smartSet(View v, String str)
    {
        switch (v.getId()) {
            case R.id.positionEditText:
                posePub.setParentId(str);
                savePreferences("e1_text",str);
                break;
            case R.id.positionFrameEditText:
                posePub.setFrameId(str);
                savePreferences("e2_text",str);
                break;
            case R.id.depthEditText:
                depthPub.setTopicName(str);
                savePreferences("e3_text",str);
                break;
            case R.id.depthFrameEditText:
                depthPub.setFrameId(str);
                savePreferences("e4_text",str);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(posePub == null) {
            posePub = new PositionPublisher();
            posePub.setParentId(sharedPreferences.getString("e1_text",getResources().getString(R.string.parent_id)));
            posePub.setFrameId(sharedPreferences.getString("e2_text",getResources().getString(R.string.odom_frame_id)));
            posePub.setCameraId(sharedPreferences.getString("e4_text",getResources().getString(R.string.depth_frame_id)));
            posePub.setOkPublish(sharedPreferences.getBoolean("tb1_checked",false));
            posePub.setRateWatcher(mRateWatcher.add(R.id.odom_rate));
        }
        if(depthPub == null) {
            depthPub = new DepthPublisher(sharedPreferences.getString("e3_text",getResources().getString(R.string.depth_topic)),
                    sharedPreferences.getString("e4_text",getResources().getString(R.string.depth_frame_id)));
            depthPub.setRateWatcher(mRateWatcher.add(R.id.depth_rate));
            depthPub.setOkPublish(sharedPreferences.getBoolean("tb2_checked",false));
        }
        mTangoAPI = new TangoAPI(posePub, depthPub);
        mTangoAPI.start();

        EditText e1 = (EditText) findViewById(R.id.positionEditText);
        EditText e2 = (EditText) findViewById(R.id.positionFrameEditText);
        EditText e3 = (EditText) findViewById(R.id.depthEditText);
        EditText e4 = (EditText) findViewById(R.id.depthFrameEditText);
        e1.setText(sharedPreferences.getString("e1_text", getResources().getString(R.string.parent_id)));
        e2.setText(sharedPreferences.getString("e2_text", getResources().getString(R.string.odom_frame_id)));
        e3.setText(sharedPreferences.getString("e3_text", getResources().getString(R.string.depth_topic)));
        e4.setText(sharedPreferences.getString("e4_text", getResources().getString(R.string.depth_frame_id)));
        final EditText[] ets = new EditText[]{e1,e2,e3,e4};

        for(EditText et : ets) {
            et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b)
                        smartSet(view,((EditText) view).getText().toString());
                }
            });
        }

        ToggleButton tb1 = (ToggleButton) findViewById(R.id.positionToggleButton);
        ToggleButton tb2 = (ToggleButton) findViewById(R.id.depthToggleButton);

        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                posePub.setOkPublish(isChecked);
                savePreferences("tb1_checked",isChecked);
            }
        });

        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                depthPub.setOkPublish(isChecked);
                savePreferences("tb2_checked",isChecked);
            }
        });
        tb1.setChecked(sharedPreferences.getBoolean("tb1_checked",true));
        tb2.setChecked(sharedPreferences.getBoolean("tb2_checked",true));

        Button b = (Button) findViewById(R.id.gotoButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posePub.publishCurrent();
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        mTangoAPI.die();
        super.onDestroy();
        Log.e("peanut","WHOA WHOA, WE'RE GOING DOWN!");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),getMasterUri());
        nodeMainExecutor.execute(posePub, nodeConfiguration);
        nodeMainExecutor.execute(depthPub, nodeConfiguration);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    private void savePreferences(String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Override
    public void update(final int id) {
        final TextView tv = (TextView)findViewById(id);
        final Resources res = getResources();
        mHandler.post(new Runnable() {
            @Override
            public void run()
            {
                tv.setText(String.format("%.3f %s",mRateWatcher.getRate(id),res.getString(R.string.frequency_suffix)));
            }
        });
    }
}