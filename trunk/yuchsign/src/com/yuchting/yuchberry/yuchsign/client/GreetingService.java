package com.yuchting.yuchberry.yuchsign.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService {
	
	String logonServer(String name,String password) throws Exception;
	
	String signinAccount(String name,String password,String verifyCode)throws Exception;
	
	String findPassword(String _signinName,String _verifyCode)throws Exception;
	
	String changePassword(String _signinName,String _verifyCode,String _origPass,String _pass)throws Exception;
	
	String syncAccount(String _xmlData,String verifyCode)throws Exception;
	
	String syncAccount_check(String _signinName,String _pass)throws Exception;
	
	String checkAccountLog(String _signinName,String _pass)throws Exception;
	
	String payTime(String _signinName,int _payType,int _fee)throws Exception;
	
	String getdownLev(String _signinName)throws Exception;
	
	
	// administrator function
	//
	String queryAlipay()throws Exception;
	
	String modifyAlipay(String _partnerID,String _key)throws Exception;
	
	String getHostList()throws Exception;
	
	String addHost(String _hostXMLData)throws Exception;
	
	String delHost(String _hostName)throws Exception;
	
	String modifyHost(String _hostName,String _hostXMLData)throws Exception;
	
	String getWeiboAuthURL(String _bber,String _type);
	String getWeiboAccessToken(String _bber);
	
	
}
