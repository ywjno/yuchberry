import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.mail.smtp.SMTPTransport;

class berrySvrPush extends Thread{
	
	berrySvrDeamon		m_serverDeamon;
	sendReceive			m_sendReceive;
	
	public berrySvrPush(berrySvrDeamon _svrDeamon){
		m_serverDeamon = _svrDeamon;
		m_sendReceive = new sendReceive(m_serverDeamon.m_socket);
		m_sendReceive.start();
	}
	
	public void run(){
		
		while(m_serverDeamon.isAlive() && m_serverDeamon.m_socket.isConnected()){
			
			try{
				m_serverDeamon.m_fetchMgr.CheckFolder();
				sleep(fetchMain.sm_pushInterval);

				Vector<fetchMail> t_unreadMailVector = m_serverDeamon.m_fetchMgr.m_unreadMailVector;
				for(int i = 0;i < t_unreadMailVector.size();i++){
					fetchMail t_mail = t_unreadMailVector.get(i); 
					
					ByteArrayOutputStream t_output = new ByteArrayOutputStream();
					
					t_output.write(msg_head.msgMail);
					t_mail.OutputMail(t_output);
					
					m_sendReceive.SendBufferToSvr(t_output.toByteArray(),false);					
				}				
				
			}catch(Exception _e){
				break;
			}
			
		}
	}
	
}

class berrySvrDeamon extends Thread{
	
	public fetchMgr		m_fetchMgr = null;
	public Socket		m_socket = null;
	
	private berrySvrPush m_pushDeamon = null;
	private sendReceive  m_sendReceive = null;
		
	public berrySvrDeamon(Socket _s,fetchMgr _mgr){
		m_fetchMgr 	= _mgr;
		m_socket	= _s;
		
		start();
		
		m_pushDeamon = new berrySvrPush(this);
		m_pushDeamon.start();
		
		m_sendReceive = new sendReceive(m_socket);
		m_sendReceive.start();
	}
	
	public void run(){
		
		// loop
		//
		while(true){
			if(!m_fetchMgr.IsConnected()){
				break;
			}
		
			// process....
			//
			try{
									
				byte[] t_package = m_sendReceive.RecvBufferFromSvr();
				
				ProcessPackage(t_package);
				
			}catch(Exception _e){
				try{
					m_socket.close();
				}catch(Exception e){}
				
				break;
			}
		}

	}
	
	private void ProcessPackage(byte[] _package)throws Exception{
		ByteArrayInputStream in = new ByteArrayInputStream(_package);
		
		switch(in.read()){
			case msg_head.msgMail:
				ProcessMail(in);
				break;
			case msg_head.msgSendMail:
				m_fetchMgr.SetBeginFetchIndex(fetchMail.ReadInt(in) + 1);
				break;
			default:
				throw new Exception("illegal client connect");
		}
	}
	
	private void ProcessMail(ByteArrayInputStream in)throws Exception{
		fetchMail t_mail = new fetchMail();
		t_mail.InputMail(in);
		
		m_fetchMgr.SendMail(t_mail);
		
		// receive send message to berry
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(msg_head.msgSendMail);
		os.write(t_mail.GetMailIndex());
		
		m_sendReceive.SendBufferToSvr(os.toByteArray(),false);
	}
}

class fetchMgr{
	
	final static int	ACCEPT_PORT = 9716;
	
	final static int	CHECK_NUM = 50;
	
	String 	m_protocol 	= null;
    String 	m_host 		= null;
    int		m_port		= 0;
    
    String 	m_protocol_send 	= null;
    String 	m_host_send 		= null;
    int		m_port_send			= 0;
    
    String 	m_inBox 	= "INBOX";
       
	String 	m_userName 	= null;
	String 	m_password 	= null;
	String	m_userPassword	= null;
	
	// Get a Properties object
    Properties m_sysProps = System.getProperties();
    Properties m_sysProps_send = System.getProperties();
    

    // Get a Session object
    Session m_session 	= null;
    Store 	m_store		= null;
    
    
    Session m_session_send 	= null;
    SMTPTransport m_sendTransport = null;
    
    	
    Vector<fetchMail> m_unreadMailVector = new Vector<fetchMail>();
    
    Vector<berrySvrDeamon>	m_vectConnect = new Vector<berrySvrDeamon>();
    
    // pushed mail index vector 
    Vector m_vectPushedMailIndex = new Vector();
    
    int		m_beginFetchIndex 	= 0;
    int		m_totalMailCount	= 0;
    
