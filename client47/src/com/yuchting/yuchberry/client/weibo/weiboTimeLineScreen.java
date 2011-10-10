package com.yuchting.yuchberry.client.weibo;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

import local.localResource;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.container.MainScreen;

import com.yuchting.yuchberry.client.msg_head;
import com.yuchting.yuchberry.client.recvMain;
import com.yuchting.yuchberry.client.sendReceive;
import com.yuchting.yuchberry.client.ui.ImageSets;
import com.yuchting.yuchberry.client.ui.ImageUnit;
import com.yuchting.yuchberry.client.ui.PhizSelectedScreen;
import com.yuchting.yuchberry.client.ui.SliderHeader;
import com.yuchting.yuchberry.client.ui.WeiboHeadImage;

abstract class WeiboUserMenu extends MenuItem{
	
	String m_userName = "@";
	int	m_strRes;
	
	public WeiboUserMenu(int _strRes,int _ordinal ,int _priority){
		super("",_ordinal,_priority);
		
		m_strRes = _strRes;
	}
	
	public void setUserName(String _userName){
		m_userName = _userName;
		setText(recvMain.sm_local.getString(m_strRes) + " " + m_userName);
	}
}

public class weiboTimeLineScreen extends MainScreen{
	
	public WeiboUserMenu m_userGetInfoMenu = new WeiboUserMenu(localResource.WEIBO_CHECK_USER_MENU_LABEL,5,5){
		public void run(){
			checkUserInfo(m_userName);
		}
	};
	
	public WeiboUserMenu m_userFollowMenu = new WeiboUserMenu(localResource.WEIBO_FOLLOW_USER_MENU_LABEL,5,6){
		public void run(){
			followUser(m_userName);
		}
	};
	
	public WeiboUserMenu m_userAtUserMenu = new WeiboUserMenu(localResource.WEIBO_AT_USER_MENU_LABEL,5,7){
		public void run(){
			m_updateItem.run();
			m_currUpdateDlg.m_updateManager.m_editTextArea.setText("@" + this.m_userName + " ");
		}
	};
	
	public WeiboUserMenu m_userSendMessageMenu = new WeiboUserMenu(localResource.WEIBO_SEND_MESSAGE_USER_MENU_LABEL,5,8){
		public void run(){
			
		}
	};
	public static recvMain				sm_mainApp = (recvMain)UiApplication.getUiApplication();
	
	public ImageSets					m_weiboUIImage = null;
			
	private WeiboUserFindFactory		m_userfactory = new WeiboUserFindFactory(this);
	
	WeiboMainManager		m_mainMgr;
	WeiboMainManager		m_mainAtMeMgr;
	WeiboMainManager		m_mainCommitMeMgr;
	WeiboDMManager			m_mainDMMgr;
	WeiboUserInfoMgr 		m_userInfoScreen = null;
	
	
	WeiboMainManager		m_currMgr = null;
		
	Vector					m_sendDaemonList = new Vector();
		
	private static ImageUnit[]	sm_VIPSign = 
	{
		null,
		null,
		null,
		
		null,
		null,
		null,
	};
	
	private static String[]		sm_VIPSignString = 
	{
		"sinaVIP",
		"sinaVIP",
		"qqVIP",
		
		"sinaVIP",
		"sinaVIP",
		"sinaVIP",
	};
	
	
	
	static ImageUnit[]		sm_WeiboSign =
	{ 
		null,null,null,null,null,	null,
	};
	
	static String[]		sm_weiboSignFilename = 
	{
		"sinaWeibo",
		"tWeibo",
		"qqWeibo",
		
		"163Weibo",
		"sohuWeibo",
		"fanWeibo",
	};
	
	
	
	
	public Vector		m_headImageList = new Vector();
			
	WeiboHeader 		m_weiboHeader		= null;
	boolean			m_weiboHeaderShow	= true;
		
	private Vector		m_delayWeiboAddList = new Vector();
	private int		m_delayWeiboAddRunnableID = -1;
	
	private Vector		m_delayWeiboDelList = new Vector();
	private int		m_delayWeiboDelRunnableID = -1;

	private WeiboMainManager[]	m_refMainManager = 
	{
		null,
		null,
		null,
		null,
	};
	
	recvMain			m_mainApp = null;
	int					m_autoRefreshWeiboIntervalID = -1;
	
	
	
	public weiboTimeLineScreen(recvMain _mainApp){
		super(Manager.VERTICAL_SCROLL);
		
		sm_mainApp 	= _mainApp;
		m_mainApp	= _mainApp;
		
		m_weiboUIImage	= recvMain.sm_weiboUIImage;
		m_weiboHeader = new WeiboHeader(this);
				
		m_currUpdateDlg = new WeiboUpdateDlg(weiboTimeLineScreen.this);
		
		m_mainMgr = new WeiboMainManager(_mainApp,this,true);
		add(m_mainMgr);
		
		m_mainAtMeMgr = new WeiboMainManager(_mainApp,this,false);
		m_mainCommitMeMgr = new WeiboMainManager(_mainApp,this,false);
		
		m_mainDMMgr = new WeiboDMManager(_mainApp, this, false);
		
		m_refMainManager[0] = m_mainMgr;
		m_refMainManager[1] = m_mainAtMeMgr;
		m_refMainManager[2] = m_mainCommitMeMgr;
		m_refMainManager[3] = m_mainDMMgr;	
		
		m_userInfoScreen = new WeiboUserInfoMgr(_mainApp,this);
		
		m_currMgr = m_mainMgr;
		
		setTitle(m_weiboHeader);
		
		m_currMgr.setFocus();	
	}
	
