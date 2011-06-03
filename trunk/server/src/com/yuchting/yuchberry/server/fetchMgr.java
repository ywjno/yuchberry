package com.yuchting.yuchberry.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

public class fetchMgr{
		
	public final static String	fsm_configFilename 			= "config.xml";
				
	Logger	m_logger					= null;
	
	ServerSocket m_svr 					= null;
	
	String	m_prefix					= null;   
	String	m_userPassword				= null;
		
	int		m_listenPort 				= 9716;
	
	int		m_fetchInterval 			= 10;
	
	boolean m_convertToSimpleChar 		= false;

    boolean m_userSSL					= false;
        
	int					m_clientVer		= 0;
    
    Vector<fetchAccount> m_fetchAccount 		= new Vector<fetchAccount>();
        
    //! is connected?
    berrySvrDeamon	m_currConnect		= null;
    
    boolean		m_endListenState	= false;
        
    int				m_clientLanguage	= 0;
    
    String			m_latestVersion		= null;
    
    boolean		m_hasPrompt			= false; 
    
    String			m_passwordKey		= "";
    
    boolean		m_isCheckFolderState	= false;
    
    public void SetLatestVersion(String _version){
    	
    	if(m_latestVersion == null || !m_latestVersion.equals(_version)){
    		
    		m_latestVersion = _version;
        	
        	if(m_latestVersion != null && GetClientConnected() != null){
        		try{
        			m_hasPrompt = true;
        			SendNewVersionPrompt(GetClientConnected().m_sendReceive);
        			
        		}catch(Exception e){}
        	}else{
        		m_hasPrompt = true;
        	}
    	}
    }
    
    public boolean IsCheckFolderState(){
    	return m_isCheckFolderState;
    }
    
    public synchronized void SetCheckFolderState(boolean _state){
    	m_isCheckFolderState = _state;
    }
    
    public String GetLatestVersion(){
    	return m_latestVersion;
    }
    
    public int GetConnectClientVersion(){
    	return m_clientVer;
    }
    
    public void SendNewVersionPrompt(sendReceive _sendRecv)throws Exception{
    	if(m_hasPrompt){
    		
    		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
    		t_os.write(msg_head.msgLatestVersion);
    		sendReceive.WriteString(t_os, m_latestVersion, true);
    		
    		_sendRecv.SendBufferToSvr(t_os.toByteArray(), false);
    		
    		m_hasPrompt = false;
    	}
    }
	
	public int GetServerPort(){
		return m_listenPort;
	}
	
	public String GetUserPassword(){
		return m_userPassword;
	}
	
	public boolean IsUseSSL(){
		return m_userSSL;
	}
	
	public String GetPrefixString(){
		return m_prefix;
	}
	
	public int GetPushInterval(){
		return m_fetchInterval;
	}
	
	
	
	public berrySvrDeamon GetClientConnected(){
		return	m_currConnect;
	}
	
	public int GetClientLanguage(){
		return m_clientLanguage;
	}
	
	public synchronized void SetClientConnected(berrySvrDeamon _set){
		m_currConnect = _set;
	}
	
	public void SendData(ByteArrayOutputStream _os,boolean _sendImm)throws Exception{
		SendData(_os.toByteArray(),_sendImm);		
	}
	
