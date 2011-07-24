package com.yuchting.yuchberry.client.ui;

import java.util.Hashtable;

import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYDimension;
import net.rim.device.api.xml.jaxp.RIMSAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.yuchting.yuchberry.client.recvMain;

public class ImageSets {

	Hashtable	m_mapImageUnits = new Hashtable();
	
	String		m_name		= "";
	Bitmap		m_fullImage = null;
	
	public ImageSets(final String _imageSets)throws Exception{
		
		RIMSAXParser t_xml = new RIMSAXParser();
		t_xml.parse(UiApplication.getUiApplication().getClass().getResourceAsStream(_imageSets),new DefaultHandler(){
			 public void startElement(String uri, String localName, String qName, Attributes  attributes){
				 
				 if(localName.equals("Imageset")){
					 m_name = attributes.getValue("Name");
					 
					 try{
						 byte[] bytes = IOUtilities.streamToBytes(UiApplication.getUiApplication().getClass()
								 .getResourceAsStream(attributes.getValue("Imagefile")));		
						 m_fullImage =  EncodedImage.createEncodedImage(bytes, 0, bytes.length).getBitmap();
					 }catch(Exception e){
						 ((recvMain)UiApplication.getUiApplication()).DialogAlertAndExit("inner load image error " + _imageSets);
					 }
				 }
				 
				 if(localName.equals("Image")){
					
					 ImageUnit t_unit = new ImageUnit();
					 
					 t_unit.m_x = Integer.valueOf(attributes.getValue("XPos")).intValue();
					 t_unit.m_y = Integer.valueOf(attributes.getValue("YPos")).intValue();
					 t_unit.m_width = Integer.valueOf(attributes.getValue("Width")).intValue();
					 t_unit.m_height = Integer.valueOf(attributes.getValue("Height")).intValue();
					 
					 m_mapImageUnits.put(attributes.getValue("Name"),t_unit);
				 }							 
			 }
		});
	}
	
	public XYDimension getImageSize(String _name){
		ImageUnit t_unit = (ImageUnit)m_mapImageUnits.get(_name);
		if(t_unit != null){
			return new XYDimension(t_unit.m_width,t_unit.m_height);
		}
		
		return null;
	}
	
	public ImageUnit getImageUnit(String _name){
		return (ImageUnit)m_mapImageUnits.get(_name);
	}
	
	public void drawImage(Graphics _g,String _name,int _x,int _y){
		ImageUnit t_unit = (ImageUnit)m_mapImageUnits.get(_name);
		if(t_unit != null){
			_g.drawBitmap(_x,_y,t_unit.m_width,t_unit.m_height,m_fullImage,t_unit.m_x,t_unit.m_y);
		}
	}
	
	public void drawImage(Graphics _g,String _name,int _x,int _y,int _width,int _height){
		ImageUnit t_unit = (ImageUnit)m_mapImageUnits.get(_name);
		if(t_unit != null){
			if(_width > t_unit.m_width){
				_width = t_unit.m_width;
			}
			if(_height > t_unit.m_height){
				_height = t_unit.m_height;
			}
			_g.drawBitmap(_x,_y,_width,_height,m_fullImage,t_unit.m_x,t_unit.m_y);
		}
	}
	
	public void drawImage(Graphics _g,ImageUnit _unit,int _x,int _y){
		_g.drawBitmap(_x,_y,_unit.m_width,_unit.m_height,m_fullImage,_unit.m_x,_unit.m_y);
	}
	
	public void drawImage(Graphics _g,ImageUnit _unit,int _x,int _y,int _width,int _height){
		
		if(_width <= 0 || _height <= 0){
			return ;
		}
		
		if(_width > _unit.m_width){
			_width = _unit.m_width;
		}
		if(_height > _unit.m_height){
			_height = _unit.m_height;
		}
		_g.drawBitmap(_x,_y,_width,_height,m_fullImage,_unit.m_x,_unit.m_y);
	}
	
	public void drawImage(Graphics _g,ImageUnit _unit,int _x,int _y,int _width,int _height,int _u_x,int _u_y){
		
		if(_width <= 0 || _height <= 0){
			return ;
		}
		
		if(_u_x >= _unit.m_width -1 || _u_y >= _unit.m_height - 1){
			return ;
		}
		
		if(_width > _unit.m_width){
			_width = _unit.m_width;
		}
		if(_height > _unit.m_height){
			_height = _unit.m_height;
		}
		_g.drawBitmap(_x,_y,_width,_height,m_fullImage,_unit.m_x + _u_x,_unit.m_y + _u_y);
	}
	
	public void fillImageBlock(Graphics _g,ImageUnit _unit,int _x,int _y,int _width,int _height){
		
		if(_width <= 0 || _height <= 0){
			return ;
		}
		
		int t_horz_num =  _width / _unit.m_width;
		int t_vert_num =  _height / _unit.m_height;		

		for(int i = 0 ;i < t_vert_num;i++){
			for(int j = 0; j < t_horz_num;j++){
				drawImage(_g,_unit,_x + j * _unit.m_width,_y + i * _unit.m_height);
			}
		}
		
		int t_horz_remain_width = _width % _unit.m_width;
		if(t_horz_remain_width > 0){
			
			int t_horz_x	= _x + t_horz_num * _unit.m_width;

			for(int i = 0 ;i < t_vert_num;i++){
				drawImage(_g,_unit,t_horz_x, _y + i * _unit.m_height, 
						t_horz_remain_width, _unit.m_height);
				
			}
		}
		
		int t_vert_remain_height = _height % _unit.m_height;
		if(t_vert_remain_height > 0){
			
			int t_vert_y	= _y + t_vert_num * _unit.m_height;

			for(int i = 0 ;i < t_horz_num;i++){
				drawImage(_g,_unit,_x + i * _unit.m_width, t_vert_y , 
						_unit.m_width, t_vert_remain_height);
			}
		}
		
		if(t_horz_remain_width > 0 && t_vert_remain_height > 0){
			drawImage(_g,_unit,_x + t_horz_num * _unit.m_width, _y + t_vert_num * _unit.m_height, 
						t_horz_remain_width, t_vert_remain_height);
		}
	}
	
	public void drawBitmapLine(Graphics _g,ImageUnit _unit,int _x,int _y,int _width){
		if(_width <= 0){
			return ;
		}
		
		int t_horz_num = _width / _unit.m_width;
		for(int i = 0 ;i < t_horz_num;i++){
			drawImage(_g,_unit,_x + i * _unit.m_width, _y);
		}
		
		int t_horz_remain_width = _width % _unit.m_width;
		if(t_horz_remain_width > 0){
			drawImage(_g,_unit,_x + t_horz_num * _unit.m_width, _y, 
					t_horz_remain_width, _unit.m_height);
		}	
	}
	
}
