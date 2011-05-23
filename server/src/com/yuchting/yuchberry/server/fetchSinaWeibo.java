package com.yuchting.yuchberry.server;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.dom4j.Element;

import weibo4j.Comment;
import weibo4j.DirectMessage;
import weibo4j.Paging;
import weibo4j.RateLimitStatus;
import weibo4j.Status;
import weibo4j.User;
import weibo4j.Weibo;
import weibo4j.http.RequestToken;

public class fetchSinaWeibo extends fetchAccount{
	
	static
	{
		Weibo.CONSUMER_KEY = "1290385296";
    	Weibo.CONSUMER_SECRET = "508aa0bfd4b1d039bdf48374f5703d2b";
    	
    	System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
    	System.setProperty("weibo4j.oauth.consumerSecret", Weibo.CONSUMER_SECRET);
	};
	
	byte[] m_headImageBuffer	= new byte[1024 * 10];
	
	String	m_headImageDir		= null;
	
	Weibo	m_weibo				= new Weibo();
	
	String	m_prefix			= null;
	
	String	m_accountName 		= null;
	
	String	m_accessToken		= null;
	String	m_secretToken		= null;	
	
	final class fetchWeiboData{
		long					m_fromIndex = -1;
		Vector<fetchWeibo>		m_historyList = null;
		Vector<fetchWeibo>		m_weiboList = new Vector<fetchWeibo>();
		int						m_sum		= 1;
		int						m_counter	= 0;
		Vector<fetchWeibo>		m_WeiboComfirm	= new Vector<fetchWeibo>();
	}
	
	fetchWeiboData				m_timeline 		= new fetchWeiboData();
	fetchWeiboData				m_directMessage	= new fetchWeiboData();
	fetchWeiboData				m_atMeMessage	= new fetchWeiboData();
	fetchWeiboData				m_commentMessage= new fetchWeiboData();
	
	//! the time of weibo check folder call
	int							m_weiboDelayTimer = 0;
	
	//! check number 
	int		m_maxCheckFolderNum = 0;
	int		m_currRemainCheckFolderNum = 0;
		
	public fetchSinaWeibo(fetchMgr _mainMgr){
		super(_mainMgr);
	}
	
	/**
	 * initialize the account to sign in 
	 * 
	 * @param _elem		: the xml element for read the attribute 
	 */
	public void InitAccount(Element _elem)throws Exception{
		m_accountName			= fetchAccount.ReadStringAttr(_elem,"account") + "[SinaWeibo]";
		m_accessToken			= fetchAccount.ReadStringAttr(_elem,"accessToken");
		m_secretToken			= fetchAccount.ReadStringAttr(_elem,"secretToken");
		
		m_timeline.m_sum		= fetchAccount.ReadIntegerAttr(_elem,"timelineSum");
		m_directMessage.m_sum	= fetchAccount.ReadIntegerAttr(_elem,"directMessageSum");
		m_atMeMessage.m_sum		= fetchAccount.ReadIntegerAttr(_elem,"atMeSum");
		m_commentMessage.m_sum	= fetchAccount.ReadIntegerAttr(_elem,"commentSum");
		
		m_prefix				= m_accountName + "/";
		
		// create the account directory
		//
		File t_file = new File(m_mainMgr.GetPrefixString() + m_prefix);
		if(!t_file.exists()){
			t_file.mkdir();
		}
		
		m_headImageDir			= m_mainMgr.GetPrefixString() + m_prefix + "WeiboHeadImage/";
		t_file = new File(m_headImageDir);
		if(!t_file.exists()){
			t_file.mkdir();
		}
		
		m_headImageDir			= m_headImageDir + "Sina/";
		t_file = new File(m_headImageDir);
		if(!t_file.exists()){
			t_file.mkdir();
		}
	}
	
	/**
	 * get the account name (Email address)
	 */
	public String GetAccountName(){
		return m_accountName;
	}
	
