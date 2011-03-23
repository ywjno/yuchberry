package com.yuchting.yuchberry.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

class CheckVersion extends Thread{
	
	public fetchMgr		m_fetchMain = null;
	
	public void run(){
		while(true){
			
			try{
				URL is_gd = new URL("http://yuchberry.googlecode.com/files/latest_version");
				
		        URLConnection yc = is_gd.openConnection();
		        BufferedReader in = new BufferedReader(
		                                new InputStreamReader(yc.getInputStream()));
		        		        
				m_fetchMain.SetLatestVersion(in.readLine());
				
				in.close();
				
				
			}catch(Exception e){
				
			}
			
			try{
				sleep(24 * 3600 * 1000);
			}catch(Exception e){}
		}
	}
}
public class fetchMain{
		
	public static void main(String[] _arg){

		
		fetchMgr t_manger = new fetchMgr();
		Logger t_logger = new Logger("");
		
		CheckVersion t_check = new CheckVersion();
		t_check.m_fetchMain = t_manger;
		t_check.start();
		
		new fakeMDSSvr();
		
		while(true){

			try{
				t_manger.InitConnect("",t_logger);
				t_manger.StartListening();	
			}catch(Exception e){
				t_logger.PrinterException(e);
			}			
			
		    try{
		    	Thread.sleep(10000);
		    }catch(InterruptedException e){
		    	System.exit(0);
		    }
			
			try{
				t_manger.EndListening();
			}catch(Exception _e){
				System.exit(0);
			}
		}
	}
}