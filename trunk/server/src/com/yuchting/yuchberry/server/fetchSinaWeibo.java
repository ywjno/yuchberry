package com.yuchting.yuchberry.server;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.dom4j.Element;

import weibo4j.Comment;
import weibo4j.DirectMessage;
import weibo4j.Paging;
import weibo4j.RateLimitStatus;
import weibo4j.Status;
import weibo4j.User;
import weibo4j.Weibo;
import weibo4j.http.RequestToken;

public class fetchSinaWeibo extends fetchAbsWeibo{
	
	public static final String	fsm_consumer_key = "1290385296";
	public static final String	fsm_consumer_serect = "508aa0bfd4b1d039bdf48374f5703d2b";
	
	static
	{
		Weibo.CONSUMER_KEY = fsm_consumer_key;
    	Weibo.CONSUMER_SECRET = fsm_consumer_serect;
    	
    	System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
    	System.setProperty("weibo4j.oauth.consumerSecret", Weibo.CONSUMER_SECRET);
	};
	
	Weibo	m_weibo				= new Weibo();
	
	User 	m_userself = 		null;

	public fetchSinaWeibo(fetchMgr _mainMgr){
		super(_mainMgr);
	}
	
	public void InitAccount(Element _elem)throws Exception{
		super.InitAccount(_elem);
		
		m_accountName = m_accountName + "[SinaWeibo]";
		
		m_headImageDir			= m_headImageDir + "Sina/";
		File t_file = new File(m_headImageDir);
		if(!t_file.exists()){
			t_file.mkdir();
		}
		
	}
	
	protected int GetCurrWeiboStyle(){
		return fetchWeibo.SINA_WEIBO_STYLE;
	}
	protected void CheckTimeline()throws Exception{
		List<Status> t_fetch = null;
		if(m_timeline.m_fromIndex > 1){
			t_fetch = m_weibo.getHomeTimeline(new Paging(m_timeline.m_fromIndex));
		}else{
			t_fetch = m_weibo.getHomeTimeline();
		}		 
		
		AddWeibo(t_fetch,m_timeline,fetchWeibo.TIMELINE_CLASS);
	}
	
	protected void CheckDirectMessage()throws Exception{
//				
//		List<DirectMessage> t_fetch = null;
//		if(m_directMessage.m_fromIndex > 1){
//			t_fetch = m_weibo.getDirectMessages(new Paging(m_directMessage.m_fromIndex));
//		}else{
//			t_fetch = m_weibo.getDirectMessages();
//		}	
//		
//		boolean t_insert;
//		for(DirectMessage fetchOne : t_fetch){
//			t_insert = true;
//			for(fetchWeibo weibo : m_directMessage.m_weiboList){
//				if(weibo.GetId() == fetchOne.getId()){
//					t_insert = false;
//					break;
//				}
//			}
//			
//			if(t_insert){
//				for(fetchWeibo weibo : m_directMessage.m_WeiboComfirm){
//					if(weibo.GetId() == fetchOne.getId()){
//						t_insert = false;
//						break;
//					}
//				}
//			}
//			
//			if(t_insert){
//				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
//				ImportWeibo(t_weibo, fetchOne);
//				
//				m_directMessage.m_weiboList.add(t_weibo);
//			}
//		}
//		
//		if(!t_fetch.isEmpty()){
//			DirectMessage t_lashOne = t_fetch.get(0);
//			m_directMessage.m_fromIndex = t_lashOne.getId() + 1;
//		}
	}
	
	protected void CheckAtMeMessage()throws Exception{
		List<Status> t_fetch = null;
		if(m_atMeMessage.m_fromIndex > 1){
			t_fetch = m_weibo.getMentions(new Paging(m_atMeMessage.m_fromIndex));
		}else{
			t_fetch = m_weibo.getMentions();
		}
		
		AddWeibo(t_fetch,m_atMeMessage,fetchWeibo.AT_ME_CLASS);
	}
	
	protected void CheckCommentMeMessage()throws Exception{
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
			
			if(_to.m_weiboList.size() >= _to.m_sum){
				break;
			}
			
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
	 * reset the session for connection
	 * 
	 * @param _fullTest		: whether test the full configure( SMTP for email)
	 */
	public void ResetSession(boolean _fullTest)throws Exception{
		
		m_weibo.setToken(m_accessToken, m_secretToken);		
		m_userself = m_weibo.verifyCredentials();
		
		ResetCheckFolderLimit();
		
		m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + "> Prepare OK!");
	}
	
	protected void ResetCheckFolderLimit()throws Exception{
		RateLimitStatus limitStatus = m_weibo.rateLimitStatus();
		m_currRemainCheckFolderNum = limitStatus.getRemainingHits();
		m_maxCheckFolderNum			= limitStatus.getHourlyLimit();
	}
	
	
	