	/**
	 * check the folder to find the news to push
	 */
	public synchronized void CheckFolder()throws Exception{
		
		try{
			
			if(m_weiboDelayTimer++ >= 2){

				m_weiboDelayTimer = 0;
				
				m_currRemainCheckFolderNum -= 4;
				if(m_currRemainCheckFolderNum > 0){

					CheckTimeline();
					CheckAtMeMessage();
					CheckCommentMeMessage();
					
					// this message called number is limited
					//
					// un-authorith
					//CheckDirectMessage();
					
				}else{
					
					ResetCheckFolderLimit();
				}
			}
			
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " CheckFolder Error:" + e.getMessage());
			
			// sleep for a while
			//
			Thread.sleep(5000);
		}
	}
	
	public String GetHeadImageDir(){
		return m_headImageDir;
	}
	
	private void CheckTimeline()throws Exception{
		List<Status> t_fetch = null;
		if(m_timeline.m_fromIndex > 1){
			t_fetch = m_weibo.getHomeTimeline(new Paging(m_timeline.m_fromIndex));
		}else{
			t_fetch = m_weibo.getHomeTimeline();
		}		 
		
		AddWeibo(t_fetch,m_timeline,fetchWeibo.TIMELINE_CLASS);
		
	}
	
	private void CheckDirectMessage()throws Exception{
		
		List<DirectMessage> t_fetch = null;
		if(m_directMessage.m_fromIndex > 1){
			t_fetch = m_weibo.getDirectMessages(new Paging(m_directMessage.m_fromIndex));
		}else{
			t_fetch = m_weibo.getDirectMessages();
		}	
		
		boolean t_insert;
		for(DirectMessage fetchOne : t_fetch){
			t_insert = true;
			for(fetchWeibo weibo : m_directMessage.m_weiboList){
				if(weibo.GetId() == fetchOne.getId()){
					t_insert = false;
					break;
				}
			}
			
			if(t_insert){
				for(fetchWeibo weibo : m_directMessage.m_WeiboComfirm){
					if(weibo.GetId() == fetchOne.getId()){
						t_insert = false;
						break;
					}
				}
			}
			
			if(t_insert){
				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_weibo, fetchOne);
				
				m_directMessage.m_weiboList.add(t_weibo);
			}
		}
		
		if(!t_fetch.isEmpty()){
			DirectMessage t_lashOne = t_fetch.get(0);
			m_directMessage.m_fromIndex = t_lashOne.getId() + 1;
		}
	}
	
	private void CheckAtMeMessage()throws Exception{
		List<Status> t_fetch = null;
		if(m_atMeMessage.m_fromIndex > 1){
			t_fetch = m_weibo.getMentions(new Paging(m_atMeMessage.m_fromIndex));
		}else{
			t_fetch = m_weibo.getMentions();
		}
		
		AddWeibo(t_fetch,m_atMeMessage,fetchWeibo.AT_ME_CLASS);
	}
	
	private void CheckCommentMeMessage()throws Exception{
		List<Comment> t_fetch = m_weibo.getCommentsToMe();
		
		if(m_commentMessage.m_historyList == null){
			m_commentMessage.m_historyList = new Vector<fetchWeibo>();
		}
		
		boolean t_insert;
		
		for(Comment fetchOne : t_fetch){
			t_insert = true;
			for(fetchWeibo weibo : m_commentMessage.m_historyList){
				if(weibo.GetId() == fetchOne.getId()){
					t_insert = false;
					break;
				}
			}
						
			if(t_insert){
				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_weibo, fetchOne);
				
				m_commentMessage.m_weiboList.add(t_weibo);
				
				if(m_commentMessage.m_historyList.size() > 512){
					m_commentMessage.m_historyList.remove(0);
				}
				
				m_commentMessage.m_historyList.add(t_weibo);
			}
		}
		
	}
	
	private void AddWeibo(List<Status> _from,fetchWeiboData _to,byte _class){
		
		boolean t_insert;
		for(Status fetchOne : _from){
			t_insert = true;
			for(fetchWeibo weibo : _to.m_weiboList){
				if(weibo.GetId() == fetchOne.getId()){
					t_insert = false;
					break;
				}
			}
			
			if(t_insert){
				for(fetchWeibo weibo : _to.m_WeiboComfirm){
					if(weibo.GetId() == fetchOne.getId()){
						t_insert = false;
						break;
					}
				}
			}
			
			if(t_insert){
				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_weibo, fetchOne,_class);
				
				_to.m_weiboList.add(t_weibo);
			}
		}
		
		if(!_from.isEmpty()){
			Status t_lashOne = _from.get(0);
			_to.m_fromIndex = t_lashOne.getId() + 1;
		}
	}
	
	
	/**
	 * get the directory prefix of this account 
	 */
	public String GetAccountPrefix(){
		return m_prefix;
	}
	
	/**
	 * reset the session for connection
	 * 
	 * @param _fullTest		: whether test the full configure( SMTP for email)
	 */
	public void ResetSession(boolean _fullTest)throws Exception{
		
		m_weibo.setToken(m_accessToken, m_secretToken);
		m_weibo.verifyCredentials();
		
		ResetCheckFolderLimit();
		
		m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + "> Prepare OK!");
	}
	
	public void ResetCheckFolderLimit()throws Exception{
		RateLimitStatus limitStatus = m_weibo.rateLimitStatus();
		m_currRemainCheckFolderNum = limitStatus.getRemainingHits();
		m_maxCheckFolderNum			= limitStatus.getHourlyLimit();
	}
	
	/**
	 * destroy the session connection
	 */
	public void DestroySession(){
		
	}
	
	/**
	 * network package process function
	 * 
	 * @return boolean		: has been processed?
	 */
	public boolean ProcessNetworkPackage(byte[] _package)throws Exception{
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
		
		boolean t_processed = false;
		
		final int t_head = in.read();
		switch(t_head){
			case msg_head.msgWeibo:
				t_processed = ProcessWeiboUpdate(in);
				break;
			case msg_head.msgWeiboConfirm:
				t_processed = ProcessWeiboConfirmed(in);
				break;
			case msg_head.msgWeiboHeadImage:
				t_processed = ProcessWeiboHeadImage(in);
				break;
			case msg_head.msgWeiboFavorite:
				t_processed = ProcessWeiboFavorite(in);
				break;
		}
		
		return t_processed;
	}
	
	private void PrepareRepushUnconfirmMsg_impl(fetchWeiboData _weiboList){
		
		boolean t_repush = true;
		
		long t_currTime = (new Date()).getTime();
		
		for(int i = 0;i < _weiboList.m_WeiboComfirm.size();i++){
			
			fetchWeibo confirmOne = _weiboList.m_WeiboComfirm.get(i);
			
			t_repush = true;
			for(fetchWeibo existOne : _weiboList.m_weiboList){
				
				if(confirmOne.GetId() == existOne.GetId()){
					t_repush = false;
				}
			}
			
			if(t_repush){
				
				final int t_maxTimes = 5;
				
				if(Math.abs(t_currTime - confirmOne.m_sendConfirmTime) >= (5 * 60 * 1000) ){
					
					if(confirmOne.m_sendConfirmCount++ < t_maxTimes){
						
						confirmOne.m_sendConfirmTime = t_currTime;
						
						_weiboList.m_weiboList.add(confirmOne);
						
						m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + 
								"> prepare Weibo<" + confirmOne.GetId() + "> send again...");
						
					}else{
						m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + 
								"> prepare Weibo<" + confirmOne.GetId() + "> sent " + t_maxTimes + " Times , give up.");
					}
					
					_weiboList.m_WeiboComfirm.remove(confirmOne);
					
					i--;
				}
				
				
			}
		}
		
		_weiboList.m_WeiboComfirm.removeAllElements();
	}
	
	private boolean ProcessWeiboHeadImage(ByteArrayInputStream in)throws Exception{
		
		if(in.read() == fetchWeibo.SINA_WEIBO_STYLE){
			
			final long t_id = sendReceive.ReadLong(in);
			
			File t_file = new File(GetHeadImageFilename(t_id));
			if(t_file.exists()){
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
				t_os.write(msg_head.msgWeiboHeadImage);
				t_os.write(fetchWeibo.SINA_WEIBO_STYLE);
				sendReceive.WriteLong(t_os,t_id);
				
				BufferedInputStream t_read = new BufferedInputStream(new FileInputStream(t_file));
				int size = 0;
		        while((size = t_read.read(m_headImageBuffer))!= -1){
		        	t_os.write(m_headImageBuffer,0,size);
		        }	        
		        t_read.close();
		        
		        m_mainMgr.SendData(t_os, false);
		        
		        m_mainMgr.m_logger.LogOut("send sina weibo head image " + t_id);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * push the message to client
	 */
	public synchronized void PushMsg(sendReceive _sendReceive)throws Exception{
		
		PrepareRepushUnconfirmMsg_impl(m_timeline);
		PrepareRepushUnconfirmMsg_impl(m_directMessage);
		PrepareRepushUnconfirmMsg_impl(m_atMeMessage);
		PrepareRepushUnconfirmMsg_impl(m_commentMessage);
		
		PushMsg_impl(m_timeline,_sendReceive);
		PushMsg_impl(m_directMessage,_sendReceive);
		PushMsg_impl(m_atMeMessage,_sendReceive);
		PushMsg_impl(m_commentMessage,_sendReceive);		
	}
	
	private void PushMsg_impl(fetchWeiboData _weiboList,sendReceive _sendReceive)throws Exception{
		
		ByteArrayOutputStream t_output = new ByteArrayOutputStream();
		
		long t_currTime = (new Date()).getTime();
		
		if(_weiboList.m_weiboList.size() + _weiboList.m_counter >= _weiboList.m_sum){
			
			StringBuffer t_debugString = _weiboList.m_weiboList.isEmpty()?null:(new StringBuffer());
			
			while(!_weiboList.m_weiboList.isEmpty()){				
				
				// send the fetchWeibo
				//
				fetchWeibo t_weibo = (fetchWeibo)_weiboList.m_weiboList.get(_weiboList.m_weiboList.size() - 1); 
				
				t_output.write(msg_head.msgWeibo);
				t_weibo.OutputWeibo(t_output);
				
				m_mainMgr.SendData(t_output,false);
							
				// add the confirm list
				//
				_weiboList.m_weiboList.remove(t_weibo);
				_weiboList.m_WeiboComfirm.add(t_weibo);
				
				t_weibo.m_sendConfirmTime = t_currTime;
				
				// debug.out...
				t_debugString.append(GetAccountName()).append(" send Weibo<").append(t_weibo.GetId())
							.append("+").append(t_weibo.GetWeiboClass()).append(":").append(t_weibo.GetText())
							.append(">,wait confirm...");
				
				if(!_weiboList.m_weiboList.isEmpty()){
					t_debugString.append("\n\t\t");
				}
							
				t_output.reset();
			}
			
			if(t_debugString != null){
				m_mainMgr.m_logger.LogOut(t_debugString.toString());
			}			
			
			_weiboList.m_counter = 0;
			
		}else{
			
			_weiboList.m_counter++;
		}
		
	}
	
	static byte[] sm_operateWeiboFailed = null;
	static{
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgNote);
		try{
			sendReceive.WriteString(t_os,"Weibo operating failed, please check server log for detail.",false);
		}catch(Exception e){}
		
		sm_operateWeiboFailed = t_os.toByteArray();
	}
	
	public boolean ProcessWeiboUpdate(ByteArrayInputStream in)throws Exception{
		
		int t_style = in.read();
		int t_type = in.read();	
		
		String t_text = sendReceive.ReadString(in);
		GPSInfo t_gpsInfo = null;
		
		try{
			
			switch(t_type){
			case 0:
				
				m_mainMgr.m_logger.LogOut(GetAccountName() + " update new weibo");
				
				if(in.read() != 0){
					
					t_gpsInfo = new GPSInfo();
					t_gpsInfo.InputData(in);
					
					if(t_gpsInfo.m_latitude != 0 && t_gpsInfo.m_longitude != 0){
						m_weibo.updateStatus(t_text,t_gpsInfo.m_latitude,t_gpsInfo.m_longitude);
					}else{
						m_weibo.updateStatus(t_text);
					}					
					
				}else{
					m_weibo.updateStatus(t_text);
				}				
				
				break;
			case 1:
			case 2:
				if(t_style == fetchWeibo.SINA_WEIBO_STYLE){
					
					long t_commentWeiboId = sendReceive.ReadLong(in);
					
					m_mainMgr.m_logger.LogOut(GetAccountName() + " comment/reply weibo " + t_commentWeiboId);

					m_weibo.updateComment(t_text,Long.toString(t_commentWeiboId),null);
					
					if(in.read() != 0){
						t_gpsInfo = new GPSInfo();
						t_gpsInfo.InputData(in);
					}
					
					boolean t_updateTimeline = (in.read() == 1);
					 
					if(t_updateTimeline){
						if(t_gpsInfo != null && t_gpsInfo.m_longitude != 0 && t_gpsInfo.m_latitude != 0){
							m_weibo.updateStatus(t_text, t_commentWeiboId, t_gpsInfo.m_latitude, t_gpsInfo.m_longitude);
						}else{
							m_weibo.updateStatus(t_text, t_commentWeiboId);
						}						
					}					
					
					return true;
				}
				
				break;
			}
			
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " Exception:" + e.getMessage());
			m_mainMgr.m_logger.PrinterException(e);
			
			m_mainMgr.SendData(sm_operateWeiboFailed, false);
		}
		
		return false;
	}
	
	
	public boolean ProcessWeiboConfirmed(ByteArrayInputStream in)throws Exception{
	
		if(in.read() == fetchWeibo.SINA_WEIBO_STYLE){

			final long t_id = sendReceive.ReadLong(in);

			if(ProcessWeiboConfirmed_imple(m_timeline,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_directMessage,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_atMeMessage,t_id)){
				return true;
			}
			if(ProcessWeiboConfirmed_imple(m_commentMessage,t_id)){
				return true;
			}
		}	
		
		return false;
	}
	
	private boolean ProcessWeiboFavorite(ByteArrayInputStream in)throws Exception{
		
		if(in.read() == fetchWeibo.SINA_WEIBO_STYLE){
			
			long t_id = sendReceive.ReadLong(in);
			m_weibo.createFavorite(t_id);			
		}
		
		return false;
	}
	
	private boolean ProcessWeiboConfirmed_imple(fetchWeiboData _weiboList,final long _id){
		boolean t_found = false;
		for(fetchWeibo confirmOne : _weiboList.m_WeiboComfirm){
			
			if(confirmOne.GetId() == _id){
				t_found = true;
				_weiboList.m_WeiboComfirm.remove(confirmOne);
				
				m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + "> confirm Weibo<" + _id + ">");
				break;
			}
		}
		
		return t_found;
	}
	
	public void ImportWeibo(fetchWeibo _weibo,Status _stat,byte _weiboClass){
		_weibo.SetId(_stat.getId());
		_weibo.SetDateLong(_stat.getCreatedAt().getTime());
		_weibo.SetText(_stat.getText());
		_weibo.SetSource(_stat.getSource());
		
		_weibo.SetWeiboStyle(fetchWeibo.SINA_WEIBO_STYLE);
		_weibo.SetWeiboClass(_weiboClass);
		
		User t_user = _stat.getUser();

		_weibo.SetUserId(t_user.getId());
		_weibo.SetUserName(t_user.getName());
		_weibo.SetSinaVIP(t_user.isVerified());
		
		if(_stat.getOriginal_pic() != null){
			_weibo.SetOriginalPic(_stat.getOriginal_pic());
		}		
		
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user));		

		try{
			
			if(_stat.getInReplyToStatusId() != -1){
				
				Status t_commentStatus = m_weibo.showStatus(_weibo.GetReplyWeiboId());
				fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				
				ImportWeibo(t_replayWeibo,t_commentStatus,fetchWeibo.TIMELINE_CLASS);
				
				_weibo.SetCommectWeiboId(_stat.getInReplyToStatusId());
				_weibo.SetCommectWeibo(t_replayWeibo);
								
			}else{
				
				if(_stat.getCommentStatus() != null){
	
					fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
					
					ImportWeibo(t_replayWeibo,_stat.getCommentStatus(),fetchWeibo.TIMELINE_CLASS);
					
					_weibo.SetCommectWeiboId(t_replayWeibo.GetId());
					_weibo.SetCommectWeibo(t_replayWeibo);
				}
			}
			
			if(_stat.getInReplyToUserId() != -1){

				Status t_replyStatus = m_weibo.showStatus(_weibo.GetReplyWeiboId());
				fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				
				ImportWeibo(t_replayWeibo,t_replyStatus,fetchWeibo.TIMELINE_CLASS);
				
				_weibo.SetReplyWeiboId(_stat.getInReplyToStatusId());
				_weibo.SetReplyWeibo(t_replayWeibo);
					
			}
		
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " Exception:" + e.getMessage());
			m_mainMgr.m_logger.PrinterException(e);
		}
	}
	
	public void ImportWeibo(fetchWeibo _weibo,DirectMessage _dm){
		_weibo.SetId(_dm.getId());
		_weibo.SetDateLong(_dm.getCreatedAt().getTime());
		_weibo.SetText(_dm.getText());
		
		
		_weibo.SetWeiboStyle(fetchWeibo.SINA_WEIBO_STYLE);
		_weibo.SetWeiboClass(fetchWeibo.DIRECT_MESSAGE_CLASS);
		
		User t_user = _dm.getSender();
		if(t_user != null){
			_weibo.SetUserId(t_user.getId());
			_weibo.SetUserName(t_user.getName());
			_weibo.SetSinaVIP(t_user.isVerified());	
		}
				
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user));
	}
	
	public void ImportWeibo(fetchWeibo _weibo,Comment _comment){
		_weibo.SetId(_comment.getId());
		_weibo.SetDateLong(_comment.getCreatedAt().getTime());
		_weibo.SetText(_comment.getText());
		_weibo.SetSource(_comment.getSource());
		
		_weibo.SetWeiboStyle(fetchWeibo.SINA_WEIBO_STYLE);
		_weibo.SetWeiboClass(fetchWeibo.COMMENT_ME_CLASS);
		
		User t_user = _comment.getUser();
		if(t_user != null){
			_weibo.SetUserId(t_user.getId());
			_weibo.SetUserName(t_user.getName());
			_weibo.SetSinaVIP(t_user.isVerified());	
		}
		
		try{
			
			if(_comment.getOriginalStatus() != null){
				
				fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_replayWeibo,_comment.getOriginalStatus(),fetchWeibo.TIMELINE_CLASS);
				
				_weibo.SetCommectWeiboId(t_replayWeibo.GetId());
				_weibo.SetCommectWeibo(t_replayWeibo);
			}
		
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " Exception:" + e.getMessage());
			m_mainMgr.m_logger.PrinterException(e);
		}
		
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user));
	}
	
	public String GetHeadImageFilename(final long _id){
		return GetHeadImageDir() + _id + ".png";
	}
	
	private int StoreHeadImage(final User _user){
		
		final String t_filename 		= GetHeadImageFilename(_user.getId());
		
		int t_hashCode = -1;
		int size = 0;
		
		try{
			
			File t_file = new File(t_filename);
			
			if(!t_file.exists()){
				
				// local file is NOT exist then download from the URL
				//
				URL t_url = _user.getProfileImageURL();
				
		        URLConnection t_connect = t_url.openConnection();
		        BufferedInputStream t_read = new   BufferedInputStream(t_connect.getInputStream()); 
		   		try{
		   		  FileOutputStream fos = new FileOutputStream(t_file);
			        try{
				        while((size = t_read.read(m_headImageBuffer))!= -1){
				        	fos.write(m_headImageBuffer,0,size);
				        }
			        }finally{
			        	fos.flush();
				        fos.close();
				        fos = null;
			        }
		   		}finally{
		   			t_read.close();
		   			t_read = null;
		   		}
		   		
		        // scale the image...
		        //
		        BufferedImage bsrc = ImageIO.read(t_file);
		        BufferedImage bdest = new BufferedImage(fetchWeibo.fsm_headImageSize,fetchWeibo.fsm_headImageSize, BufferedImage.TYPE_INT_RGB);
		        Graphics2D g = bdest.createGraphics();
		        AffineTransform at = AffineTransform.getScaleInstance((double)fetchWeibo.fsm_headImageSize/bsrc.getWidth(),
		        														(double)fetchWeibo.fsm_headImageSize/bsrc.getHeight());
		        g.drawRenderedImage(bsrc,at);
		        ImageIO.write(bdest,"PNG",t_file);
			}
			
			byte[] t_byte = new byte[(int)t_file.length()];
			
			FileInputStream t_fileIn = new FileInputStream(t_file);
			try{
				sendReceive.ForceReadByte(t_fileIn, t_byte, t_byte.length);	        
		        
		        t_hashCode = t_byte.length;

			}finally{
				t_fileIn.close();
				t_fileIn = null;
			}
			
	        
		}catch(Exception e){
			m_mainMgr.m_logger.PrinterException(e);
		}
		
		return t_hashCode;
	}
	
	public RequestToken getRequestToken()throws Exception{		
		return m_weibo.getOAuthRequestToken();
	}	
	
	static public void main(String[] _arg)throws Exception{
		fetchMgr t_manger = new fetchMgr();
		Logger t_logger = new Logger("");
		
		t_logger.EnabelSystemOut(true);
		t_manger.InitConnect("",t_logger);
		
		fetchSinaWeibo t_weibo = new fetchSinaWeibo(t_manger);		
		
		t_weibo.m_accessToken = "8a2bf4e5a97194a1eb73740b448f034e";
		t_weibo.m_secretToken = "7529265879f3c97af609c694064bbc59";
		
		t_weibo.ResetSession(true);
		User t_user = t_weibo.m_weibo.showUser("1894359415");
				
		System.out.print( t_weibo.StoreHeadImage(t_user));
	}
	
}