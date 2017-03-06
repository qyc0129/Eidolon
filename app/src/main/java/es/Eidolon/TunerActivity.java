/*
 * Copyright 2013 Henner Zeller <h.zeller@acm.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.Eidolon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

import es.Eidolon.model.DisplayNote;
import es.Eidolon.model.MeasuredPitch;
import es.Eidolon.model.NoteDocument;
import es.Eidolon.view.CenterOffsetView;
import es.Eidolon.view.StaffView;

public class TunerActivity extends Activity {

    HashMap musicId=new HashMap();
    private SoundPool soundPool;
    // 当前升调度数
    private int tunesAdd = 0;
    // key: 时间戳  value: 音调值
    // music play
    private Timer mTimer;
    private static final int kCentThreshold = 10;  // TODO: make configurable
    private static final boolean kShowTechInfo = false;
    private MediaPlayer mp;
    private StaffView staff;
    private TextView frequencyDisplay;
    private int averagenote=14;
    private TextView flatDisplay;
    private TextView sharpDisplay;
    private TextView decibelView;
    private TextView prevNote;
    private TextView nextNote;
    private TextView instruction;
    private CenterOffsetView offsetCentView;
    private TextView record;
    private PitchSource pitchPoster;
    private ImageView earIcon;
    private String cod;
    private enum KeyDisplay {
        DISPLAY_FLAT,
        DISPLAY_SHARP,
    }
    private KeyDisplay keyDisplay = KeyDisplay.DISPLAY_SHARP;
    private static final String noteNames[][] = {
        { "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab" ," "},
        { "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#" ," "},
    };
    private final int HANDLER_MSG_TELL_RECV = 0x124;
    private String pswd22;
    public static String PUCLIC_KEY1 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfRTdcPIH10gT9f31rQuIInLwe"
            + "\r" + "7fl2dtEJ93gTmjE9c2H+kLVENWgECiJVQ5sonQNfwToMKdO0b3Olf4pgBKeLThra" + "\r"
            + "z/L3nYJYlbqjHC3jTjUnZc0luumpXGsox62+PuSGBlfb8zJO6hix4GV/vhyQVCpG" + "\r"
            + "9aYqgE7zyTRZYX9byQIDAQAB" + "\r";
    private Handler handler1;
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);

        earIcon = (ImageView) findViewById(R.id.earIcon);
        flatDisplay = (TextView) findViewById(R.id.flatText);
        sharpDisplay = (TextView) findViewById(R.id.sharpText);
        prevNote = (TextView) findViewById(R.id.nextLower);
        nextNote = (TextView) findViewById(R.id.nextHigher);
        record=(TextView) findViewById(R.id.textView2);
        instruction = (TextView) findViewById(R.id.tunerInsruction);
        instruction.setText("");

        offsetCentView = (CenterOffsetView) findViewById(R.id.centView);
        offsetCentView.setRange(25);
        offsetCentView.setQuantization(2.5f);
        offsetCentView.setMarkAt(kCentThreshold);

        int techVisibility = kShowTechInfo ? View.VISIBLE : View.INVISIBLE;
        frequencyDisplay = (TextView) findViewById(R.id.frequencyDisplay);
        frequencyDisplay.setVisibility(techVisibility);
        decibelView = (TextView) findViewById(R.id.decibelView);
        decibelView.setVisibility(techVisibility);

        staff = (StaffView) findViewById(R.id.practiceStaff);
        staff.setNotesPerStaff(16);
        staff.setNoteModel(new NoteDocument());
        addPlaybuttonListener();
        addAccidentalListener();
        addListenerOnButton();
        addListenerOnClearButton();
        addListenerOnLoginButton();
        addListenerOnRegisterButton();
        handler1=new DoUIupdataHandler();
        handler=new UIUpdateHandler();
    }
    private void setTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                  if(averagenote!=14) {
                    Message massage = new Message();
                    massage.what = 1;
                    handler1.sendMessage(massage);
                }
                if(pitchPoster!=null)
               {
                 pitchPoster.stopSampling();
                 pitchPoster = null;
                }
                pitchPoster = new MicrophonePitchSource();
                pitchPoster.setHandler(handler);
                pitchPoster.startSampling();


            }
        },0, 250/* 表示1000毫秒之後，每隔1000毫秒執行一次 */);
    }

    private void addPlaybuttonListener()
    {
        Button playbutton;
        playbutton=(Button)findViewById(R.id.buttonplay);
        playbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                playmusic();

            }});
    }
    private void playmusic()
    {
        new AlertDialog.Builder(TunerActivity.this).setTitle(cod).show();
        musicId.put(1, soundPool.load(this, R.raw.s31, 1));
        musicId.put(2, soundPool.load(this, R.raw.s32, 1));
        musicId.put(3, soundPool.load(this, R.raw.s33, 1));
        soundPool.play(1,1,1, 0, 0, 1);

    }
    private void addListenerOnClearButton()
    {
     Button clearbutton;
        clearbutton=(Button)findViewById(R.id.clearbutton);
        clearbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
            staff.setNoteModel(new NoteDocument());
        }});
    }
    private void addListenerOnLoginButton()
    {
        Button loginbutton;
        loginbutton=(Button)findViewById(R.id.login);
        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
               dialoglogin();

            }});

    }
    private void addListenerOnRegisterButton()
    {
        Button registerbutton;
        registerbutton=(Button)findViewById(R.id.regis);
        registerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                dialogreg();

            }});

    }
    private void dialoglogin()
    {

        LayoutInflater inflater = getLayoutInflater();
        final View   dialog = inflater.inflate(R.layout.login,(ViewGroup) findViewById(R.id.dialog));
        final EditText   name = (EditText) dialog.findViewById(R.id.name);
        final EditText   pswd = (EditText) dialog.findViewById(R.id.pswd);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
        builder.setTitle("用户登录");
        builder.setView(dialog);
        mp  = MediaPlayer.create(this, R.raw.s31);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, you will be overriding this anyway
                final String name1=name.getText().toString();
                final String pswd1=pswd.getText().toString();
                startNetThread("10.206.43.162",21567,name1,pswd1);
                //mp.start();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, you will be overriding this anyway

                soundPool.play(2,1,1, 0, 0, 1);
            }
        });

        //final AlertDialog dialog = builder.create();
        // Make sure you show the dialog first before overriding the
        // OnClickListener
        //dialog.show();
        final Dialog dialogexit=builder.show();


        //addListenerOnExitButton(dialogexit);


    }
    private void dialogreg()
    {

        LayoutInflater inflater = getLayoutInflater();
        final View   dialog = inflater.inflate(R.layout.login,(ViewGroup) findViewById(R.id.dialog));
        final EditText   name = (EditText) dialog.findViewById(R.id.name);
        final EditText   pswd = (EditText) dialog.findViewById(R.id.pswd);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
        builder.setTitle("用户注册");
        builder.setView(dialog);
        mp  = MediaPlayer.create(this, R.raw.s31);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, you will be overriding this anyway
                final String name1=name.getText().toString();
                final String pswd1=pswd.getText().toString();
                Register("10.206.43.162",21567,name1,pswd1);
                //mp.start();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, you will be overriding this anyway

                soundPool.play(2,1,1, 0, 0, 1);
            }
        });

        //final AlertDialog dialog = builder.create();
        // Make sure you show the dialog first before overriding the
        // OnClickListener
        //dialog.show();
        final Dialog dialogexit=builder.show();


        //addListenerOnExitButton(dialogexit);


    }
    private void Register(final String host, final int port, final String usname,final String pswd) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(host, port);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(("RE").getBytes());
                    outputStream.flush();
                    InputStream is2 = socket.getInputStream();
                    byte[] bytes2 = new byte[1024];
                    int n2 = is2.read(bytes2);
                    String ready=new String(bytes2, 0, n2);
                    System.out.println(ready);


                    outputStream.write((usname).getBytes());
                    outputStream.flush();
                    System.out.println(socket);
                    pswd22=jm(pswd);
                    outputStream.write((pswd22).getBytes());
                    outputStream.flush();
                    System.out.println(socket);

                    InputStream is = socket.getInputStream();
                    byte[] bytes = new byte[1024];
                    int n = is.read(bytes);

                    System.out.println(new String(bytes, 0, n));

                    Message msg = handler2.obtainMessage(HANDLER_MSG_TELL_RECV, new String(bytes, 0, n));
                    msg.sendToTarget();
                    is.close();
                    socket.close();
                } catch (Exception e) {
                }
            }
        };

        thread.start();
    }

    Handler handler2 = new Handler() {
        public void handleMessage(Message msg) {
            AlertDialog.Builder builder = new AlertDialog.Builder(TunerActivity.this);
            builder.setMessage("来自服务器的数据：" + (String)msg.obj);
            builder.create().show();
        };
    };


    private String jm(final String pswd)
    {
        rsa pswdt1=new rsa();
        InputStream inPublic =   new ByteArrayInputStream(PUCLIC_KEY1.getBytes());
        PublicKey publicKey = null;
        try {
            publicKey = RSAUtils.loadPublicKey1(inPublic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 加密
        byte[] encryptByte = RSAUtils.encryptData(pswd.getBytes(), publicKey);
        // 为了方便观察吧加密后的数据用base64加密转一下，要不然看起来是乱码,所以解密是也是要用Base64先转换
        String afterencrypt = Base64Utils.encode(encryptByte);
        return afterencrypt;
    }
    private void startNetThread(final String host, final int port, final String usname,final String pswd) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(host, port);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(("HI").getBytes());
                    outputStream.flush();
                    InputStream is2 = socket.getInputStream();
                    byte[] bytes2 = new byte[1024];
                    int n2 = is2.read(bytes2);
                    String ready=new String(bytes2, 0, n2);
                    System.out.println(ready);


                    outputStream.write((usname).getBytes());
                    outputStream.flush();
                    System.out.println(socket);
                    pswd22=jm(pswd);
                    outputStream.write((pswd22).getBytes());
                    outputStream.flush();
                    System.out.println(socket);

                    InputStream is = socket.getInputStream();
                    byte[] bytes = new byte[1024];
                    int n = is.read(bytes);

                    System.out.println(new String(bytes, 0, n));
                    Message msg = handler2.obtainMessage(HANDLER_MSG_TELL_RECV, new String(bytes, 0, n));
                    msg.sendToTarget();

                    is.close();
                    socket.close();
                } catch (Exception e) {
                }
            }
        };

        thread.start();
    }
    private void addListenerOnButton() {
        ImageButton imgButton;
        imgButton = (ImageButton) findViewById
                (R.id.imageButton);

        imgButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundResource(R.drawable.ba2);
                    mTimer=new Timer();

                    setTimerTask();
                    soundPool.play(1,1,1, 0, -1, 2);
                                   }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.setBackgroundResource(R.drawable.block);
                    if(pitchPoster!=null){
                    pitchPoster.stopSampling();
                    pitchPoster = null;}
                    mTimer.cancel();
                    view.setBackgroundResource(0);
                    soundPool.pause(1);

                }
                return false;
            }
        });
    }
    private void addAccidentalListener() {
        final RadioGroup accidentalGroup = (RadioGroup) findViewById(R.id.accidentalSelection);
        accidentalGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
            	flatDisplay.setVisibility(View.INVISIBLE);
            	sharpDisplay.setVisibility(View.INVISIBLE);
                switch (checkedId) {
                    case R.id.flatRadio:
                        keyDisplay = KeyDisplay.DISPLAY_FLAT;
                        break;
                    case R.id.sharpRadio:
                        keyDisplay = KeyDisplay.DISPLAY_SHARP;
                        break;
                }
            }
        });
        ((RadioButton) findViewById(R.id.sharpRadio)).setChecked(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onResume();
    }

    // Whenever MicrophonePitchSource has a new note value available, it will
    // post it to the message queue, received here.
    private final class UIUpdateHandler extends Handler {
        private final static int kMaxWait = 32;

        // Old Android versions don't seem to have the 'setAlpha()' method.
        private void setAlphaOnText(TextView v, float alpha) {
            int alpha_bits = ((int) (0xFF * alpha)) << 24;
            v.setTextColor(v.getCurrentTextColor() & 0xFFFFFF | alpha_bits);
        }

        private void setFadeableComponentAlpha(float alpha) {

            setAlphaOnText(flatDisplay, alpha);
            setAlphaOnText(sharpDisplay, alpha);
            setAlphaOnText(frequencyDisplay, alpha);
            setAlphaOnText(prevNote, alpha);
            setAlphaOnText(nextNote, alpha);
            setAlphaOnText(instruction, alpha);
            offsetCentView.setFadeAlpha(alpha);
        }

        public void handleMessage(Message msg) {
            final MeasuredPitch data
                = (MeasuredPitch) msg.obj;


            if (data != null && data.decibel > -30)
            {
               averagenote=0;
                if(index>0) {
                    for (int i = 0; i < index; i++) {
                        averagenote += average[i];
                    }
                    averagenote = (averagenote / index)%12;
                }

                average[index++]=data.note;
                frequencyDisplay.setText(String.format(data.frequency < 200 ? "%.1fHz" : "%.0fHz",
                                                       data.frequency));
                final String printNote = noteNames[keyDisplay.ordinal()][data.note % 12];
                if(noteNames[keyDisplay.ordinal()][data.note % 12]!=null) {//if not null coding
                    cod += noteNames[keyDisplay.ordinal()][data.note % 12];
                    record.setText(cod);
                }
                final String accidental = printNote.length() > 1 ? printNote.substring(1) : "";
                flatDisplay.setVisibility("b".equals(accidental) ? View.VISIBLE : View.INVISIBLE);
                sharpDisplay.setVisibility("#".equals(accidental) ? View.VISIBLE : View.INVISIBLE);
                nextNote.setText(noteNames[keyDisplay.ordinal()][(data.note + 1) % 12]);//next note
                prevNote.setText(noteNames[keyDisplay.ordinal()][(data.note + 11) % 12]);//pre not
                //cod+= noteNames[keyDisplay.ordinal()][(data.note + 1) % 12];             //not coding
                //record.setText(cod);
                final boolean inTune = Math.abs(data.cent) <= kCentThreshold;
                final int c = inTune ? Color.rgb(50, 255, 50) : Color.rgb(255,50, 50);
                flatDisplay.setTextColor(c);
                sharpDisplay.setTextColor(c);
                if (!inTune) {
                    instruction.setText(data.cent < 0 ? "Too low" : "Too high");
                } else {
                    instruction.setText("");
                }
                setFadeableComponentAlpha(1.0f);
                offsetCentView.setValue((int) data.cent);
                fadeCountdown = kMaxWait;
            } else {
                --fadeCountdown;
                if (fadeCountdown < 0) fadeCountdown = 0;
                setFadeableComponentAlpha(1.0f * fadeCountdown / kMaxWait);
            }
            earIcon.setVisibility(data != null && data.decibel > -30? View.VISIBLE : View.INVISIBLE);
            if (data != null && data.decibel > -60) {
                decibelView.setText(String.format("%.0fdB", data.decibel));
            } else {
                decibelView.setText("");
            }
            lastPitch = data;
        }
        private int index=0;

        private int[] average=new int[100];
        private MeasuredPitch lastPitch;
        private int fadeCountdown;

    }
    private final class DoUIupdataHandler extends Handler{
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            int msgId = msg.what;

                staff.getNoteModel().add(new DisplayNote(averagenote, 4));
                staff.ensureNoteInView(0);
                staff.onModelChanged();
        averagenote=12;}




    }

}
