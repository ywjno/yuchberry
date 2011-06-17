package com.yuchting.yuchberry.client.weibo;

import java.util.Calendar;
import java.util.Date;

import local.localResource;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.TextField;

import com.yuchting.yuchberry.client.recvMain;

public class WeiboItemField extends Manager{
	
	public final static int		fsm_weiboItemFieldWidth		= recvMain.fsm_display_width - WeiboMainManager.fsm_scrollbarSize;
	public final static int		fsm_maxWeiboTextLength		= 140;
	public final static int		fsm_headImageWidth 			= fetchWeibo.fsm_headImageSize;
	public final static int		fsm_headImageTextInterval	= 3;
	
	public final static int		fsm_weiboSignImageSize		= 16;
	
	public final static int		fsm_weiboVIPImageSize		= 12;
	public final static int		fsm_weiboBBerImageSize		= 12; 
	
	public final static int		fsm_maxWeiboAbstractLength	= 20;
	
	public final static int		fsm_textWidth				= fsm_weiboItemFieldWidth - fsm_headImageWidth - fsm_headImageTextInterval;
	public final static int		fsm_editTextWidth			= fsm_weiboItemFieldWidth;
	
	public final static int		fsm_commentTextWidth		= fsm_weiboItemFieldWidth - fsm_headImageTextInterval;
	
	public final static int		fsm_darkColor				= 0xdfdfdf;
	public final static int		fsm_selectedColor			= 0x2020ff;
	public final static int		fsm_spaceLineColor			= 0x8f8f8f;
	public final static int		fsm_ownWeiboColor			= 0xffffee;
	
	public static TextField 	sm_testTextArea			= new TextField(Field.READONLY){
		public void setText(String _text){
			super.setText(_text);
			layout(WeiboItemField.fsm_textWidth,1000);
		}
	};
	
	public static TextField 	sm_testCommentTextArea	= new TextField(Field.READONLY){
		public void setText(String _text){
			super.setText(_text);
			layout(WeiboItemField.fsm_commentTextWidth,1000);
		}
	};	
	
	public static Font		sm_defaultFont				= sm_testTextArea.getFont();
	public static int		sm_fontHeight				= sm_defaultFont.getHeight() + 2;
	
	public static int		sm_editTextAreaHeight		= 0;
	

	public static int		sm_atBut_x					= sm_defaultFont.getAdvance(recvMain.sm_local.getString(localResource.WEIBO_FORWARD_WEIBO_BUTTON_LABEL)) 
															+ WeiboItemField.fsm_headImageTextInterval;
	
	public static int		sm_favoriteBut_x			= sm_atBut_x + sm_defaultFont.getAdvance(recvMain.sm_local.getString(localResource.WEIBO_AT_WEIBO_BUTTON_LABEL)) 
															+ WeiboItemField.fsm_headImageTextInterval;
	
	public static int		sm_picBut_x					= sm_favoriteBut_x + sm_defaultFont.getAdvance(recvMain.sm_local.getString(localResource.WEIBO_FAVORITE_WEIBO_BUTTON_LABEL)) 
															+ WeiboItemField.fsm_headImageTextInterval;
		
	public static int		sm_imageAreaMinHeight		= WeiboItemField.fsm_weiboSignImageSize + WeiboItemField.fsm_headImageWidth + WeiboItemField.fsm_headImageTextInterval;
	
	public final static int		fsm_closeHeight		= sm_fontHeight * 2 + 1;
	public final static int		fsm_maxDisplayRows	= (recvMain.fsm_display_height - WeiboHeader.fsm_headHeight) / fsm_closeHeight + 2; 

	static String	sm_followTextEmptySpace				= "   --";
	static {
		int t_followTextAdvance = sm_defaultFont.getAdvance(recvMain.sm_local.getString(localResource.WEIBO_FOLLOW_USER_BUTTON_LABEL));
		while(true){
			if(sm_defaultFont.getAdvance(sm_followTextEmptySpace) >= t_followTextAdvance){
				break;
			}else{
				sm_followTextEmptySpace = " " + sm_followTextEmptySpace ;
			}
		}
		
		sm_followTextEmptySpace = "   " + sm_followTextEmptySpace;
	}
	

	public static	boolean		sm_commentFirst		= false;
	public static	boolean		sm_displayHeadImage	= false;
	public static boolean			sm_simpleMode		= false;
	