	public SliderHeader getHeader(){
		return m_weiboHeader;
	}
	
	public void enableHeader(boolean _enable){
		if(m_mainApp.m_hideHeader){
			if(_enable){
				if(!m_weiboHeaderShow){
					m_weiboHeaderShow = true;
					setTitle(m_weiboHeader);
				}				
			}else{
				if(m_weiboHeaderShow){
					m_weiboHeaderShow = false;
					setTitle((Field)null);
				}
			}
		}else{
			// show the header always
			//
			if(!m_weiboHeaderShow){
				m_weiboHeaderShow = true;
				setTitle(m_weiboHeader);
			}
		}
	}
	
	public boolean isHeaderShow(){
		if(m_mainApp.m_hideHeader){
			return m_weiboHeaderShow;
		}
		return true;
	}
	
	public void startAutoRefresh(){
		
		if(m_autoRefreshWeiboIntervalID != -1){
			m_mainApp.cancelInvokeLater(m_autoRefreshWeiboIntervalID);
			m_autoRefreshWeiboIntervalID = -1;
		}
		
		if(m_mainApp.getRefreshWeiboInterval() != 0){
			m_autoRefreshWeiboIntervalID = m_mainApp.invokeLater(new Runnable(){
				public void run(){
					if(!m_mainApp.m_connectDeamon.CanNotConnectSvr() && m_mainApp.IsPromptTime()){
						m_refreshItem.run();
					}					
				}
			}, m_mainApp.getRefreshWeiboInterval() * 60 * 1000, true);
		}
	}
		
	private final static int fsm_promptBubble_x = 30;
	private final static int fsm_promptBubble_y = 40;
	private final static int fsm_promptBubbleBorder = 5;
	private final static int fsm_promptBubbleArc = 8;
	private final static int	fsm_promptBubbleWidth = recvMain.fsm_display_width - (fsm_promptBubble_x) * 2;
	
	private final static int	fsm_promptTextWidth = fsm_promptBubbleWidth - fsm_promptBubbleBorder * 2;
	
	
	int					m_runnableTextShowID = -1;
	int					m_popupPromptTextTimer = 0;
	
	
	final class PromptTextField extends TextField {
		
		int m_width;
		
		public PromptTextField(boolean _small){
			super(Field.READONLY);
			if(_small){
				setFont(getFont().derive(getFont().getStyle(),getFont().getHeight() - 4));
				m_width = getFont().getAdvance("000/000 ");
			}else{
				m_width = fsm_promptTextWidth;
			}
		}
		public void setText(String _text){
			super.setText(_text);
			layout(m_width,1000);
		}
		
		public void paint(Graphics _g){
			super.paint(_g);
		}

	}
	
	public PromptTextField 	m_promptTextArea	= new PromptTextField(false);
	public PromptTextField m_inputTextNum		= new PromptTextField(true);

	public synchronized void popupPromptText(String _prompt){
		
		if(!m_promptTextArea.getText().equals(_prompt)){
			
			m_popupPromptTextTimer = 0;
			
			m_promptTextArea.setText(_prompt);
			
			if(m_runnableTextShowID == -1){
				
				m_runnableTextShowID = m_mainApp.invokeLater(new Runnable() {
					
					public void run() {
						
						synchronized (weiboTimeLineScreen.this) {
							if(++m_popupPromptTextTimer > 10){
								m_promptTextArea.setText("");
								m_mainApp.cancelInvokeLater(m_runnableTextShowID);
								m_runnableTextShowID = -1;
								
								invalidate();
							}
						}
						
					}
				}, 200, true);
			}
			
			invalidate();
		}
	}

	public void setInputPromptText(String _prompt){
		m_inputTextNum.setText(_prompt);
		
		if(m_weiboHeaderShow){
			m_weiboHeader.invalidate();
		}
		
	}
	
	protected void paint(Graphics g){
		super.paint(g);

		if(m_promptTextArea.getText().length() != 0){
			
			paintPromptText(g,fsm_promptBubble_x,fsm_promptBubble_y,
					fsm_promptBubbleWidth,m_promptTextArea.getHeight() + fsm_promptBubbleBorder * 2,m_promptTextArea);
		}
		
		
		if(m_inputTextNum.getText().length() != 0){
			
			paintPromptText(g,recvMain.fsm_display_width - m_inputTextNum.getWidth() / 2 - fsm_promptBubbleBorder * 2 - 30,
					0,
					m_inputTextNum.getWidth() + fsm_promptBubbleBorder * 2 ,
					m_inputTextNum.getHeight() + fsm_promptBubbleBorder * 2,m_inputTextNum);
		}
	}
	
