package com.yuchting.yuchdroid.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Yuchdroid16Activity extends Activity {
	
	public final static String TAG = "Yuchdroid16Activity";
		
	EditText	m_host	= null;
	EditText	m_port	= null;
	EditText	m_userPass = null;
	
	TextView	m_connectStateView = null;
	
	YuchDroidApp m_mainApp;
	
	ConfigInit m_config;	
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        m_mainApp = (YuchDroidApp)getApplicationContext();
        m_config = m_mainApp.m_config;
        
        initLoginLayout();
    }  
    
    private void initLoginLayout() { 
        
        Button buttonStart = (Button) findViewById(R.id.login_start_svr);
        buttonStart.setOnClickListener(new OnClickListener() {  
            public void onClick(View arg0){

            	if(m_mainApp.m_connectDeamonRun){
            		stopConnectDeamon();
                }else{

                	String t_host = m_host.getText().toString();
                	String t_port = m_port.getText().toString();
                	String t_pass = m_userPass.getText().toString();                	
                	
                	if(validateHost(t_host,t_port,t_pass)){
                		
                		boolean t_readConfig = false;
                		
                    	if(!t_host.equals(m_mainApp.m_config.m_host) 
                    	|| !t_port.equals(m_mainApp.m_config.m_port) 
                    	|| !t_pass.equals(m_mainApp.m_config.m_userPass)){
                    		t_readConfig = true;
                    		
                    		m_config.m_host = t_host;
                    		m_config.m_port = Integer.valueOf(t_port).intValue();
                    		m_config.m_userPass = t_pass;
                    		
                    		m_config.WriteReadIni(false);
                    	}
                    	
                		startConnectDeamon(t_readConfig);
                	}
                }
            }
        });
        
        m_connectStateView = (TextView)findViewById(R.id.login_connect_text);
        
        m_host = (EditText)findViewById(R.id.login_host);
        m_port = (EditText)findViewById(R.id.login_port);
        m_userPass = (EditText)findViewById(R.id.login_user_pass);
        
        m_host.setText(m_config.m_host);
        m_port.setText(m_config.m_port == 0?"":Integer.toString(m_config.m_port));
        m_userPass.setText(m_config.m_userPass);        
        
        setConnectState(m_mainApp.m_connectState);
        
  
//        Button buttonStop = (Button) findViewById(R.id.stop_svr);  
//        buttonStop.setOnClickListener(new OnClickListener() {  
//            public void onClick(View arg0) {
//            	// add a test mail
//            	//
//            	Random t_rand = new Random();
//            	fetchMail t_newMail = new fetchMail();
//            	t_newMail.SetContain("This is a test mail + "+t_rand.nextInt());
//            	t_newMail.SetSubject("Subject " + t_rand.nextInt());
//            	t_newMail.SetFromVect(new String[]{"From@gmail.com"});
//            	t_newMail.SetSendToVect(new String[]{"SendTo@gmail.com"});
//            	t_newMail.SetSendDate(new Date());
//            	
//            	MailDbAdapter t_ad = new MailDbAdapter(Yuchdroid16Activity.this);
//            	t_ad.open();
//            	t_ad.createMail(t_newMail,null);
//            	
//            	Toast.makeText(getApplicationContext(), t_newMail.GetSubject() + " add OK!",
//            	          Toast.LENGTH_SHORT).show();
//            }  
//        });  
   
    }
    
    private boolean validateHost(String _host,String _port,String _userPass){
    	
    	if(!_host.matches("([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%%&=]*)?") 
    	&& !_host.matches("/(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)/g")){
    		Toast.makeText(this, R.string.login_host_error_prompt, Toast.LENGTH_SHORT).show();
    		return false;
    	}
    	
    	if(_port.length() == 0 || Integer.valueOf(_port).intValue() > 65535){
    		Toast.makeText(this, R.string.login_port_error_prompt, Toast.LENGTH_SHORT).show();
    		return false;
    	}
    	
    	if(_userPass.length() == 0){
    		Toast.makeText(this, R.string.login_user_pass_error_prompt, Toast.LENGTH_SHORT).show();
    		return false;
    	}
    	
    	return true;
    }
    
    private void startConnectDeamon(boolean _readConfig){
    	Intent intent = new Intent(this,ConnectDeamon.class);    	
        startService(intent);
    }  
  
    private void stopConnectDeamon(){  
    	Intent intent = new Intent(this,ConnectDeamon.class);
        stopService(intent);
        
        // set the text
        setConnectState(YuchDroidApp.STATE_DISCONNECT);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        Log.i(TAG,"onStart " + this);
    }
    
    BroadcastReceiver	m_intentRecv = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			setConnectState(m_mainApp.m_connectState);
		}
	};
	
	private synchronized void setConnectState(int _state){
		switch(_state){
		case YuchDroidApp.STATE_CONNECTED:
			m_connectStateView.setText(getString(R.string.login_state_connected));
			break;
		case YuchDroidApp.STATE_CONNECTING:
			m_connectStateView.setText(getString(R.string.login_state_connecting));
			break;
		case YuchDroidApp.STATE_DISCONNECT:
			m_connectStateView.setText(getString(R.string.login_state_disconnect));
			break;
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_menu,menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.login_menu_debug_info:
            	startActivity(new Intent(this,DebugInfoActivity.class));
                return true;
            case R.id.login_menu_home:
            	startActivity(new Intent(this,HomeActivity.class));
            	return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(m_intentRecv, new IntentFilter(YuchDroidApp.FILTER_CONNECT_STATE));
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
        Log.i(TAG,"onPause " + this);
        
        unregisterReceiver(m_intentRecv);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
        
        Log.i(TAG,"onStop " + this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
        
        Log.i(TAG,"onDestroy " + this);
    }
}