	public void SendData(byte[] _data,boolean _sendImm)throws Exception{
		
		if(m_currConnect == null){
			throw new Exception("Client has been closed <m_currConnect == null>");
		}
		
		if(m_currConnect.m_sendReceive == null ){
			throw new Exception("Client has been closed <m_currConnect.m_sendReceive == null>");
		}
		
		if(!m_currConnect.m_sendReceive.isAlive()){
			throw new Exception("Client has been closed <!m_currConnect.m_sendReceive.isAlive()>");
		}
		
		m_currConnect.m_sendReceive.SendBufferToSvr(_data, _sendImm);		
	}

		       
	public void InitConnect(String _prefix,Logger _logger)throws Exception{
		
		m_prefix	= _prefix;
		m_logger	= _logger;
		
		DestroyAllSession();
		m_fetchAccount.removeAllElements();
		
		FileInputStream t_xmlFile = new FileInputStream(m_prefix + fsm_configFilename);
		try{
			
			SAXReader t_xmlReader = new SAXReader();
			Document t_doc = t_xmlReader.read(t_xmlFile); 
			Element t_root = t_doc.getRootElement();
			
			m_userPassword					= fetchAccount.ReadStringAttr(t_root,"userPassword");
			m_listenPort					= fetchAccount.ReadIntegerAttr(t_root,"serverPort");
			m_fetchInterval					= fetchAccount.ReadIntegerAttr(t_root,"pushInterval");
			m_userSSL						= fetchAccount.ReadBooleanAttr(t_root,"userSSL");
			m_convertToSimpleChar			= fetchAccount.ReadBooleanAttr(t_root,"convertoSimpleChar");
			
			for( Iterator i = t_root.elementIterator("EmailAccount"); i.hasNext();){
	            Element element = (Element) i.next();
	            fetchAccount t_email = new fetchEmail(this);
	            t_email.InitAccount(element);
	            
	            m_fetchAccount.addElement(t_email);
	        }
			
			for( Iterator i = t_root.elementIterator("WeiboAccount"); i.hasNext();){
	            Element element = (Element) i.next();
	            
	            String t_type = fetchAccount.ReadStringAttr(element,"type");
	            
	            fetchAccount t_weibo = null;
	            
	            if(t_type.equalsIgnoreCase("sina")){
	            	t_weibo = new fetchSinaWeibo(this);
	            }else if(t_type.equalsIgnoreCase("twitter")){
	            	t_weibo = new fetchTWeibo(this);
	            }else{
	            	continue;
	            }
	            
	            t_weibo.InitAccount(element);
	            
	            m_fetchAccount.addElement(t_weibo);
	        }
			

			
	    	
		}catch(Exception ex){
			
			m_logger.PrinterException(ex);
			throw ex;
			
		}finally{
			t_xmlFile.close();
		}
			
	}
	
	public String GetPasswordKey(){
		return m_passwordKey;
	}
	
	static byte[] fsm_userPasswordErrorData = null;
	
	static{
		ByteArrayOutputStream t_os = new ByteArrayOutputStream();
		t_os.write(msg_head.msgNote);
		try{
			sendReceive.WriteString(t_os,"User Password or Port Error!(用户密码或端口错误)",false);
		}catch(Exception e){}
		
		fsm_userPasswordErrorData = t_os.toByteArray();
	}
	
	public sendReceive ValidateClient(Socket _s)throws Exception{
		
		sendReceive t_tmp = null;
		
		try{
			
			t_tmp = new sendReceive(_s.getOutputStream(),_s.getInputStream());
			
			m_logger.LogOut("some client<"+ _s.getInetAddress().getHostAddress() +"> connecting ,waiting for auth");
			
			// first handshake with the client via CA instead of 
			// InputStream.read function to get the information within 1sec time out
			//
			//if(_s instanceof SSLSocket){
				//((SSLSocket)_s).startHandshake();
			//}
			
			// wait for signIn first
			//
			_s.setSoTimeout(10000);			
			
			ByteArrayInputStream in = new ByteArrayInputStream(t_tmp.RecvBufferFromSvr());
									
			int t_msg_head = in.read();
			String t_sendPass = sendReceive.ReadString(in); 
		
			if(msg_head.msgConfirm != t_msg_head 
			|| !t_sendPass.equals(m_userPassword)){
				
				t_tmp.SendBufferToSvr(fsm_userPasswordErrorData,true);
				
				throw new Exception("illeagel client<"+ _s.getInetAddress().getHostAddress() +"> sent Pass<"+t_sendPass+"> connected.");			
			}
			
			if((m_clientVer = sendReceive.ReadInt(in)) < 2){
				throw new Exception("error version client<"+ _s.getInetAddress().getHostAddress() +"> connected.");
			}
			
			
			// read the language state
			//
			m_clientLanguage = in.read();
			
			if(m_clientVer >= 3){
				String t_clientVersion = sendReceive.ReadString(in);
				
				if(GetLatestVersion() != null
				&& !GetLatestVersion().equals(t_clientVersion)){
					// send the latest version information
					//
					SendNewVersionPrompt(t_tmp);
				}
			}
			
			if(m_clientVer >= 5){
				m_passwordKey = sendReceive.ReadString(in);
			}
			
			_s.setSoTimeout(0);
			_s.setKeepAlive(true);
						
			return t_tmp;
			
		}catch(Exception _e){
			
			// time out or other problem
			//
			try{
				_s.close();
			}catch(Exception e){}
			
			m_logger.PrinterException(_e);
			t_tmp.CloseSendReceive();
			
			throw _e;
		}	
	}

