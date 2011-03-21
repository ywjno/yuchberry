package com.yuchting.yuchberry.server.frame;

import java.util.Date;

import com.yuchting.yuchberry.server.Logger;
import com.yuchting.yuchberry.server.fetchMgr;

public class fetchThread extends Thread{
	
	fetchMgr 	m_fetchMgr = null;
	Logger 		m_logger = null;
	
	boolean	m_pauseState = false;
	boolean	m_close		= false;
	
	boolean	m_sendTimeupMail = false;
		
	long		m_usingHours	= 0;
	long		m_formerTimer	= 0;
	
	long		m_clientDisconnectTime	= 0;
		
	public fetchThread(fetchMgr _mainMgr,String _prefix,long _expiredTime,
					long _formerTimer,boolean _testConnect)throws Exception{
		m_usingHours = _expiredTime;
		
		m_fetchMgr = _mainMgr;
		m_logger = new Logger(_prefix);
		m_fetchMgr.InitConnect(_prefix,m_logger);
		
		if(_testConnect){
			m_fetchMgr.ResetAllAccountSession(true);
		}
		
		m_formerTimer = _formerTimer;
		
		start();
	}
	
	public void run(){
		
		while(!m_close){
			
			try{
				try{
					while(m_pauseState){
						sleep(2000);
					}	
				}catch(Exception e){}
							
				
				m_fetchMgr.StartListening();
				
			}catch(Exception e){
				m_logger.PrinterException(e);
			}
			
			if(m_close){
				break;
			}else{

				try{
					sleep(600000);
				}catch(Exception ex){}	
			}
		}
	}
	
	
	public long GetLastTime(long _currTime){
		if(m_usingHours > 0){
			return m_usingHours * 3600000 - (_currTime - m_formerTimer);
		}
		
		return 0;
	}
	
	public void Pause(){
		if(m_close == true){
			return;
		}
		
		if(m_pauseState == false){
			m_fetchMgr.EndListening();
			m_pauseState = true;
			
			try{
				sleep(100);
			}catch(Exception e){}
			
			interrupt();
		}
	}
	
	public void Reuse()throws Exception{
		
		if(m_close == true){
			return;
		}
		
		if(m_pauseState == true){
			
			m_sendTimeupMail = false;
			m_pauseState = false;
			
			try{
				
				m_fetchMgr.InitConnect(m_fetchMgr.GetPrefixString(),m_logger);
				
				sleep(100);				
				
			}catch(Exception e){
				m_logger.PrinterException(e);
				
				throw e;
				
			}finally{
								
				interrupt();
			}
			
						
		}
	}
	
	public synchronized void Destroy(){
		
		m_fetchMgr.EndListening();
		m_logger.StopLogging();
		
		try{
			sleep(100);
		}catch(Exception e){}
		
		m_close = true;
		interrupt();
	}
}
