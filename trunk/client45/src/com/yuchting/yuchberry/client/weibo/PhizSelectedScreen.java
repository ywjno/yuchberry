package com.yuchting.yuchberry.client.weibo;

import local.localResource;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.MainScreen;

import com.yuchting.yuchberry.client.recvMain;
import com.yuchting.yuchberry.client.ui.ImageButton;
import com.yuchting.yuchberry.client.ui.ImageSets;
import com.yuchting.yuchberry.client.ui.ImageUnit;

final class Phiz extends ImageButton{
	
	public final static int		fsm_phizSize = 22;
	
	public Phiz(ImageUnit _image,ImageSets _imageSets){
        super( "",_image,_image,_imageSets,0);
    }
	
	protected void focusPaint(Graphics g,boolean focus){
		super.focusPaint(g,focus);
		
		if(focus){
			int t_color = g.getColor();
			try{
				g.setColor(0x4694ea);
				g.drawRect(0,0,m_image.getWidth(),m_image.getHeight());
			}finally{
				g.setColor(t_color);
			}
		}
	}
	
	public String getPhizName(){
		return m_image.getName();
	}
}

final class PhizMgr extends Manager{
	
	Vector 				m_phizList	= null;
	IPhizSelected		m_selectedCallback = null;
	
	final static int	fsm_phizInterval = 2;
	
	int 				m_move_x = 0;
	int 				m_move_y = 0;
	
	int					m_maxRowNum = 0;
	int					m_maxColNum = 0;
	
	public PhizMgr(Vector _phizList,IPhizSelected _selected){
		super(Manager.VERTICAL_SCROLL);
		
		
		m_phizList = _phizList;
		m_selectedCallback = _selected;
		
		for(int i = 0 ; i < m_phizList.size();i++){
			Phiz t_phiz = (Phiz)m_phizList.elementAt(i);
			add(t_phiz);
		}
	}
	
	protected void sublayout(int width,int _height){
				
		int t_x = 0;
		int t_y = 0;
		
		m_maxRowNum = recvMain.fsm_display_width / (Phiz.fsm_phizSize + fsm_phizInterval);
		m_maxColNum = m_phizList.size() / m_maxRowNum + 1;
		
		if(m_phizList.size() % m_maxRowNum == 0){
			m_maxColNum--;
		}
		
		for(int i = 0;i < m_phizList.size();i++){
			Phiz t_phiz = (Phiz)m_phizList.elementAt(i);
			
			setPositionChild(t_phiz,t_x,t_y);		
			layoutChild(t_phiz,Phiz.fsm_phizSize,Phiz.fsm_phizSize);
			
			t_x += Phiz.fsm_phizSize + fsm_phizInterval; 
			if(t_x + Phiz.fsm_phizSize + fsm_phizInterval>= recvMain.fsm_display_width){				
				t_x = 0;
				t_y += Phiz.fsm_phizSize + fsm_phizInterval;
			}
		}
		
		setExtent(recvMain.fsm_display_width, recvMain.fsm_display_height - 1 - getFont().getHeight());
	}
	
	public boolean fieldClicked(int status) {
		
		for(int i = 0 ;i < m_phizList.size();i++){
			
			Phiz t_phiz = (Phiz)m_phizList.elementAt(i);
			
			if(t_phiz.isFocus()){
				
				if(m_selectedCallback != null){
					m_selectedCallback.phizSelected(t_phiz.getPhizName());
				}				
				
				boolean t_shiftDown = (status & KeypadListener.STATUS_SHIFT) != 0;
				
				if(!t_shiftDown){
					getScreen().close();
				}
				
				return true;
			}	
		}
		
		return false;
	}
	
	public void setSelectedCallback(IPhizSelected _callback){
		m_selectedCallback = _callback;
	}
	
	protected boolean navigationClick(int status, int time){
		
		if(fieldClicked(status)){
			return true;
		}
		
		return super.navigationClick(status, time);
	}
	
	protected boolean keyDown(int keycode,int time){
			
		int key = Keypad.key(keycode);
		
		if(key == 10 && fieldClicked(Keypad.status(keycode))){
			return true;
		}
		
		return super.keyDown(keycode,time);
	}
		
	public void resetSelected(){
		m_move_x = m_move_y = 0;
		getField(0).setFocus();
	}
	
	protected boolean navigationMovement(int dx,int dy,int status,int time){
		
		m_move_x += dx;
		if(m_move_x >= m_maxRowNum){
			m_move_x = 0;
			m_move_y++;
		}
		
		if(m_move_x < 0){
			if(m_move_y > 0){
				m_move_y--;
				m_move_x = m_maxRowNum - 1;
			}else{
				m_move_x = 0;
			}			
		}
		
		m_move_y += dy;
		if(m_move_y >= m_maxColNum){
			m_move_y -= dy;
		}
		
		if(m_move_y < 0){
			m_move_y = 0;
		}
		
		int t_index = m_move_y * m_maxRowNum + m_move_x;
		int t_fieldNum = getFieldCount();
		
		if(t_index < t_fieldNum){
			getField(t_index).setFocus();
		}else{
			getField(t_fieldNum - 1).setFocus();
			m_move_x = t_fieldNum % m_maxRowNum - 1;
			m_move_y = m_maxColNum - 1;
		}
		
		return true;
	}
	
}
public class PhizSelectedScreen extends MainScreen{

	PhizMgr		m_phizMgr;
	
	public PhizSelectedScreen(recvMain _mainApp,IPhizSelected _selectedCallback){
		
		LabelField		t_prompt = new LabelField(recvMain.sm_local.getString(localResource.WEIBO_PHIZ_SCREEN_PROMPT),Field.NON_FOCUSABLE);
		
		add(t_prompt);
		add(new SeparatorField());
		
		Hashtable t_imageList = weiboTimeLineScreen.sm_weiboUIImage.getImageList();
		
		Vector t_phizList = new Vector();
		
		Enumeration elements = t_imageList.elements();
		while(elements.hasMoreElements()) {
		    ImageUnit t_unit = (ImageUnit)elements.nextElement();
		    
		    if(t_unit.getName().charAt(0) == '['){
		    	t_phizList.addElement(new Phiz(t_unit,weiboTimeLineScreen.sm_weiboUIImage));
		    }
		}
		
		m_phizMgr = new PhizMgr(t_phizList, _selectedCallback);
		add(m_phizMgr);
	}
	
	public PhizMgr getPhizMgr(){
		return m_phizMgr;
	}
	
	protected  void	onDisplay(){
		super.onDisplay();
		
		m_phizMgr.resetSelected();
	}

	protected boolean navigationMovement(int dx,int dy,int status,int time){
		return m_phizMgr.navigationMovement(dx, dy, status, time);
	}
}
