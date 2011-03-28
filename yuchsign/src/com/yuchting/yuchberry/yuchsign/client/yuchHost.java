package com.yuchting.yuchberry.yuchsign.client;

import com.google.gwt.xml.client.Element;

public class yuchHost {

	private String m_hostName = "";
	
	private String m_connectHost = "";

	private int m_httpPort = 4929;
	
	private String m_httpPassword = "";
	
	private String m_recommendHost = "";
	
	public String GetHostName(){return m_hostName;}
	public void SetHostName(String _name){m_hostName = _name;}
	
	public String GetConnectHost(){return m_connectHost;}
	public void SetConnectHost(String _host){m_connectHost = _host;}
	
	public int GetHTTPPort(){return m_httpPort;}
	public void SetHTTPPort(int _port){m_httpPort = _port;}
	
	public String GetHTTPPass(){return m_httpPassword;}
	public void SetHTTPPass(String _pass){m_httpPassword = _pass;}
	
	public String GetRecommendHost(){return m_recommendHost;}
	public void SetRecommendHost(String _recommendHost){m_recommendHost = _recommendHost;}
	
	public void OutputXMLData(StringBuffer _buffer){
		_buffer.append("<Host ").append("name=\"").append(m_hostName)
								.append("\" host=\"").append(m_connectHost)
								.append("\" port=\"").append(Integer.toString(m_httpPort))
								.append("\" pass=\"").append(m_httpPassword)
								.append("\" recom=\"").append(m_recommendHost)
				.append("\" />");
	}
	
	public void InputXMLData(final Element _elem)throws Exception{
		
		m_hostName		= yuchbber.ReadStringAttr(_elem,"name");
		m_connectHost	= yuchbber.ReadStringAttr(_elem,"host");
		m_httpPort		= yuchbber.ReadIntegerAttr(_elem,"port");
		m_httpPassword	= yuchbber.ReadStringAttr(_elem,"pass");
		m_recommendHost	= yuchbber.ReadStringAttr(_elem,"recom");
	}
	
	public int compareTo(yuchHost o) {
      return (o == null || o.m_hostName == null) ? -1 
    		  : -o.m_hostName.compareTo(m_hostName);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof yuchHost) {
        return m_hostName.equals(((yuchHost) o).m_hostName);
      }
      return false;
    }
}
