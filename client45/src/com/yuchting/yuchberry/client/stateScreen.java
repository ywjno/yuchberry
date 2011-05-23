package com.yuchting.yuchberry.client;

import java.util.Vector;

import local.localResource;
import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.container.MainScreen;

public class stateScreen extends MainScreen implements FieldChangeListener{
										
        
    EditField           m_hostName      = null;
    EditField			m_hostport		= null;
    PasswordEditField   m_userPassword  = null;
       
    ButtonField         m_connectBut    = null;
    LabelField          m_stateText     = null;
   
    RichTextField		m_uploadingText = null;
            
    recvMain			m_mainApp		= null;
    
	MenuItem	m_helpMenu = new MenuItem(recvMain.sm_local.getString(localResource.STATE_SCREEN_HELP_MENU), 99, 10) {
		public void run() {
			recvMain.openURL("http://code.google.com/p/yuchberry/wiki/YuchBerry_Client_Help");
		}
	};
	
    
    MenuItem 	m_aboutMenu = new MenuItem(recvMain.sm_local.getString(localResource.ABOUT_MENU_TEXT), 100, 10) {
		public void run() {
			recvMain t_app = (recvMain)UiApplication.getUiApplication();
			t_app.PopupAboutScreen();
		}
	};
	
	MenuItem	m_shareMenu = new MenuItem(recvMain.sm_local.getString(localResource.SHARE_TO_FRIEND_MENU), 101, 10) {
		public void run() {
			recvMain t_app = (recvMain)UiApplication.getUiApplication();
			t_app.PopupShareScreen();			
		}
	};
	
	MenuItem 	m_setingMenu = new MenuItem(recvMain.sm_local.getString(localResource.SETTING_MENU_TEXT), 103, 10) {
		public void run() {
			recvMain t_app = (recvMain)UiApplication.getUiApplication();
			t_app.PopupSettingScreen();													
		}
	};
	
											
	MenuItem	m_debugInfoMenu = new MenuItem(recvMain.sm_local.getString(localResource.DEBUG_MENU_TEXT), 104, 10) {
		public void run() {
			recvMain t_app = (recvMain)UiApplication.getUiApplication();
			t_app.m_debugInfoScreen = new debugInfo(t_app);
			t_app.pushScreen(t_app.m_debugInfoScreen);
		}
	};
	
											
	MenuItem	m_weiboMenu = new MenuItem(recvMain.sm_local.getString(localResource.YB_WEIBO_MENU_LABEL), 105, 10) {
		public void run() {
			recvMain t_app = (recvMain)UiApplication.getUiApplication();
			if(t_app.getScreenCount() == 2){
				t_app.popStateScreen();
			}else{
				t_app.PopupWeiboScreen();
			}
		}
	};