	public static void paintPromptText(Graphics g,int _x,int _y,int _width,int _height,PromptTextField _text){
		
		int t_color = g.getColor();
		try{
			g.setColor(WeiboItemField.fsm_promptTextBGColor);
        	g.fillRoundRect(_x, _y, _width, _height, fsm_promptBubbleArc, fsm_promptBubbleArc);
        	
        	g.setColor(WeiboItemField.fsm_promptTextBorderColor);
        	g.drawRoundRect(_x, _y, _width, _height, fsm_promptBubbleArc, fsm_promptBubbleArc);
			
		}finally{
			g.setColor(t_color);
		}
		
		boolean notEmpty = g.pushContext( _x + fsm_promptBubbleBorder , _y + fsm_promptBubbleBorder ,
										_width , _height, _x + fsm_promptBubbleBorder, _y + fsm_promptBubbleBorder);
        try {
            if( notEmpty ) {
            	_text.paint(g);
            }
        } finally {
            g.popContext();
            g.setColor(t_color);
        }
	}
	
	public void ClearWeibo(){
		m_mainMgr.deleteAll();
		m_mainMgr.add(m_mainMgr.m_updateWeiboFieldNull);
		
		m_mainAtMeMgr.deleteAll();
		m_mainCommitMeMgr.deleteAll();
		
		invalidate();
	}
		
	private void AddWeibo_imple(fetchWeibo _weibo,boolean _initAdd){
		
		try{
						
			WeiboHeadImage t_headImage = WeiboHeadImage.SearchHeadImage(m_headImageList,
					_weibo.GetHeadImageId(),_weibo.GetWeiboStyle(),_weibo.GetUserHeadImageHashCode(),true);
			
			switch(_weibo.GetWeiboClass()){
			case fetchWeibo.TIMELINE_CLASS:
				m_mainMgr.AddWeibo(_weibo,t_headImage,_initAdd);
				break;
			case fetchWeibo.COMMENT_ME_CLASS:
				m_mainCommitMeMgr.AddWeibo(_weibo,t_headImage,_initAdd);
				break;
			case fetchWeibo.AT_ME_CLASS:
				m_mainAtMeMgr.AddWeibo(_weibo,t_headImage,_initAdd);
				break;
			case fetchWeibo.DIRECT_MESSAGE_CLASS:
				m_mainDMMgr.AddWeibo(_weibo,t_headImage,_initAdd);
				break;
			}
			
			if(!_initAdd && !_weibo.IsOwnWeibo()){
				
				m_weiboHeader.invalidate();				
				
				if(_weibo.GetWeiboClass() != fetchWeibo.TIMELINE_CLASS){
					m_mainApp.TriggerWeiboNotification();
				}
			}
			
		}catch(Exception e){
			m_mainApp.SetErrorString("AW_i:"+e.getMessage());
		}
	}
	
	public void autoLoadTimelineWeibo(){
		if(m_mainApp.m_autoLoadNewTimelineWeibo){
			m_mainMgr.clickUpdateField();
		}
	}
	
	public boolean isAddingWeibo(){
		return m_delayWeiboAddRunnableID != -1;
	}
	
	final class DelayAddWeiboData{
		public fetchWeibo	m_weibo;
		public boolean		m_initAdd;
		public DelayAddWeiboData(fetchWeibo _weibo,boolean _initAdd){
			m_weibo = _weibo;
			m_initAdd = _initAdd;
		}
	}
	
	public void AddWeibo(fetchWeibo _weibo,boolean _initAdd){
		
		synchronized (m_delayWeiboAddList) {
			
			m_delayWeiboAddList.addElement(new DelayAddWeiboData(_weibo,_initAdd));
			
			if(m_delayWeiboAddRunnableID == -1){
				
				m_delayWeiboAddRunnableID = m_mainApp.invokeLater(new Runnable() {
					
					public void run() {
						
						synchronized (m_delayWeiboAddList){
							
							if(!m_delayWeiboAddList.isEmpty()){
								
								DelayAddWeiboData t_data = (DelayAddWeiboData)m_delayWeiboAddList.elementAt(0);
								if(t_data.m_initAdd && m_mainApp.m_connectDeamon.CanNotConnectSvr()){
									// initAdd is system starting run, it must wait data service is available
									//
									return;
								}
								
								AddWeibo_imple(t_data.m_weibo,t_data.m_initAdd);
								m_delayWeiboAddList.removeElementAt(0);	
							}								
							
							if(m_delayWeiboAddList.isEmpty()){
								m_mainApp.cancelInvokeLater(m_delayWeiboAddRunnableID);
								m_delayWeiboAddRunnableID = -1;
							}
						}
					}
					
				},recvMain.fsm_delayLoadingTime, true);
			}
		}
	}
	
