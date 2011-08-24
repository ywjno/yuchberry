package com.yuchting.yuchberry.client.weibo;

import local.localResource;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;

import com.yuchting.yuchberry.client.recvMain;
import com.yuchting.yuchberry.client.ui.ImageUnit;

public class WeiboUpdateField extends WeiboItemField{
	
	private ImageUnit m_updateBitmap = null;
		
	private String m_updatePromptText = recvMain.sm_local.getString(localResource.WEIBO_UPDATE_WEIBO_LABEL);
	
	String m_sendUpdateText = "";
	
	public WeiboUpdateField(WeiboMainManager _mainManager){
		super(_mainManager);
		
		m_updateBitmap = weiboTimeLineScreen.sm_weiboUIImage.getImageUnit("compose");
	}
	
	public void AddDelControlField(boolean _add){
		AddDelEditTextArea(_add,m_sendUpdateText);
		
		if(_add){
			
			if(m_sendUpdateText.length() != 0){
				m_parentManager.m_editTextArea.select(true);
				m_parentManager.m_editTextArea.setCursorPosition(m_sendUpdateText.length()-1);
			}
		}
		
	}
	
	public int getPreferredHeight() {
		if(m_parentManager.getCurrExtendedItem() == this){
			return sm_fontHeight + sm_editTextAreaHeight;
		}else{
			return sm_fontHeight;
		}
	}
	
	public void sublayout(int width, int height){
		
		if(m_parentManager.getCurrExtendedItem() == this){
					
			setPositionChild(m_parentManager.m_editTextArea,0,0);
			layoutChild(m_parentManager.m_editTextArea,getPreferredWidth(),sm_editTextAreaHeight);
			
			height = sm_fontHeight + sm_editTextAreaHeight;
			
		}else{		
			height = sm_fontHeight;
		}
		
		setExtent(fsm_weiboItemFieldWidth,height);
	}
		
	public void subpaint(Graphics _g){
		
		if(m_parentManager.getCurrExtendedItem() == this){
			
			paintChild(_g,m_parentManager.m_editTextArea);
			
			int t_y = getPreferredHeight() - 1;
			_g.drawLine(0,t_y, getPreferredWidth(),t_y);
			
		}else{
			paintFocus(_g,isFocus());
		}
	}
	
	public void paintFocus(Graphics _g,boolean _on){
		
		int oldColour = _g.getColor();
		Font oldFont	= _g.getFont();
		
        try{
			
			if(_on){
			
				_g.setColor(fsm_selectedColor);
				_g.fillRect(1,1,getPreferredWidth() - 2,getPreferredHeight());
				
			}else{
				
				if(m_parentManager.getCurrExtendedItem() != null){
					_g.setColor(fsm_darkColor);
					_g.fillRect(0,0, getPreferredWidth(),getPreferredHeight());
				}else{
					_g.setColor(0xc0c0c0);
					_g.fillRect(0,0,getPreferredWidth(),getPreferredHeight());
				}				
			}	
        	
			weiboTimeLineScreen.sm_weiboUIImage.drawImage(_g,m_updateBitmap,2,0);
        	
			_g.setColor(0x757575);
			_g.setFont(WeiboItemField.sm_boldFont);
        	_g.drawText(m_updatePromptText,m_updateBitmap.getWidth() + 2,2,Graphics.ELLIPSIS);
        	
        }finally{
        	_g.setColor( oldColour );
        	_g.setFont(oldFont);
        }
	}
}