    int		m_unreadFetchIndex	= 0;
    
        
	public void InitConnect(String _protocol,
							String _host,
							int _port,
							String _protocol_send,
							String _host_send,
							int _port_send,
							String _username,
							String _password,
							String _userPassword) throws Exception{

    	DestroyConnect();
    	
		if(m_session != null){
			throw new Exception("has been initialize the session");
		}
		
    	m_session = Session.getInstance(m_sysProps, null);
    	m_session.setDebug(false);
		
    	m_protocol	= _protocol;
    	m_host		= _host;
    	m_port		= _port;
    	
    	m_protocol_send	= _protocol_send;
    	m_host_send		= _host_send;
    	m_port_send		= _port_send;
    	
    	m_userName	= _username;
    	m_password	= _password;
    	m_userPassword = _userPassword;
    	
    	m_beginFetchIndex = fetchMain.sm_fetchIndex;
    	
    	if(m_protocol == null){
    		m_protocol = "pop3";
    	}else{
    		
    		if(!m_protocol.equals("imap") 
    		&& !m_protocol.equals("pop3") 
    		&& !m_protocol.equals("pop3s") 
    		&& !m_protocol.equals("imaps")){
    			
    			m_protocol = "pop3";
    		}   		
	    }
    	
		
    	m_store = m_session.getStore(m_protocol);
    	m_store.connect(m_host,m_port,m_userName,m_password);
    	
    	// initialize the smtp transfer
    	//
    	m_session_send = Session.getInstance(m_sysProps_send, null);
    	m_session_send.setDebug(false);
    	
    	m_sendTransport = (SMTPTransport)m_session_send.getTransport(m_protocol_send);
    	   	
    	   	
    	//
    	//
    	ServerSocket t_svr = GetSocketServer(m_userPassword);
    	    	
		while(true){
			try{
    			m_vectConnect.addElement(new berrySvrDeamon(t_svr.accept(),this));
			}catch(Exception _e){
				
//				for(int i = 0;i < m_vectConnect.size();i++){
//					berrySvrDeamon d = m_vectConnect.get(i);
//					if(d.m_socket.isClosed()){
//						d.destroy();
//										
//						m_vectConnect.remove(i);
//						
//						i--;
//					}
//				}			
	    	}
    	}
    		
	}
	
	public int GetMailCountWhenFetched(){
		return m_totalMailCount;
	}
	
	public synchronized void SetBeginFetchIndex(int _index){
		m_beginFetchIndex = _index;
		
		try{
			
			Properties p = new Properties(); 
			p.load(new FileInputStream("config.ini"));
			p.setProperty("userFetchIndex",Integer.toString(_index));
			
			p.save(new FileOutputStream("config.ini"), "");
			p.clear();
			
		}catch(Exception _e){
			sendReceive.prt(_e.getMessage());
			_e.printStackTrace();
		}
		
	}
	
	public int GetBeginFetchIndex(){
		return m_beginFetchIndex;
	}
	
	public void SetUnreadFetchIndex(int _index){
		m_unreadFetchIndex = _index;
	}
	
	public int GetUnreadFetchIndex(){
		return m_unreadFetchIndex;
	}
	
	public void CheckFolder()throws Exception{
		
		Folder folder = m_store.getDefaultFolder();
	    if(folder == null) {
	    	throw new Exception("Cant find default namespace");
	    }
	    
	    folder = folder.getFolder("INBOX");
	    if (folder == null) {
	    	throw new Exception("Invalid INBOX folder");
	    }
	    	    
	    folder.open(Folder.READ_ONLY);
	   
	    if(m_totalMailCount != folder.getMessageCount()){
	    	m_totalMailCount = folder.getMessageCount();	    
		    final int t_startIndex = Math.max(m_totalMailCount - Math.min(CHECK_NUM,m_totalMailCount) + 1,m_beginFetchIndex);
		    
		    Message[] t_msgs = folder.getMessages(t_startIndex, m_totalMailCount);
		    
		    for(int i = 0;i < t_msgs.length;i++){
		    	
		    	Message t_msg = t_msgs[i];
		    	
		    	Flags flags = t_msg.getFlags();
	        	Flags.Flag[] flag = flags.getSystemFlags();  
	        	
	        	boolean t_isNew = true;
	        	for(int j = 0; j < flag.length; j++){
	                if (flag[j] == Flags.Flag.SEEN 
	                	&& flag[j] != Flags.Flag.DELETED
	                	&& flag[j] != Flags.Flag.DRAFT) {
	                	
	                    t_isNew = false;
	                    break;      
	                }
	            }      
	        	
		    	if(t_isNew){
		    		
		    		fetchMail t_mail = new fetchMail();
		    		t_mail.SetMailIndex(i + t_startIndex);
		    		t_mail.ImportMail(t_msg);
		    		
		    		m_unreadMailVector.addElement(t_mail);
		    	}
		    }
		    		    
		    
	    }	       
	    
	    folder.close(false);
	}
	
	public void SendMail(fetchMail _mail)throws Exception{
		
		Message msg = new MimeMessage(m_session_send);

		msg.setFrom(new InternetAddress(fetchMain.sm_strUserNameFull));
		
		String t_addressList = new String();

		
	    msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(parseAddressList(), false));
	    if (cc != null)
		msg.setRecipients(Message.RecipientType.CC,
					InternetAddress.parse(cc, false));
	    if (bcc != null)
		msg.setRecipients(Message.RecipientType.BCC,
					InternetAddress.parse(bcc, false));

	    msg.setSubject(subject);

	    String text = collect(in);