	public void DelWeibo(fetchWeibo _weibo){
		
		synchronized (m_delayWeiboAddList) {
			// first check in the delay add list
			//
			for(int i = 0 ;i < m_delayWeiboAddList.size();i++){
				DelayAddWeiboData t_weiboData = (DelayAddWeiboData)m_delayWeiboAddList.elementAt(i);
				if(_weibo == t_weiboData.m_weibo){
					m_delayWeiboAddList.removeElementAt(i);
					
					return;
				}
			}
		}
		
		synchronized(m_delayWeiboDelList){
			
			m_delayWeiboDelList.addElement(_weibo);
			
			if(m_delayWeiboDelRunnableID == -1){
				
				m_delayWeiboDelRunnableID = m_mainApp.invokeLater(new Runnable() {
					
					public void run() {
						synchronized (m_delayWeiboDelList) {
														
							if(!m_delayWeiboDelList.isEmpty()){
								fetchWeibo t_delWeibo = (fetchWeibo)m_delayWeiboDelList.elementAt(0);
															
								if(m_currMgr.getCurrExtendedItem() != null 
								&& m_currMgr.getCurrExtendedItem().hasTheWeibo(t_delWeibo)){
									// if the user is reading this extend item
									//
									return ;
								}
								
								m_delayWeiboDelList.removeElementAt(0);
								
								for(int i = 0;i < m_refMainManager.length;i++){
									if(m_refMainManager[i].DelWeibo(t_delWeibo)){
										break;
									}
								}
								
								WeiboHeadImage.DelWeiboHeadImage(m_headImageList,t_delWeibo.GetWeiboStyle(),Long.toString(t_delWeibo.GetId()));
							}
							
							if(m_delayWeiboDelList.isEmpty()){
								m_mainApp.cancelInvokeLater(m_delayWeiboDelRunnableID);
								m_delayWeiboDelRunnableID = -1;
							}
							
						}
					}
					
				}, 200, true);
			}
		}
	}
	
	
	
	public void weiboSendFileConfirm(int _hashCode,int _index){
		
		for(int i = 0 ;i < m_sendDaemonList.size();i++){
			
			WeiboSendDaemon t_daemon = (WeiboSendDaemon)m_sendDaemonList.elementAt(i);
			
			if(t_daemon.m_hashCode == _hashCode){
				if(t_daemon.m_sendFileDaemon != null){
					t_daemon.m_sendFileDaemon.sendNextFile(_index);
				}
				
				t_daemon.inter();
				
				m_sendDaemonList.removeElementAt(i);
				
				break;
			}
		}
	}
	public void UpdateNewWeibo(String _weiboText,byte[] _fileBuffer,int _fileType){
		
		try{
			m_sendDaemonList.addElement(new WeiboSendDaemon(_weiboText,_fileBuffer,_fileType,m_mainApp));

			m_mainApp.m_sentWeiboNum++;
			m_currMgr.EscapeKey();
			
		}catch(Exception e){
			m_mainApp.SetErrorString("UNW:"+e.getMessage()+e.getClass().getName());
		}
		
	}
	
	public void SendMenuItemClick(){
		
		if(m_currMgr == m_mainDMMgr && m_currMgr.getCurrExtendedItem() != null){
			
			// send direct message
			//
			String t_text = m_currMgr.m_editTextArea.getText();
			WeiboDMItemField t_field = (WeiboDMItemField)m_currMgr.getCurrExtendedItem();
			fetchWeibo t_weibo = t_field.getReplyWeibo();
			
			if(t_weibo != null){
				try{
					m_sendDaemonList.addElement(new WeiboSendDaemon(t_text,t_weibo.GetWeiboStyle(),
														t_weibo.GetUserScreenName(),m_mainApp));
					m_mainApp.m_sentWeiboNum++;
					m_currMgr.EscapeKey();
				}catch(Exception e){
					m_mainApp.SetErrorString("SMIC:"+e.getMessage()+e.getClass().getName());
				}				
			}
			
		}else{
			
			// send forward/reply message
			//
			try{
				
				String t_text = m_currMgr.m_editTextArea.getText();
				if(t_text.length() > WeiboItemField.fsm_maxWeiboTextLength){
					if(Dialog.ask(Dialog.D_YES_NO,recvMain.sm_local.getString(localResource.WEIBO_SEND_TEXT_CUTTED_PROMPT),Dialog.NO) != Dialog.YES){
						return;
					}
					
					t_text = t_text.substring(0,WeiboItemField.fsm_maxWeiboTextLength);
				}
				
				m_currMgr.BackupSendWeiboText(m_currMgr.m_currentSendType,m_currMgr.getCurrEditItem().m_weibo,t_text);
				
				if(m_currMgr.getCurrEditItem().m_weibo.GetWeiboClass() == fetchWeibo.COMMENT_ME_CLASS){
					
					if(m_currMgr.getCurrEditItem().m_weibo.GetCommentWeibo() != null){
						
						long t_orgId = 0;
						
						if(m_currMgr.getCurrEditItem().m_weibo.GetReplyWeiboId() != -1){
							t_orgId = m_currMgr.getCurrEditItem().m_weibo.GetReplyWeiboId();
						}else{
							t_orgId = m_currMgr.getCurrEditItem().m_weibo.GetCommentWeiboId();
						}
						
						long t_commentId = m_currMgr.getCurrEditItem().m_weibo.GetId();
						
						sendCommentReply(t_text,m_currMgr.getCurrEditItem().m_weibo.GetWeiboStyle(),
										m_currMgr.m_currentSendType,t_orgId,t_commentId);
						
					}else{
						UpdateNewWeibo(t_text,null,0);
						return ;
					}
				}else{
					long t_orgId = m_currMgr.getCurrEditItem().m_weibo.GetId();
					
					sendCommentReply(t_text,m_currMgr.getCurrEditItem().m_weibo.GetWeiboStyle(),
									m_currMgr.m_currentSendType,t_orgId,0);
				}			
				
				
			}catch(Exception e){
				m_mainApp.SetErrorString("SMIC1:" + e.getMessage() + e.getClass().getName());
			}
		}
	}
	