	public void StartListening(boolean _fulltest){
		
		try{
			synchronized (this) {
				m_endListenState = false;
			}
			
			ResetAllAccountSession(_fulltest);
			
			m_svr = GetSocketServer(m_userPassword,m_userSSL);			
	    	
			while(!m_endListenState){
				try{
										
					Socket t_sock = m_svr.accept();
					
					synchronized (this) {
						
						try{
							
							sendReceive t_sendReceive = ValidateClient(t_sock);
							
							if(m_currConnect != null){
								
								m_currConnect.m_isCloseByMgr = true;
								
								// kick the former client
								//
								synchronized (m_currConnect) {
									if(m_currConnect.m_socket != null && !m_currConnect.m_socket.isClosed()){
										m_currConnect.m_socket.close();
									}
									m_currConnect.m_socket = null;
								}		
								
								// wait	quit
								while(!m_currConnect.m_quit){
									Thread.sleep(50);
								}
							}
													
							m_currConnect = new berrySvrDeamon(this,t_sock,t_sendReceive);
							
						}catch(Exception e){
							t_sock.close();
							throw e;
						}
					}			
					
				}catch(Exception _e){
					m_logger.PrinterException(_e);
		    	}
	    	}			
			
			if(m_currConnect != null && m_currConnect.m_socket != null){
				m_currConnect.m_socket.close();
			}
			
			synchronized (this) {
				if(m_svr != null){
					m_svr.close();
					m_svr= null;
				}	
			}
			
			
		}catch(Exception ex){
			m_logger.PrinterException(ex);
		}
		
		SetClientConnected(null);		
	}
	
	public synchronized void EndListening(){
		
		m_endListenState = true;
		
		try{
			
			DestroyAllSession();
			
			if(m_svr != null){
				m_svr.close();
				m_svr = null;
			}
			
		}catch(Exception e){
			if(m_logger != null){
				m_logger.PrinterException(e);
			}
		}		
	}
			
	public void DestroyAllSession()throws Exception{
		for(fetchAccount accout :m_fetchAccount){
			accout.DestroySession();
		}
	}
	
	public void ResetAllAccountSession(boolean _testAll)throws Exception{
		for(fetchAccount accout :m_fetchAccount){
			try{
				accout.ResetSession(_testAll);
			}catch(Exception e){
				Exception t_newExp = new Exception(accout.GetAccountName() + " error:" + e.getMessage());
				t_newExp.setStackTrace(e.getStackTrace());
				
				throw t_newExp;
			}
			
		}
	}
	
	public String GetAccountName(){
		final int t_slash = m_prefix.lastIndexOf("/");
		if(t_slash != -1){
			return m_prefix.substring(0, t_slash);
		}
		return m_prefix;
	}
	
	public void ProcessPackage(byte[] _package)throws Exception{
		
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
				
		final int t_msg_head = in.read();
		
		switch(t_msg_head){			
			case msg_head.msgKeepLive:
				break;
			case msg_head.msgSponsorList:
				ProcessSponsorList(in);
				break;
			default:
			{
				for(fetchAccount account :m_fetchAccount){
					if(account.ProcessNetworkPackage(_package)){
						break;
					}
				}
			}
		}		
	}
	
	public void SendImmMail(final String _subject ,final String _contain,final String _from){
		for(fetchAccount account :m_fetchAccount){
			
			if(account instanceof fetchEmail){
				((fetchEmail)account).SendImmMail(_subject, _contain, _from);
				break;
			}
		}
	}
	public void CheckAccountFolders(){
				
		for(fetchAccount account :m_fetchAccount){			
			try{
				
				account.CheckFolder();
				
			}catch(Exception e){
				
				m_logger.PrinterException(e);
							
				int t_reconnectNum = 0;
				while(t_reconnectNum++ < 5){
								
					try{
						Thread.sleep(t_reconnectNum * 20000);
					}catch(Exception ex){
						m_logger.LogOut(account.GetAccountName() + " checkfolder interpret.");
						return;
					}
					
					try{
						
						account.ResetSession(false);
						
						return ;
						
					}catch(Exception ex){
						m_logger.PrinterException(ex);
					}					
				}
				
				if(t_reconnectNum > 5){
					m_logger.LogOut(account.GetAccountName() + " failed to ResetSession!");
				}
				
			}
		}
	}
		