    public stateScreen(final recvMain _app) {
    	        
        super();
              
        m_mainApp	= _app;        
        
        m_hostName = new EditField(recvMain.sm_local.getString(localResource.HOST),
        				m_mainApp.m_hostname,128, EditField.FILTER_DEFAULT);
        
       
        m_hostName.setChangeListener(this);
        add(m_hostName);
        
        m_hostport = new EditField(recvMain.sm_local.getString(localResource.PORT),
        				m_mainApp.m_port == 0?"":Integer.toString(m_mainApp.m_port),5,EditField.FILTER_INTEGER);

        m_hostport.setChangeListener(this);
        add(m_hostport);
        
        m_userPassword = new PasswordEditField(recvMain.sm_local.getString(localResource.USER_PASSWORD),
        								m_mainApp.m_userPassword,128,EditField.NO_COMPLEX_INPUT);
        
        add(m_userPassword);
        
       
        m_connectBut = new ButtonField(recvMain.sm_local.getString(m_mainApp.m_connectDeamon.IsConnectState()?
        									localResource.DISCONNECT_BUTTON_LABEL:localResource.CONNECT_BUTTON_LABEL),
        									ButtonField.CONSUME_CLICK| ButtonField.NEVER_DIRTY);
        
        m_connectBut.setChangeListener(this);
        
        add(m_connectBut);              
        
        m_stateText = new LabelField(m_mainApp.GetStateString(), LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        add(m_stateText);
        
        m_uploadingText = new RichTextField("", LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        add(m_uploadingText);
            
        RefreshUploadState(_app.m_uploadingDesc);
        
        setTitle(new LabelField(recvMain.sm_local.getString(localResource.STATE_TITLE_LABEL),LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH));
        
        if(m_mainApp.m_connectDeamon.IsConnectState()){
        	m_connectBut.setFocus();
        }else{        	
            if(m_mainApp.m_hostname.length() != 0){
            	m_connectBut.setFocus();            	
            }
        }    
               
    }
    
    protected void makeMenu(Menu _menu,int instance){
    	_menu.add(m_helpMenu);
    	_menu.add(m_aboutMenu);
    	_menu.add(m_shareMenu);
    	_menu.add(MenuItem.separator(102));
    	_menu.add(m_setingMenu);
    	_menu.add(m_debugInfoMenu);
    	
    	if(m_mainApp.m_enableWeiboModule){
    		_menu.add(m_weiboMenu);
    	}    	
    	
    	super.makeMenu(_menu, instance);
    }
    
    public final boolean onClose(){
    	
    	if(m_mainApp.m_connectDeamon.IsConnectState()){
    		if(m_mainApp.getScreenCount() == 1){
    			m_mainApp.requestBackground();
        		return false;	
    		}else{
    			m_mainApp.popStateScreen();
    			return true;
    		}    		
    	}
    	
    	m_mainApp.Exit();
    	
    	return true;
    }
    
    protected boolean keyDown(int keycode,int time){
    	if(m_connectBut.isFocus()){
    		final int key = Keypad.key(keycode);
        	switch(key){
        	case 'A':
        		m_aboutMenu.run();
        		return true;
        	case 'S':
        		m_setingMenu.run();
        		return true;
        	case 'D':
        		m_debugInfoMenu.run();
        		return true;
        	case 'H':
        		m_helpMenu.run();
        		return true;
        	case 'F':
        		m_shareMenu.run();
        		return true;
        	case 'W':
        		if(m_mainApp.m_enableWeiboModule){
        			m_weiboMenu.run();
        			return true;
        		}
        		break;
        	}
    	}
    	
    	return false;    	
    }       
   
    
    public void RefreshUploadState(final Vector _uploading){
    	String t_total = new String();
    	
    	for(int i = 0;i < _uploading.size();i++){
    		
    		recvMain.UploadingDesc t_desc = (recvMain.UploadingDesc)_uploading.elementAt(i);
    		
    		if(t_desc.m_attachmentIdx == -1){
    			
    			t_total = t_total + "(Failed) retry again " + t_desc.m_mail.GetSubject();
    			
    		}else{
    			
    			final int t_tmp = (int)((float)t_desc.m_uploadedSize / (float)t_desc.m_totalSize * 1000);
    			final float t_percent = (float)t_tmp / 10;
        		
        		t_total = t_total +"(" + t_desc.m_attachmentIdx + "/" + t_desc.m_mail.GetAttachment().size() + " " + t_percent + "%)" + t_desc.m_mail.GetSubject() ;
    		}   		
    	}
    	
    	m_uploadingText.setText(t_total);    	
    }
        
    public void fieldChanged(Field field, int context) {
        if(context != FieldChangeListener.PROGRAMMATIC){
			// Perform action if user changed field. 
			//
			if(field == m_connectBut){
				
				if(m_hostName.getText().length() == 0 
					|| m_userPassword.getText().length() == 0
					|| m_hostport.getText().length() == 0){
					
					Dialog.alert(recvMain.sm_local.getString(localResource.INPUT_FULL_SIGN_IN_SEG));
					
					return;
				}				
												
				if(m_mainApp.m_connectDeamon.IsConnectState()){
					
					try{
						m_mainApp.m_connectDeamon.Disconnect();
					}catch(Exception _e){}
					
					m_connectBut.setLabel(recvMain.sm_local.getString(localResource.CONNECT_BUTTON_LABEL));
					m_mainApp.SetStateString(recvMain.sm_local.getString(localResource.DISCONNECT_BUTTON_LABEL));
					
				}else{
					
					ServiceBook t_sb = ServiceBook.getSB();
					ServiceRecord[] t_record = t_sb.findRecordsByCid("CMIME");
					if(t_record == null || t_record.length == 0){
						m_mainApp.DialogAlert(recvMain.sm_local.getString(localResource.NEED_CMIME_PROMPT));
						return;
					}
					
					try{
						m_mainApp.m_hostname 		= m_hostName.getText();
						m_mainApp.m_port 			= Integer.valueOf(m_hostport.getText()).intValue();
						m_mainApp.m_userPassword 	= m_userPassword.getText();

						m_mainApp.m_connectDeamon.Connect();
						
						m_connectBut.setLabel(recvMain.sm_local.getString(localResource.DISCONNECT_BUTTON_LABEL));
						
						m_mainApp.Start();
						
					}catch(Exception e){
						m_mainApp.DialogAlert(e.getMessage() + e.getClass().getName());
					}
				}				
			}
        }else{
        	// Perform action if application changed field.
        }
    }
    
}