	private void sendCommentReply(String _text,int _weiboStyle,int _sendType,long _orgId,long _commentId)throws Exception{
					
		m_sendDaemonList.addElement(new WeiboSendDaemon(_text,(byte)_weiboStyle,(byte)_sendType,_orgId,_commentId,m_mainApp));
		
		m_mainApp.m_sentWeiboNum++;
		m_currMgr.EscapeKey();
	}
	
	int m_menuIndex = 0;
	
	MenuItem m_homeManagerItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_HOME_MANAGER_MENU_LABEL),m_menuIndex++,0){
        public void run() {
        	m_weiboHeader.setCurrState(WeiboHeader.STATE_TIMELINE);
    		refreshWeiboHeader();
        }
    }; 
    
    MenuItem m_atMeManagerItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_AT_ME_MANAGER_MENU_LABEL),m_menuIndex++,0){
        public void run() {
        	m_weiboHeader.setCurrState(WeiboHeader.STATE_AT_ME);
    		refreshWeiboHeader();
        }
    }; 
    
    MenuItem m_commentMeItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_COMMENT_ME_MANAGER_MENU_LABEL),m_menuIndex++,0){
        public void run() {
        	m_weiboHeader.setCurrState(WeiboHeader.STATE_COMMENT_ME);
    		refreshWeiboHeader();
        }
    }; 
    
    MenuItem m_directMsgItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_DM_MANAGER_MENU_LABEL),m_menuIndex++,0){
        public void run() {
        	m_weiboHeader.setCurrState(WeiboHeader.STATE_DIRECT_MESSAGE);
    		refreshWeiboHeader();
        }
    };
    
    MenuItem m_userInfoItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_USER_INFO_MENU_LABEL),m_menuIndex++,0){
        public void run() {
        	m_weiboHeader.setCurrState(WeiboHeader.STATE_WEIBO_USER);
    		refreshWeiboHeader();
        }
    };
    
   
    int m_menuIndex_op = 20;
    
    public WeiboUpdateDlg m_currUpdateDlg = null;
    public boolean m_pushUpdateDlg = false;
    public MenuItem m_updateItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_UPDATE_DLG),m_menuIndex_op++,0){
        public void run() {
        	if(m_currUpdateDlg == null){
        		m_currUpdateDlg = new WeiboUpdateDlg(weiboTimeLineScreen.this);
        	}
        	m_pushUpdateDlg = true;
        	m_currUpdateDlg.clearAttachment();
        	UiApplication.getUiApplication().pushScreen(m_currUpdateDlg);
        }
    };
    
	MenuItem m_sendItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_SEND_LABEL),m_menuIndex_op++,0){
        public void run() {
        	SendMenuItemClick();
        }
    };
    
    MenuItem m_phizItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_PHIZ_LABEL),m_menuIndex_op++,0){
        public void run() {
        	UiApplication.getUiApplication().pushScreen(PhizSelectedScreen.getPhizScreen(m_mainApp, m_currMgr.m_editTextArea));
        }
    };
    
    MenuItem m_refreshItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_REFRESH_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	SendRefreshMsg();
        }
    };    
     
    
    MenuItem m_topItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_RETURN_TOP_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	m_currMgr.ScrollToTop();
        }
    };
    
    MenuItem m_bottomItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_BACK_BOTTOM_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	m_currMgr.ScrollToBottom();
        }
    };
    
    MenuItem m_preWeiboItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_PRE_ITEM_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	m_currMgr.OpenNextWeiboItem(false);
        }
    };
    
    MenuItem m_nextWeiboItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_NEXT_WEIBO_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	m_currMgr.OpenNextWeiboItem(true);
        }
    };
    
	
    MenuItem m_forwardWeiboItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_FORWARD_WEIBO_BUTTON_LABEL),m_menuIndex_op++,0){
		public void run(){
			m_currMgr.ForwardWeibo(m_currMgr.getCurrSelectedItem());
		}
	};
        
	MenuItem m_atWeiboItem	= new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_AT_WEIBO_BUTTON_LABEL),m_menuIndex_op++,0){
		public void run(){
			m_currMgr.AtWeibo(m_currMgr.getCurrSelectedItem());
		}
	};
	
	MenuItem m_favWeiboItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_FAVORITE_WEIBO_BUTTON_LABEL),m_menuIndex_op++,0){
    	public void run(){
    		m_currMgr.FavoriteWeibo(m_currMgr.getCurrSelectedItem());
    	}
    };
    
	MenuItem m_picWeiboItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_CHECK_PICTURE_LABEL),m_menuIndex_op++,0){
		public void run(){
			m_currMgr.OpenOriginalPic(m_currMgr.getCurrSelectedItem());
		}
	};   
    
    MenuItem m_deleteItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_DELETE_MENU_LABEL),m_menuIndex_op++,0){
        public void run() {
        	deleteWeiboItem();
        }
    };
    
    public WeiboOptionScreen m_optionScreen = null;
    MenuItem m_optionItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_OPTION_MENU_LABEL),98,0){
    	public void run() {
        	if(m_optionScreen == null){
        		m_optionScreen = new WeiboOptionScreen(m_mainApp);
        	}
        	m_mainApp.pushScreen(m_optionScreen);
        }
    };
    MenuItem m_helpItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_HELP_MENU_LABEL),99,0){
        public void run() {
        	recvMain.openURL("http://code.google.com/p/yuchberry/wiki/YuchBerry_Weibo");
        }
    }; 
    
    MenuItem m_stateItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_STATE_SCREEN_MENU_LABEL),100,0){
        public void run() {
        	
        	recvMain t_recv = (recvMain)UiApplication.getUiApplication();
        	
        	t_recv.popScreen(weiboTimeLineScreen.this);
        	t_recv.pushStateScreen();
        }
    };
    
    MenuItem m_imScreenItem = new MenuItem(recvMain.sm_local.getString(localResource.WEIBO_IM_SCREEN_MENU_LABEL),101,0){
        public void run() {
        	
        	if(m_mainApp.m_enableIMModule){
        		
        		recvMain t_recv = (recvMain)UiApplication.getUiApplication();
            	t_recv.popScreen(weiboTimeLineScreen.this);
            	
        		m_mainApp.PopupIMScreen();
        	}
        }
    };
	
	protected void makeMenu(Menu _menu,int instance){
		
		if(m_currMgr.getCurrEditItem() == null){
			_menu.add(m_homeManagerItem);
			_menu.add(m_atMeManagerItem);
			_menu.add(m_commentMeItem);
			_menu.add(m_directMsgItem);
			_menu.add(m_userInfoItem);
			
			_menu.add(MenuItem.separator(m_menuIndex));
		}
		
		if(m_currMgr.getCurrEditItem() != null && m_currMgr.getCurrExtendedItem() != null){
			_menu.add(m_sendItem);
			_menu.add(m_phizItem);
		}		
		
		_menu.add(m_refreshItem);
		
		if(m_currMgr != m_mainDMMgr){
			_menu.add(m_topItem);
			_menu.add(m_bottomItem);
		}	
		
		if(m_currMgr.getCurrExtendedItem() != null && m_currMgr != m_mainDMMgr){
			if(m_currMgr.getCurrEditItem() == null){
				_menu.add(m_preWeiboItem);
				_menu.add(m_nextWeiboItem);
			}
		}
		
		if(m_currMgr.getCurrEditItem() == null && !m_pushUpdateDlg){
			_menu.add(m_updateItem);
		}
		
		if(m_currMgr.getCurrSelectedItem() != null && m_currMgr != m_mainDMMgr){
			
			if(m_currMgr.getCurrEditItem() == null){
				_menu.add(m_forwardWeiboItem);
				_menu.add(m_atWeiboItem);
				
				_menu.setDefault(m_forwardWeiboItem);
			}
			
			_menu.add(m_favWeiboItem);

			if(m_currMgr.getCurrSelectedItem().m_weiboPic != null){
				_menu.add(m_picWeiboItem);
			}
		}		
		
		if(m_currMgr.getCurrSelectedItem() != null				// has selected
		&& m_currMgr.getCurrSelectedItem().m_weibo != null		// is not update weibo
		&& m_currMgr.getCurrSelectedItem().m_weibo.IsOwnWeibo()
		&& m_currMgr != m_mainDMMgr){ // is own weibo
			_menu.add(m_deleteItem);
		}
		
		_menu.add(MenuItem.separator(50));
		_menu.add(m_optionItem);
		_menu.add(m_helpItem);
		_menu.add(m_stateItem);
		if(m_mainApp.m_enableIMModule){
			_menu.add(m_imScreenItem);
		}
		
		super.makeMenu(_menu,instance);
    }

    public void deleteWeiboItem(){
    	
    	if(m_currMgr.getCurrSelectedItem() != null 
    	&& m_currMgr.getCurrSelectedItem().m_weibo != null
    	&& m_currMgr.getCurrSelectedItem().m_weibo.IsOwnWeibo()){
    		if(Dialog.ask(Dialog.D_YES_NO,recvMain.sm_local.getString(localResource.WEIBO_DELETE_ASK_PROMPT),Dialog.NO) == Dialog.YES){
    			try{
    				fetchWeibo t_weibo = m_currMgr.getCurrSelectedItem().m_weibo;
    				
    				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
        			t_os.write(msg_head.msgWeiboDelete);
        			t_os.write(t_weibo.GetWeiboStyle());
        			sendReceive.WriteLong(t_os,t_weibo.GetId());
        			sendReceive.WriteBoolean(t_os,t_weibo.GetWeiboClass() == fetchWeibo.COMMENT_ME_CLASS);
        			
        			m_mainApp.m_connectDeamon.addSendingData(msg_head.msgWeiboDelete,t_os.toByteArray(),true);
        			
        			m_currMgr.DelWeibo(m_currMgr.getCurrSelectedItem().m_weibo);
    			}catch(Exception e){
    				m_mainApp.SetErrorString("DWI:" + e.getMessage() + e.getClass().getName());
    			}
    		}
    	}
    }
    
    public void checkUserInfo(String _screenName){
    	
    	WeiboItemField t_field = m_currMgr.getCurrExtendedItem();
    	
    	if(t_field != null && _screenName.length() != 0){
    		
	    	try{
				
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
				t_os.write(msg_head.msgWeiboUser);
				t_os.write(t_field.m_weibo.GetWeiboStyle());
	
				sendReceive.WriteString(t_os,_screenName);
				
				weiboTimeLineScreen.sm_mainApp.m_connectDeamon.addSendingData(
									msg_head.msgWeiboUser, t_os.toByteArray(),true);
				
			}catch(Exception e){
				weiboTimeLineScreen.sm_mainApp.SetErrorString("CUI:" + e.getMessage() + e.getClass().getName());
			}
    	}
    }
    
    public void followUser(String _screenName){
		
    	WeiboItemField t_field = m_currMgr.getCurrExtendedItem();
    	
		if(t_field != null && _screenName.length() != 0){
			
			try{
				
				ByteArrayOutputStream t_os = new ByteArrayOutputStream();
				t_os.write(msg_head.msgWeiboFollowUser);
				t_os.write(t_field.m_weibo.GetWeiboStyle());

				sendReceive.WriteString(t_os,_screenName);
				
				weiboTimeLineScreen.sm_mainApp.m_connectDeamon.addSendingData(
						msg_head.msgWeiboFollowUser, t_os.toByteArray(),true);
				
			}catch(Exception e){
				weiboTimeLineScreen.sm_mainApp.SetErrorString("FCU:" + e.getMessage() + e.getClass().getName());
			}
		}
	}

    
    public void displayWeiboUser(final fetchWeiboUser _user){
    	m_mainApp.invokeLater(new Runnable() {
			public void run() {
				
				m_weiboHeader.setCurrState(WeiboHeader.STATE_WEIBO_USER);
				refreshWeiboHeader();
				
				m_userInfoScreen.setWeiboUser(_user);
			}
		});
    	
    	
    }	
	
	private void SendRefreshMsg(){
		try{
			ByteArrayOutputStream t_os = new ByteArrayOutputStream();
			t_os.write(msg_head.msgWeiboRefresh);
			m_mainApp.m_connectDeamon.addSendingData(msg_head.msgWeiboRefresh, t_os.toByteArray(),true);
		}catch(Exception e){
			m_mainApp.SetErrorString("SRM:"+e.getMessage() + e.getClass().getName());
		}
	}
	
	boolean m_shiftKeyIsDown = false;
	
	protected boolean keyDown(int keycode,int time){
		
		final int key = Keypad.key(keycode);
		
		if(m_currMgr.getCurrEditItem() == null){

			switch(key){
			case 'C':
				m_updateItem.run();
				return true;
			case 'Y':
	    		m_homeManagerItem.run();
	    		return true;
	    	case 'U':
	    		m_atMeManagerItem.run();
	    		return true;
	    	case 'I':
	    		m_commentMeItem.run();
	    		return true;
	    	case 'O':
	    		m_directMsgItem.run();
	    		return true;
	    	case 'P':
	    		m_userInfoItem.run();
	    		return true;
	    	case 'R':
	    		m_refreshItem.run();
	    		return true;
	    	case 'T':		    		
    			m_topItem.run();
    			return true;
	    	case 'B':
    			m_bottomItem.run();	
    			return true;
    			
	    	case 'F':
	    		m_forwardWeiboItem.run();
	    		return true;
	    	case 'V':
	    		m_favWeiboItem.run();
	    		return true;
	    	case 'E':
	    		m_atWeiboItem.run();
	    		return true;
	    	case 'G':
	    		m_picWeiboItem.run();
	    		return true;
	    	case 'D':
	    		m_deleteItem.run();
	    		return true;
	    	case 'M':
	    		m_imScreenItem.run();
	    		return true;
			}
			
			if(m_currMgr.getCurrExtendedItem() != null && m_currMgr != m_mainDMMgr){
				switch(key){
				case '0':
		    		m_currMgr.OpenNextWeiboItem(!m_mainApp.m_spaceDownWeiboShortcutKey);
		    		return true;
				case ' ':
		    		m_currMgr.OpenNextWeiboItem(m_mainApp.m_spaceDownWeiboShortcutKey);
		    		return true;
				}
			}else{
				
				if(m_currMgr.getCurrExtendedItem() == null){
					switch(key){
			    	case 'S':
			    		m_stateItem.run();
			    		return true;
			    	case 'R':
			    		SendRefreshMsg();
			    		return true;
			    	case 10: // enter key
			    	case ' ':
			    	case '0':
			    		m_currMgr.Clicked(0, 0);
			    		return true;   	
			    	
			    	}
				}
			}	
		}
		
		if(m_currMgr.getCurrEditItem() != null){
			if(key == 10){
				
				boolean t_shiftDown = (Keypad.status(keycode) & KeypadListener.STATUS_SHIFT) != 0;
				
	    		if(t_shiftDown){
	    			// send the contain
	    			//
	    			m_sendItem.run();
	    			return true;
	    		}			
			}else if(key == ' '){
							
				boolean t_shiftDown = (Keypad.status(keycode) & KeypadListener.STATUS_SHIFT) != 0;
				
	    		if(t_shiftDown ){
	    			
	    			m_phizItem.run();    			
	    			return true;
	    		}
			}
		}
				
		return super.keyDown(keycode,time);   	
	}
		 
	public boolean onClose(){
		
		if(!m_currMgr.EscapeKey()){
			
			if(m_mainApp.m_connectDeamon.IsConnectState()){
	    		m_mainApp.requestBackground();
	    		m_mainApp.m_isWeiboOrIMScreen = true;
	    		return false;
	    	}else{
	    		
	    		m_stateItem.run();
	    		
	    		return true;
	    	}
		}
		
		return false;
	}
		
	protected boolean navigationMovement(int dx,int dy,int status,int time){
		if(dx != 0){
			if(m_currMgr.getCurrExtendedItem() == null && m_currMgr.getCurrEditItem() == null){
				
				m_weiboHeader.setCurrState(m_weiboHeader.getCurrState() + dx);
				refreshWeiboHeader();
				
				return true;
			}
		}
		
		if(m_runnableTextShowID != -1){
			invalidate();
		}	
		
		return 	super.navigationMovement(dx, dy, status, time);
		
	}
	
	private void refreshWeiboHeader(){
		
		switch(m_weiboHeader.getCurrState()){
		case WeiboHeader.STATE_TIMELINE:
			if(m_currMgr != m_mainMgr){
				m_currMgr.backupFocusField();
				replace(m_currMgr,m_mainMgr);
				m_currMgr = m_mainMgr;
			}else{
				return;
			}
			break;
		case WeiboHeader.STATE_COMMENT_ME:
			if(m_currMgr != m_mainCommitMeMgr){
				m_currMgr.backupFocusField();
				replace(m_currMgr,m_mainCommitMeMgr);
				m_currMgr = m_mainCommitMeMgr;
			}else{
				return;
			}
			break;
			
		case WeiboHeader.STATE_AT_ME:
			if(m_currMgr != m_mainAtMeMgr){
				m_currMgr.backupFocusField();
				replace(m_currMgr,m_mainAtMeMgr);
				m_currMgr = m_mainAtMeMgr;
			}else{
				return;
			}
			break;
		case WeiboHeader.STATE_DIRECT_MESSAGE:
			if(m_currMgr != m_mainDMMgr){
				m_currMgr.backupFocusField();
				replace(m_currMgr, m_mainDMMgr);
				m_currMgr = m_mainDMMgr;
			}else{
				return;
			}
			break;
		case WeiboHeader.STATE_WEIBO_USER:
			if(m_currMgr != m_userInfoScreen){
				m_currMgr.backupFocusField();
				replace(m_currMgr,m_userInfoScreen);
				m_currMgr = m_userInfoScreen;
			}else{
				return ;
			}
			break;
		}
		
		m_currMgr.restoreFocusField();
				
		if(m_currMgr.getFieldCount() <= WeiboMainManager.fsm_maxItemInOneScreen){
			enableHeader(true);
		}
		
		m_mainApp.StopWeiboNotification();
		m_mainApp.StopWeiboHomeNotification();
		
	}
	
	protected boolean navigationClick(int status, int time){
		return m_currMgr.Clicked(status,time);		
	}
	
	static public ImageUnit GetWeiboSign(int _weiboStyle){
		if(sm_WeiboSign[_weiboStyle] == null){
			sm_WeiboSign[_weiboStyle] = recvMain.sm_weiboUIImage.getImageUnit(sm_weiboSignFilename[_weiboStyle]);
		}
		
		return sm_WeiboSign[_weiboStyle];
	}
	
	static public ImageUnit GetVIPSignBitmap(int _weiboStyle){
		
		if(sm_VIPSign[_weiboStyle] == null){
			sm_VIPSign[_weiboStyle] = recvMain.sm_weiboUIImage.getImageUnit(sm_VIPSignString[_weiboStyle]);
		}
	
		return sm_VIPSign[_weiboStyle];		
	}
		
	private static ImageUnit sm_weiboPicSignImage = null;
	private static ImageUnit sm_weiboCommentSignImage = null;
		
	static public ImageUnit getWeiboPicSignImage(){
		if(sm_weiboPicSignImage == null){
			sm_weiboPicSignImage = recvMain.sm_weiboUIImage.getImageUnit("picSign");
		}
		
		return sm_weiboPicSignImage;
	}
	static public ImageUnit getWeiboCommentSignImage(){
		if(sm_weiboCommentSignImage == null){
			sm_weiboCommentSignImage = recvMain.sm_weiboUIImage.getImageUnit("commentSign");
		}
		
		return sm_weiboCommentSignImage;
	}
	
	// 
	//

}