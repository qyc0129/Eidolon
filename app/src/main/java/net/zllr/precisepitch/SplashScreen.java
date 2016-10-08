package net.zllr.precisepitch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreen extends Activity {

	private static int SPLASH_TIME_OUT = 3000;
	ImageView logo_holder;
	TextView text_holder;
	Animation anim_logo;
	Animation anim_txt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		logo_holder = (ImageView) findViewById(R.id.imgLogo);
		
		text_holder = (TextView) findViewById(R.id.txtLogo);
		
	    logo_holder.setVisibility(View.INVISIBLE);
	    text_holder.setVisibility(View.INVISIBLE);
	    
	    anim_logo = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_info_popup);
	    anim_txt = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_info_popup);
	    
		new Handler().postDelayed(new Runnable() {
			@Override
            public void run() {
                Intent i = new Intent(SplashScreen.this,TunerActivity.class );
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
	}
	
	protected void onStart(){
		super.onStart();
		
	}


	protected void onPause() {
	    super.onPause();
	    logo_holder.setVisibility(View.INVISIBLE);
	    text_holder.setVisibility(View.INVISIBLE);
	}
	
	protected void onResume() {
	    super.onResume();
	    logo_holder.startAnimation(anim_logo);
	    
	    Handler handler = new Handler();
		Runnable startMain = new Runnable() {

			@Override
			public void run() {
				if (!isFinishing()){
					text_holder.startAnimation(anim_txt);
				}
			}
		};
		
		handler.postDelayed(startMain, 200);
	    
	}
}