	static public final int fsm_controlField_text 		= 0;
	static public final int fsm_controlField_comment 		= 1;
	static public final int fsm_controlField_forwardBtn 	= 2;
	static public final int fsm_controlField_atBtn 		= 3;
	static public final int fsm_controlField_favorBtn		= 4;
	static public final int fsm_controlField_picBtn		= 5;
	static public final int fsm_controlField_followBtn	= 6;
	static public final int fsm_controlField_editText		= 7;
	
	boolean[]				m_hasControlField = {false,false,false,false,false,false,false,false};
	
	fetchWeibo				m_weibo			= null;
		
	String					m_simpleAbstract = null;
	String					m_weiboText		= null;
	String					m_commentText	= null;
	
	String					m_weiboPic		= null;
	
	int						m_extendHeight 	= sm_fontHeight;
	
	int						m_functionButton_y	= sm_fontHeight;
	int						m_commentText_y		= sm_fontHeight;
	int						m_commentText_height= sm_fontHeight;
	int						m_textHeight		= sm_fontHeight;
	
	WeiboMainManager		m_parentManager		= null;
	
	WeiboHeadImage			m_headImage = null;
	
	WeiboItemFocusField		m_focusField = new WeiboItemFocusField(this);
	
	public WeiboItemFocusField getFocusField(){
		return m_focusField;
	}
	
	public WeiboItemField(WeiboMainManager _manager){
		super(Manager.NO_VERTICAL_SCROLL );
		
		m_parentManager = _manager;
	}

	public WeiboItemField(fetchWeibo _weibo,WeiboHeadImage _headImage,WeiboMainManager _manager){
		super(Manager.NO_VERTICAL_SCROLL);
		
		m_weibo 		= _weibo;
		m_headImage 	= _headImage;
		m_parentManager = _manager;
		
		StringBuffer t_weiboTextBuffer = new StringBuffer();
		
		t_weiboTextBuffer.append("@").append(m_weibo.GetUserScreenName()).append(" :").append(m_weibo.GetText());
		
		if(!sm_simpleMode){
			t_weiboTextBuffer.append("\n").append(sm_followTextEmptySpace).append(recvMain.sm_local.getString(localResource.WEIBO_SOURCE_PREFIX))
			.append(parseSource(m_weibo.GetSource()));
		}						
		
		m_weiboText 		= t_weiboTextBuffer.toString();
		t_weiboTextBuffer = null;
		
		sm_testTextArea.setText(m_weiboText);
		
		m_simpleAbstract		= m_weibo.GetText().length() > fsm_maxWeiboAbstractLength ? 
									(m_weibo.GetText().substring(0,fsm_maxWeiboAbstractLength) + "...") :  m_weibo.GetText();
									
		m_textHeight			= sm_testTextArea.getHeight();
		
		m_functionButton_y		= Math.max(m_textHeight,sm_imageAreaMinHeight) + fsm_headImageTextInterval;
				
		if(m_weibo.GetCommentWeibo() != null){
			
			fetchWeibo t_comment = m_weibo.GetCommentWeibo();
			
			StringBuffer t_commentText = new StringBuffer();
			t_commentText.append("@").append(t_comment.GetUserScreenName()).append(":").append(t_comment.GetText());
			if(!sm_simpleMode){
				t_commentText.append("\n").append(sm_followTextEmptySpace).append(recvMain.sm_local.getString(localResource.WEIBO_SOURCE_PREFIX))
				.append(parseSource(t_comment.GetSource()));
			}
						
					
			m_commentText = t_commentText.toString();
			
			t_commentText = null;
			
			sm_testCommentTextArea.setText(m_commentText);
			
			m_commentText_height = sm_testCommentTextArea.getHeight() + fsm_headImageTextInterval;
			
			m_commentText_y = m_functionButton_y + fsm_headImageTextInterval;
			
			m_functionButton_y = m_commentText_y + m_commentText_height + fsm_headImageTextInterval;
			
			if(m_weibo.GetCommentWeibo().GetOriginalPic().length() != 0 ){
				m_weiboPic = m_weibo.GetCommentWeibo().GetOriginalPic();
			}
			
		}else{
			
			m_commentText_height = 0;
			if(m_weibo.GetOriginalPic().length() != 0){
				m_weiboPic = m_weibo.GetOriginalPic(); 
			}
		}
		
		if(!sm_simpleMode){
			m_extendHeight 			= m_functionButton_y + sm_fontHeight + fsm_headImageTextInterval;
		}else{
			m_extendHeight 			= m_functionButton_y + fsm_headImageTextInterval;
		}
		
	}
	