	protected void UpdataStatus(String _text,GPSInfo _info)throws Exception{
		if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
			m_weibo.updateStatus(_text,_info.m_latitude,_info.m_longitude);
		}else{
			m_weibo.updateStatus(_text);
		}	
	}
	
	protected void UpdataComment(int _style,String _text,long _commentWeiboId,
									GPSInfo _info,boolean _updateTimeline)throws Exception{

		if(_style == GetCurrWeiboStyle()){
			m_weibo.updateComment(_text,Long.toString(_commentWeiboId),null);
						 
			if(_updateTimeline){
				if(_info != null && _info.m_longitude != 0 && _info.m_latitude != 0){
					m_weibo.updateStatus(_text, _commentWeiboId, _info.m_latitude, _info.m_longitude);
				}else{
					m_weibo.updateStatus(_text, _commentWeiboId);
				}						
			}
			
		}else{
			
			if(_info != null && _info.m_longitude != 0 && _info.m_latitude != 0){
				m_weibo.updateStatus(_text, _info.m_latitude, _info.m_longitude);
			}else{
				m_weibo.updateStatus(_text);
			}
		}
			
	}
	
	protected void FavoriteWeibo(long _id)throws Exception{
		m_weibo.createFavorite(_id);			
	}

	protected void FollowUser(String _id)throws Exception{
		m_weibo.createFriendship(_id);
	}
	
	protected void DeleteWeibo(long _id)throws Exception{
		m_weibo.destroyStatus(_id);
	}
	
	public void ImportWeibo(fetchWeibo _weibo,Status _stat,byte _weiboClass){
		
		_weibo.SetId(_stat.getId());
		_weibo.SetDateLong(_stat.getCreatedAt().getTime());
		_weibo.SetText(_stat.getText());
		_weibo.SetSource(_stat.getSource());
		
		_weibo.SetWeiboStyle(fetchWeibo.SINA_WEIBO_STYLE);
		_weibo.SetWeiboClass(_weiboClass);
		
		User t_user = _stat.getUser();

		_weibo.SetOwnWeibo(t_user.getId() == m_userself.getId());
		
		_weibo.SetUserId(t_user.getId());
		_weibo.SetUserName(t_user.getName());
		_weibo.SetUserScreenName(t_user.getScreenName());
		_weibo.SetSinaVIP(t_user.isVerified());
				
		if(_stat.getOriginal_pic() != null){
			_weibo.SetOriginalPic(_stat.getOriginal_pic());
		}		
		
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user.getProfileImageURL(),Long.toString(t_user.getId())));		

		try{
			
			if(_stat.getInReplyToStatusId() != -1){
				
				Status t_commentStatus = m_weibo.showStatus(_weibo.GetReplyWeiboId());
				fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				
				ImportWeibo(t_replayWeibo,t_commentStatus,_weiboClass);
				
				_weibo.SetCommectWeiboId(_stat.getInReplyToStatusId());
				_weibo.SetCommectWeibo(t_replayWeibo);
								
			}else{
				
				if(_stat.getCommentStatus() != null){
	
					fetchWeibo t_replayWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
					
					ImportWeibo(t_replayWeibo,_stat.getCommentStatus(),_weiboClass);
					
					_weibo.SetCommectWeiboId(t_replayWeibo.GetId());
					_weibo.SetCommectWeibo(t_replayWeibo);
				}
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
		
		_weibo.SetOwnWeibo(_dm.getSenderId() == m_userself.getId());
		_weibo.SetReplyWeiboId(_dm.getRecipientId());
		
		User t_user = _dm.getSender();
		if(t_user != null){
			_weibo.SetUserId(t_user.getId());
			_weibo.SetUserName(t_user.getName());
			_weibo.SetUserScreenName(t_user.getScreenName());
			_weibo.SetSinaVIP(t_user.isVerified());	
		}
				
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user.getProfileImageURL(),Long.toString(t_user.getId())));
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
			
			_weibo.SetOwnWeibo(t_user.getId() == m_userself.getId());
			_weibo.SetUserId(t_user.getId());
			_weibo.SetUserName(t_user.getName());
			_weibo.SetUserScreenName(t_user.getScreenName());
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
		
		_weibo.SetUserHeadImageHashCode(StoreHeadImage(t_user.getProfileImageURL(),Long.toString(t_user.getId())));
	}
		
	public RequestToken getRequestToken()throws Exception{		
		return m_weibo.getOAuthRequestToken();
	}
	
	public RequestToken getRequestToken(String _callback)throws Exception{		
		return m_weibo.getOAuthRequestToken(_callback);
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
				
		System.out.print( t_weibo.StoreHeadImage(t_user.getProfileImageURL(),Long.toString(t_user.getId())));
	}
	
}