	public void Push(sendReceive _send){
						
		for(fetchAccount account :m_fetchAccount){					
			try{				
				account.PushMsg(_send);
			}catch(Exception e){
				m_logger.PrinterException(e);
			}			
		}
	}
		
	private void ProcessSponsorList(ByteArrayInputStream _in){
		try{
			
			// read the google code host page
			//
			final String ft_URL = new String("http://code.google.com/p/yuchberry/wiki/Thanks_sheet");
			String charSet = "utf-8";
			StringBuffer t_stringBuffer = new StringBuffer();
			
			URL url = new URL(ft_URL);
			URLConnection con = url.openConnection();
			con.setAllowUserInteraction(false);
			con.connect();
			   
			String type = URLConnection.guessContentTypeFromStream(con.getInputStream());
			
			if (type == null)
				type = con.getContentType();
			
			if (type == null || type.trim().length() == 0 || type.trim().indexOf("text/html") < 0){
				return ;
			}
			
			if(type.indexOf("charset=") > 0){
				charSet = type.substring(type.indexOf("charset=") + 8);
			}
						
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), charSet));
			
			String temp;
			while ((temp = in.readLine()) != null) {
				t_stringBuffer.append(temp+"\n");
			}
			in.close();
	        	        
	        String t_line = fetchMgr.ParseHTMLText(t_stringBuffer.toString(),false);
	        
	        final int t_start = t_line.indexOf("##@##");
	        final int t_end = t_line.indexOf("@@#@@");
	        if(t_start != -1 && t_end != -1){
	        	t_line = t_line.substring(t_start + 5 ,t_end);
	        }
	        t_line = t_line.replace("&para;","");
	        
	        ByteArrayOutputStream t_os = new ByteArrayOutputStream();
	        t_os.write(msg_head.msgSponsorList);
	        sendReceive.WriteString(t_os,t_line,m_convertToSimpleChar);
	        
	        SendData(t_os,true);
	        
		}catch(Exception _e){}
	}
	
		
	static public String ReadSimpleIniFile(String _file,String _decodeName,Vector _lines)throws Exception{
		
		File t_file = new File(_file);
		
		String t_ret = new String();
		
		if(_lines != null){
			_lines.removeAllElements();
		}
		
		if(t_file.exists()){
			BufferedReader in = new BufferedReader(
									new InputStreamReader(
											new FileInputStream(_file),_decodeName));
			
			StringBuffer t_stringBuffer = new StringBuffer();
			String t_line = null;
			
			boolean t_firstLine = true;
			while((t_line = in.readLine()) != null){
				
				if(t_firstLine && _decodeName.equals("UTF-8")){
					
					byte[] t_bytes = t_line.getBytes("UTF-8");
					
					// BOM process
					//
					if(t_bytes.length >= 3
						&& t_bytes[0] == -17 && t_bytes[1] == -69 && t_bytes[2] == -65){						
						
						if(t_bytes.length == 3){
							t_line = "";
						}else{
							t_line = new String(t_bytes,3,t_bytes.length - 3,"UTF-8");
						}
															
					}
				}
				
				t_firstLine = false;
								
				if(!t_line.startsWith("#")){
					t_stringBuffer.append(t_line + "\n");
					
					if(_lines != null){
						_lines.addElement(t_line);
					}
				}
			}
			
			t_ret = t_stringBuffer.toString();
			in.close();
		}
		
		return t_ret;
	}
	
		
	public ServerSocket GetSocketServer(String _userPassword,boolean _ssl)throws Exception{
		
		if(_ssl){
			String	key				= m_prefix + "YuchBerrySvr.key";  
			
			char[] keyStorePass		= _userPassword.toCharArray();
			char[] keyPassword		= _userPassword.toCharArray();
			
			KeyStore ks				= KeyStore.getInstance(KeyStore.getDefaultType());
			
			ks.load(new FileInputStream(key),keyStorePass);
			
			KeyManagerFactory kmf = null;
			
			try{
				kmf = KeyManagerFactory.getInstance("SunX509");
			}catch(Exception e){
				kmf = KeyManagerFactory.getInstance("IbmX509");
			}
			
			kmf.init(ks,keyPassword);
			
			SSLContext sslContext = SSLContext.getInstance("SSLv3");
			sslContext.init(kmf.getKeyManagers(),null,null);
			  
			SSLServerSocketFactory factory=sslContext.getServerSocketFactory();
			
			SSLServerSocket t_socket = (SSLServerSocket)factory.createServerSocket(m_listenPort); 
			//t_socket.setNeedClientAuth(true);
			
			return t_socket;
			
		}else{
			return new ServerSocket(m_listenPort);
		}		  
	}
	
	static public String ParseHTMLText(String _html,boolean _shortURL){
		StringBuffer t_text = new StringBuffer();	
		StringBuffer t_shorterText = new StringBuffer();
		
		boolean t_shorted = false;
		try{
			Parser parser = new Parser(_html,null);
			parser.setEncoding("UTF-8");
			
	        NodeList list = parser.parse(new  NodeFilter() {
	        								public boolean accept(Node node) {
	        									return node instanceof TextNode || node instanceof LinkTag ;
	        								}
	        							});
	        
	        Node[] nodes = list.toNodeArray();

            for (int i = 1; i < nodes.length; i++){
                Node nextNode = nodes[i];

                if (nextNode instanceof TextNode){
                    TextNode textnode = (TextNode) nextNode;
                    t_text.append(textnode.getText());
                    
                    if(textnode.isWhiteSpace()){
                    	t_text.append("\n");
                    }                    
                    
                }else{
                	
                	LinkTag link = (LinkTag)nextNode;
                	if(_shortURL){
                		t_text.append(GetShortURL(link.getLink()));
                		t_shorted = true;
                	}else{
                		t_text.append(link.getLink());
                	}
                	
                	t_text.append("\n");
                }              
            }            
            
            int t_emptyCharCounter = 0;
            
            for(int i = 0;i < t_text.length();i++){
            	final char t_char = t_text.charAt(i);
            	if(IsEmptyChar(t_char)){
            		if(t_emptyCharCounter++ < 2){
            			t_shorterText.append(t_char);
            		}
            	}else{
            		t_emptyCharCounter = 0;
            		t_shorterText.append(t_char);
            	}            	
            }
            
		}catch(Exception _e	){}
		
		String t_result = t_shorterText.toString();
		
		t_result = t_result.replace("&lt;", "<");
		t_result = t_result.replace("&gt;", ">");
		t_result = t_result.replace("&amp;", "&");
		t_result = t_result.replace("&apos;", "'");
		t_result = t_result.replace("&quot;", "\"");
		t_result = t_result.replace("&nbsp;", " ");	
		
		t_result = t_result.replaceAll("<!--(?s).*?-->","");
		
		if(t_shorted){
			return "[yuchberry prompt:some URL would be shorted]\n" + t_result;
		}else{
			return t_result;
		}
		
	}
	
	static public boolean IsEmptyChar(final char _char){
		return _char == ' ' || _char == '\n' || _char == '\t' || _char == '\r';
	}
	
	static public boolean IsEmptyLine(final String _string){
		for(int i = 0;i < _string.length();i++){
			if(!IsEmptyChar(_string.charAt(i))){
				return false;
			}
		}
		
		return true;
	}
	
	static private String GetShortURL(String _longURL){
		
		try{
			URL is_gd = new URL("http://is.gd/api.php?longurl=" + _longURL);
			
	        URLConnection yc = is_gd.openConnection();
	        BufferedReader in = new BufferedReader(
	                                new InputStreamReader(yc.getInputStream()));
	        
	        String inputLine = in.readLine();	        
	        in.close();
	        
	        return (inputLine != null && inputLine.length() < _longURL.length()) ? inputLine:_longURL ;
	        
		}catch(Exception _e){}
		
		return _longURL;
		
        
	}
}