	public WeiboMainManager getParentManager(){
		return m_parentManager;
	}
	
	public static  String parseSource(String _source){
		int t_start = _source.indexOf('>');
		int t_end = _source.lastIndexOf('<');
		if(t_start != -1 && t_end != -1){
			return _source.substring(t_start + 1,t_end);
		}
		
		return _source;
	}
	
	public void AddDelControlField(boolean _add){
		if(_add){
			
			if(WeiboItemField.sm_commentFirst){
				
				if(m_commentText != null){
					if(!m_hasControlField[fsm_controlField_comment]){
						m_hasControlField[fsm_controlField_comment] = true;
						m_parentManager.m_commentTextArea.setText(m_commentText);
						add(m_parentManager.m_commentTextArea);
					}
					
					if(!sm_simpleMode){
						if(!m_hasControlField[fsm_controlField_followBtn]){
							m_hasControlField[fsm_controlField_followBtn] = true;
							add(m_parentManager.m_followCommentUser);
						}	
					}
					
				}
				
				if(!m_hasControlField[fsm_controlField_text]){
					m_hasControlField[fsm_controlField_text] = true;
					m_parentManager.m_textArea.setText(m_weiboText);
					add(m_parentManager.m_textArea);
				}
				
				
			}else{
				
				if(!m_hasControlField[fsm_controlField_text]){
					m_hasControlField[fsm_controlField_text] = true;
					m_parentManager.m_textArea.setText(m_weiboText);
					add(m_parentManager.m_textArea);
				}
				
				if(m_commentText != null){
					if(!m_hasControlField[fsm_controlField_comment]){
						m_hasControlField[fsm_controlField_comment] = true;
						m_parentManager.m_commentTextArea.setText(m_commentText);
						add(m_parentManager.m_commentTextArea);
					}
					
					if(!sm_simpleMode){
						if(!m_hasControlField[fsm_controlField_followBtn]){
							m_hasControlField[fsm_controlField_followBtn] = true;
							add(m_parentManager.m_followCommentUser);
						}
					}
				}
			}		
			
			if(!sm_simpleMode){
				if(!m_hasControlField[fsm_controlField_forwardBtn]){
					m_hasControlField[fsm_controlField_forwardBtn] = true;
					add(m_parentManager.m_forwardBut);
				}
				
				if(!m_hasControlField[fsm_controlField_atBtn]){
					m_hasControlField[fsm_controlField_atBtn] = true;
					add(m_parentManager.m_atBut);
				}
				
				if(!m_hasControlField[fsm_controlField_favorBtn]){
					m_hasControlField[fsm_controlField_favorBtn] = true;
					add(m_parentManager.m_favoriteBut);
				}
				
				if(!m_hasControlField[fsm_controlField_picBtn]){
					m_hasControlField[fsm_controlField_picBtn] = true;
					add(m_parentManager.m_picBut);
				}
			}
			
			
		}else{
			
			
			if(m_hasControlField[fsm_controlField_comment]){
				m_hasControlField[fsm_controlField_comment] = false;
				delete(m_parentManager.m_commentTextArea);
			}
			
			if(m_hasControlField[fsm_controlField_followBtn]){
				m_hasControlField[fsm_controlField_followBtn] = false;
				delete(m_parentManager.m_followCommentUser);
			}
			
			if(m_hasControlField[fsm_controlField_text]){
				m_hasControlField[fsm_controlField_text] = false;
				delete(m_parentManager.m_textArea);
			}
								
			
			if(m_hasControlField[fsm_controlField_forwardBtn]){
				m_hasControlField[fsm_controlField_forwardBtn] = false;
				delete(m_parentManager.m_forwardBut);
			}
			
			if(m_hasControlField[fsm_controlField_atBtn]){
				m_hasControlField[fsm_controlField_atBtn] = false;
				delete(m_parentManager.m_atBut);
			}
			
			if(m_hasControlField[fsm_controlField_favorBtn]){
				m_hasControlField[fsm_controlField_favorBtn] = false;
				delete(m_parentManager.m_favoriteBut);
			}	
			
			if(m_hasControlField[fsm_controlField_picBtn]){
				m_hasControlField[fsm_controlField_picBtn] = false;
				delete(m_parentManager.m_picBut);
			}
		}							
	}
	