	    if (file != null) {
		// Attach the specified file.
		// We need a multipart message to hold the attachment.
		MimeBodyPart mbp1 = new MimeBodyPart();
		mbp1.setText(text);
		MimeBodyPart mbp2 = new MimeBodyPart();
		mbp2.attachFile(file);
		MimeMultipart mp = new MimeMultipart();
		mp.addBodyPart(mbp1);
		mp.addBodyPart(mbp2);
		msg.setContent(mp);
	    } else {
		// If the desired charset is known, you can use
		// setText(text, charset)
		msg.setText(text);
	    }

	    msg.setHeader("X-Mailer", mailer);
	    msg.setSentDate(new Date());
	    
		m_sendTransport.connect(m_userName,m_password);
		
	}
	
	public String parseAddressList(Vector<String> _list)throws Exception{
		String 	t_addressList = new String();
		
		for(int i = 0;i < _list.size();i++){
			t_addressList += _list.get(i);
			t_addressList += ",";
		}
		
		return t_addressList;
	}
	
	public void DestroyConnect()throws Exception{
		m_session = null;
		
		if(m_store != null){
			
		    m_unreadMailVector.clear();
		    m_vectConnect.clear();
		    
		    // pushed mail index vector 
		    m_vectPushedMailIndex.clear();
		    
			m_store.close();
			m_store = null;
		}
	}
		
	public boolean IsConnected(){
		return m_session != null;
	}
	
	public static SSLServerSocket GetSocketServer(String _userPassword)throws Exception{
		
		String	key				= "YuchBerryKey";  
		
		char[] keyStorePass		= _userPassword.toCharArray();
		char[] keyPassword		= _userPassword.toCharArray();
		
		KeyStore ks				= KeyStore.getInstance(KeyStore.getDefaultType());
		
		ks.load(new FileInputStream(key),keyStorePass);
		
		KeyManagerFactory kmf	= KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks,keyPassword);
		
		SSLContext sslContext = SSLContext.getInstance("SSLv3");
		sslContext.init(kmf.getKeyManagers(),null,null);
		  
		SSLServerSocketFactory factory=sslContext.getServerSocketFactory();
		
		return (SSLServerSocket)factory.createServerSocket(ACCEPT_PORT);
		  
	}
	
}

public class fetchMain{
	
	static String sm_protocol;
    static String sm_host;
    static int		sm_port	;
    
    static String sm_protocol_send;
    static String sm_host_send;
    static int		sm_port_send	;
    
    static String sm_inBox;
    
    static boolean sm_debug 		= false;
    
	static String sm_strUserName ;
	static String sm_strUserNameFull ;
	static String sm_strPassword ;
	static String sm_strUserPassword;
	static int		sm_pushInterval = 10000;
	
	static int		sm_fetchIndex = 1;
	
	public static void main(String[] _arg){
			
		Properties p = new Properties(); 
		fetchMgr t_manger = new fetchMgr();
		
		while(true){

			try{
				
				FileInputStream fs = new FileInputStream("config.ini");
				p.load(fs);
				
				sm_protocol			= p.getProperty("protocol");
				sm_host				= p.getProperty("host");
				sm_port				= Integer.valueOf(p.getProperty("port")).intValue();
				
				sm_protocol_send	= p.getProperty("protocol_send");
				sm_host_send		= p.getProperty("host_send");
				sm_port_send		= Integer.valueOf(p.getProperty("port_send")).intValue();
				
				sm_strUserNameFull		= p.getProperty("account");
				if(sm_strUserNameFull.indexOf('@') == -1 || sm_strUserNameFull.indexOf('.') == -1){
					throw new Exception("account : xxxxx@xxx.xxx such as 1234@gmail.com");
				}
				
				sm_strUserName = sm_strUserNameFull.substring(0, sm_strUserNameFull.indexOf('@'));
				
				sm_strPassword		= p.getProperty("password");
				sm_strUserPassword	= p.getProperty("userPassword");
				
				sm_fetchIndex		= Integer.valueOf(p.getProperty("userFetchIndex")).intValue();
				
				sm_pushInterval		= Integer.valueOf(p.getProperty("pushInterval")).intValue() * 1000;
				
				fs.close();
				p.clear();
				
				p = null;
				
				t_manger.InitConnect(sm_protocol, sm_host, sm_port, 
									sm_protocol_send, sm_host_send, sm_port_send, 
									sm_strUserName, sm_strPassword,sm_strUserPassword);
				
				
			}catch(Exception ex){
								
				System.out.println("Oops, got exception! " + ex.getMessage());
			    ex.printStackTrace();
			    
			    if(ex.getMessage().indexOf("Invalid credentials") != -1){
					// the password or user name is invalid..
					//
					System.out.println("the password or user name is invalid");
				}
			    
			    try{
			    	Thread.sleep(10000);
			    }catch(InterruptedException e){
			    	System.exit(0);
			    }
			}
			
			try{
				t_manger.DestroyConnect();
			}catch(Exception _e){
				System.exit(0);
			}
		}
	}
}