	public void AddDelEditTextArea(boolean _add,String _text){
		if(_add){
			
			m_parentManager.m_editTextArea.setText(_text);
			m_parentManager.RefreshEditTextAreHeight();
			
			m_parentManager.setCurrEditItem(this);
			
			if(!m_hasControlField[fsm_controlField_editText]){
				m_hasControlField[fsm_controlField_editText] = true;
				add(m_parentManager.m_editTextArea);
			}
						
		}else{
			
			m_parentManager.setCurrEditItem(null);
			WeiboItemField.sm_editTextAreaHeight = 0;
			
			if(m_hasControlField[fsm_controlField_editText]){
				m_hasControlField[fsm_controlField_editText] = false;
				delete(m_parentManager.m_editTextArea);
			}
		}
	}
	
	
	
	
	public int getPreferredWidth() {
		return fsm_weiboItemFieldWidth;
	}
	
	public int getPreferredHeight() {
		if(m_parentManager.getCurrExtendedItem() == this){
			
			if(m_parentManager.getCurrEditItem() == this){
				return m_extendHeight + sm_editTextAreaHeight;
			}
			
			return m_extendHeight;
		}else{
			return fsm_closeHeight;
		}
	}
	
	public void sublayout(int width, int height){
		
		if(m_parentManager.getCurrExtendedItem() == this){
			
			int t_commentText_y = WeiboItemField.sm_commentFirst?0:m_commentText_y;
			
			int t_text_y = WeiboItemField.sm_commentFirst?m_commentText_height:0;
		
			if(m_commentText != null){
				// comment area
				//
				setPositionChild(m_parentManager.m_commentTextArea,fsm_headImageTextInterval,t_commentText_y);
				layoutChild(m_parentManager.m_commentTextArea,fsm_commentTextWidth,m_functionButton_y - m_commentText_y);
				
				if(!sm_simpleMode){
					// follow button
					//
					setPositionChild(m_parentManager.m_followCommentUser,3,t_commentText_y + m_commentText_height - sm_fontHeight - 5);
					layoutChild(m_parentManager.m_followCommentUser,m_parentManager.m_followCommentUser.getPreferredWidth(),
													m_parentManager.m_followCommentUser.getPreferredHeight());
				}
			}
			
			if(!sm_simpleMode){
				// forward button
				//
				setPositionChild(m_parentManager.m_forwardBut,0,m_functionButton_y);
				layoutChild(m_parentManager.m_atBut,m_parentManager.m_atBut.getPreferredWidth(),m_parentManager.m_atBut.getPreferredHeight());
				
				
				// at button
				//
				setPositionChild(m_parentManager.m_atBut,sm_atBut_x,m_functionButton_y);
				layoutChild(m_parentManager.m_forwardBut,m_parentManager.m_forwardBut.getPreferredWidth(),m_parentManager.m_forwardBut.getPreferredHeight());
				
				// favorite button
				//
				setPositionChild(m_parentManager.m_favoriteBut,sm_favoriteBut_x,m_functionButton_y);
				layoutChild(m_parentManager.m_favoriteBut,m_parentManager.m_favoriteBut.getPreferredWidth(),m_parentManager.m_favoriteBut.getPreferredHeight());
				
				if(m_weiboPic != null){
					// open the browser to check the picture button 
					//
					setPositionChild(m_parentManager.m_picBut,sm_picBut_x,m_functionButton_y);
					layoutChild(m_parentManager.m_picBut,m_parentManager.m_picBut.getPreferredWidth(),m_parentManager.m_picBut.getPreferredHeight());
				}
			}
			
			// text area
			//
			setPositionChild(m_parentManager.m_textArea,fsm_headImageWidth + fsm_headImageTextInterval,t_text_y + 2);
			layoutChild(m_parentManager.m_textArea,fsm_textWidth,m_textHeight);
						
			if(m_parentManager.getCurrEditItem() == this){
				
				setPositionChild(m_parentManager.m_editTextArea,0,m_extendHeight);				
				layoutChild(m_parentManager.m_editTextArea,fsm_weiboItemFieldWidth,sm_editTextAreaHeight);
				
				height = m_extendHeight + sm_editTextAreaHeight;
				
			}else{
				height = m_extendHeight;
			}
						
		}else{		
			height = fsm_closeHeight;
		}
		
		setExtent(fsm_weiboItemFieldWidth,height);
	}
	
	static Calendar sm_calendar = Calendar.getInstance();
	static Date		sm_timeDate = new Date();
	
	
	static protected synchronized String getTimeString(fetchWeibo _weibo){
		
		sm_timeDate.setTime(_weibo.GetDateTime());
		sm_calendar.setTime(sm_timeDate);		
		
		int t_minutes = sm_calendar.get(Calendar.MINUTE);
		
		if(t_minutes > 9){
			return sm_calendar.get(Calendar.DAY_OF_MONTH) + "d " 
					+ sm_calendar.get(Calendar.HOUR_OF_DAY) + ":" 
					+ t_minutes;
		}else{
			return sm_calendar.get(Calendar.DAY_OF_MONTH) + "d " 
					+ sm_calendar.get(Calendar.HOUR_OF_DAY) + ":" 
					+ "0" + t_minutes;
		}		
	}
		
	public void subpaint(Graphics _g){
		
		if(m_parentManager.getCurrExtendedItem() == this){
			
			int t_textStart_y = WeiboItemField.sm_commentFirst?m_commentText_height : 0;
			
			_g.drawBitmap(0, t_textStart_y, fsm_weiboSignImageSize, fsm_weiboSignImageSize, 
						weiboTimeLineScreen.GetWeiboSign(m_weibo), 0, 0);
			
			displayHeadImage(_g,0, t_textStart_y + fsm_weiboSignImageSize + fsm_headImageTextInterval,true);
						
			int t_startSign_x = fsm_weiboSignImageSize;
			
			// name VIP sign
			//
			if(m_weibo.IsSinaVIP()){
				_g.drawBitmap(t_startSign_x,t_textStart_y,fsm_weiboVIPImageSize, fsm_weiboVIPImageSize, 
						weiboTimeLineScreen.GetSinaVIPSignBitmap(), 0, 0);
				
				t_startSign_x += fsm_weiboVIPImageSize;

			}
			
			// name BBer sign
			//
			if(m_weibo.IsBBer()){
				_g.drawBitmap(t_startSign_x,t_textStart_y,fsm_weiboBBerImageSize, fsm_weiboBBerImageSize, 
								weiboTimeLineScreen.GetBBerSignBitmap(), 0, 0);
			}
			
			if(!sm_simpleMode){
				paintChild(_g,m_parentManager.m_atBut);
				paintChild(_g,m_parentManager.m_forwardBut);
				paintChild(_g,m_parentManager.m_favoriteBut);
				
				if(m_weiboPic != null){
					paintChild(_g,m_parentManager.m_picBut);
				}
			}
			
			paintChild(_g,m_parentManager.m_textArea);
			
			if(m_commentText != null){
				
				int t_commentText_y = WeiboItemField.sm_commentFirst?0:m_commentText_y;
				
				// comment area
				//
				// draw a round rectangle of text area
				int color		= _g.getColor();
				try{
					_g.setColor(0xefefef);
					_g.fillRoundRect(1,t_commentText_y,fsm_commentTextWidth,m_commentText_height,10,10);				
					
				}finally{
					_g.setColor(color);
				}
				paintChild(_g,m_parentManager.m_commentTextArea);
				
				if(!sm_simpleMode){
					paintChild(_g,m_parentManager.m_followCommentUser);
				}
			}
			
			if(m_parentManager.getCurrEditItem() == this){
				paintChild(_g,m_parentManager.m_editTextArea);
			}
			
			// draw the finally line
			//
			int t_y = getPreferredHeight() - 1;
			_g.drawLine(0,t_y, fsm_weiboItemFieldWidth,t_y);
			
			
			// draw a round rectangle of text area
			int color		= _g.getColor();
			try{
				_g.setColor(0x8f8f8f);
				_g.drawRoundRect(fsm_headImageWidth + fsm_headImageTextInterval - 1 ,t_textStart_y + 1,fsm_textWidth,m_textHeight,10,10);				
				
			}finally{
				_g.setColor(color);
			}
			
		}else{		
			paintFocus(_g,isFocus());
		}		
	}
	
	public void paintFocus(Graphics _g,boolean _on){
		
		final int t_firstLineHeight = 2;
		final int t_leadingSpace = 2;
		
		int color		= _g.getColor();
		try{
			
			if(m_parentManager.getCurrExtendedItem() != null){
				_g.setColor(fsm_darkColor);
				_g.fillRect(0,0, fsm_weiboItemFieldWidth,fsm_closeHeight);
			}
			
			if(_on){
				_g.setColor(fsm_selectedColor);
				_g.fillRect(0, 0, fsm_weiboItemFieldWidth,fsm_closeHeight);
			}else{
				if(m_weibo.IsOwnWeibo() && m_parentManager.getCurrExtendedItem() == null){
					_g.setColor(fsm_ownWeiboColor);
					_g.fillRect(0, 0, fsm_weiboItemFieldWidth,fsm_closeHeight);
				}
				_g.setColor(fsm_spaceLineColor);
				_g.drawLine(0,fsm_closeHeight - 1,fsm_weiboItemFieldWidth,fsm_closeHeight - 1);
			}			
			
			// weibo sign 
			//
			_g.drawBitmap(t_leadingSpace, t_firstLineHeight, fsm_weiboSignImageSize, fsm_weiboSignImageSize, 
						weiboTimeLineScreen.GetWeiboSign(m_weibo), 0, 0);
			
			int t_nameLeadingSpace = t_leadingSpace;
			
			if(sm_displayHeadImage){
				// display head image when closed
				//
				displayHeadImage(_g,t_leadingSpace + fsm_weiboSignImageSize + fsm_headImageTextInterval,
								t_firstLineHeight,_on);
				t_nameLeadingSpace += fsm_headImageWidth + fsm_headImageTextInterval;
			}

			// name 
			//
			if(_on){
				_g.setColor(0xffffff);
			}else{
				_g.setColor(0);
			}
			
			// display name
			//
			String t_displayName = null;
			if(m_weibo.GetWeiboStyle() == fetchWeibo.TWITTER_WEIBO_STYLE
			|| m_weibo.GetWeiboStyle() == fetchWeibo.QQ_WEIBO_STYLE ){
				t_displayName = m_weibo.GetUserName();
			}else{
				t_displayName = m_weibo.GetUserScreenName();
			}
			
			int t_nameLength = _g.drawText(t_displayName,
										fsm_weiboSignImageSize + t_nameLeadingSpace,
										t_firstLineHeight,Graphics.ELLIPSIS);
			
			// add the weibo sign size
			t_nameLength += fsm_weiboSignImageSize + t_nameLeadingSpace;
			
			// name VIP sign
			//
			if(m_weibo.IsSinaVIP()){
				_g.drawBitmap(t_nameLength + t_leadingSpace, t_firstLineHeight, fsm_weiboVIPImageSize, fsm_weiboVIPImageSize, 
						weiboTimeLineScreen.GetSinaVIPSignBitmap(), 0, 0);
				
				t_nameLength += fsm_weiboVIPImageSize;

			}
			
			// name BBer sign
			//
			if(m_weibo.IsBBer()){
				_g.drawBitmap(t_nameLength + t_nameLeadingSpace, t_firstLineHeight, 
								fsm_weiboBBerImageSize, fsm_weiboBBerImageSize, 
								weiboTimeLineScreen.GetBBerSignBitmap(), 0, 0);
			}
			
			// contain abstract
			//
			if(_on){
				_g.setColor(0xffffff);
			}else{
				_g.setColor(0x8f8f8f);		
			}
	        _g.drawText(m_simpleAbstract,t_nameLeadingSpace + fsm_weiboSignImageSize,sm_fontHeight + fsm_headImageTextInterval ,Graphics.ELLIPSIS);
	        
	        // time string
	        //
	        if(_on){
	        	_g.setColor(0xffffff);
	        }else{
	        	_g.setColor(0);
	        }		        
	        String t_dateString = getTimeString(m_weibo);		        
	        _g.drawText(t_dateString,fsm_weiboItemFieldWidth - _g.getFont().getAdvance(t_dateString)
	        				,t_firstLineHeight,Graphics.ELLIPSIS);
	       

		}finally{
			_g.setColor(color);
		}
	}
	
	private void displayHeadImage(Graphics _g,int _x,int _y,boolean _focus){
		
		_g.drawBitmap(_x,_y,fsm_headImageWidth,fsm_headImageWidth,m_headImage.m_headImage,0,0);
		
		if(m_parentManager.getCurrExtendedItem() == null){
			if(!_focus){
				_g.drawBitmap(_x,_y,fsm_headImageWidth,fsm_headImageWidth,weiboTimeLineScreen.GetHeadImageMaskBitmap(),0,0);
			}
		}else if(m_parentManager.getCurrExtendedItem() == this){
			_g.drawBitmap(_x,_y,fsm_headImageWidth,fsm_headImageWidth,weiboTimeLineScreen.GetHeadImageMaskBitmap(),0,0);
		}
	}	
}