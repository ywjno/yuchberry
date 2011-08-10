package com.yuchting.yuchberry.client.weibo;

import java.util.Vector;

import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.ActiveFieldContext;
import net.rim.device.api.ui.component.ActiveFieldCookie;
import net.rim.device.api.ui.component.CookieProvider;
import net.rim.device.api.util.AbstractString;
import net.rim.device.api.util.Factory;
import net.rim.device.api.util.StringPattern;
import net.rim.device.api.util.StringPatternRepository;

/**
 *  copy from the blackberry system sample
 * 
 * The ActiveFieldCookie implementation allows for the factory to
 * provide a container for the zipcode information when moving
 * between the StringPattern and the MenuItem.
 */
class WeiboUserFindActiveFieldCookie implements ActiveFieldCookie
{
	private String	m_userName = "";
	   
    /**
     * Create the ZipCodeLookupActiveFieldCookie by passing in the zip code
     * @param data the zip code to lookup
     */
    public WeiboUserFindActiveFieldCookie(){}
    public WeiboUserFindActiveFieldCookie(String _name){
    	m_userName = _name;
    }
    
    public void setUserName(String _name){
    	m_userName = _name;
    }
    
    public String getUserName(){
    	return m_userName;
    }

    /**
     * This is an abstract method for the ActiveFieldCookie and must be implemented.
     * @return always return false.
     */
    public boolean invokeApplicationKeyVerb()
    {
        return false;
    }

    /**
     * In this case, we are under-utilizing the getFocusVerbs method by simply adding
     * our menu item to the list of items and returning.  Refer to the javadocs for
     * all of the different options here.
     */
    public MenuItem getFocusVerbs(CookieProvider provider, Object context, Vector items)
    {
    	WeiboItemField t_extendedItem = WeiboUserFindFactory.sm_mainScreen.m_currMgr.getCurrExtendedItem();
    	
    	WeiboUserFindFactory.sm_mainScreen.m_userGetInfoMenu.setUserName(m_userName);
    	WeiboUserFindFactory.sm_mainScreen.m_userAtUserMenu.setUserName(m_userName);
    	
    	items.addElement( WeiboUserFindFactory.sm_mainScreen.m_userGetInfoMenu );
    	items.addElement( WeiboUserFindFactory.sm_mainScreen.m_userAtUserMenu );
    	
    	if(t_extendedItem != null){
            
            if(!t_extendedItem.m_weibo.GetUserScreenName().equals(m_userName) || !t_extendedItem.m_weibo.IsOwnWeibo()){
            	WeiboUserFindFactory.sm_mainScreen.m_userSendMessageMenu.setUserName(m_userName);    	
                items.addElement( WeiboUserFindFactory.sm_mainScreen.m_userSendMessageMenu);	
            }
            
            if(!t_extendedItem.m_weibo.GetUserScreenName().equals(m_userName) 
            || t_extendedItem.m_weibo.GetWeiboClass() != fetchWeibo.TIMELINE_CLASS){
            	
            	WeiboUserFindFactory.sm_mainScreen.m_userFollowMenu.setUserName(m_userName);
                items.addElement( WeiboUserFindFactory.sm_mainScreen.m_userFollowMenu );
            }
            
    	}            
        
        return (MenuItem)items.elementAt(0);
    }
}

/**
 * This class provides an implementation of the StringPattern.
 * It is important to note that every single time a character
 * is entered into an active field that this method will be executed.
 * As such, it is incredibly important to stress that the findMatch method
 * must be meticulously written for speed and efficiency so as to not
 * alter the user experience.
 */
class WeiboUserFind extends StringPattern
{
    public WeiboUserFind() {}

    /**
     * For the purposes of this implementation, findMatch will match any five digit continuous number (no spaces, etc)
     * that it finds.  It is better to err on the side of caution and accept a zip code than exclude it.
     * @param str the AbstractString to search through.
     * @param beginIndex the beginning index in the string.
     * @param maxIndex the ending index in the string.
     * @param match the holder for information on the match if applicable
     * @return true if a match was found and false otherwise
     */
    public boolean findMatch( AbstractString str, int beginIndex, int maxIndex, StringPattern.Match match ){
    	
		char a = str.charAt(beginIndex);
		
		if(a == '@'){
			
			int i = beginIndex + 1;
			for(;i < maxIndex;i++){
				
				a = str.charAt(i);
				
				if(!isLeagalNameCharacter(a)){
					break;
				}
			}
			
			if(i != beginIndex + 1){
				match.beginIndex 	= beginIndex;
				match.endIndex		= i;
				match.id			= WeiboUserFindFactory.fsm_pattern_factory_id;
				match.prefixLength	= 0;
				
				return true;
			}
		}
		
		return false;
    }
    public boolean findMatch(AbstractString str, int beginIndex, StringPattern.Match match) {
    	super.findMatch(str,beginIndex,match);
    	return false;
    }
    
    public static boolean isLeagalNameCharacter(char a){
		
		if(a == '，' || a == '；' ||a == '：' ||a == '？' ||a == '‘'){
			return false;
		}
		
		if(Character.isDigit(a) || isChinese(a) || isAlpha(a)){
			return true;
		}
		
		if(a == '-' || a== '_' ){
			return true;
		}
		
		return false;
	}
	
	public static boolean isChinese(char a){ 
		int v = (int)a; 
		return (v>=19968 && v <=171941);	
	}
	
	public static boolean isAlpha(char a){
		return Character.isLowerCase(a) || Character.isUpperCase(a);
	}
}

/**
 * The ZipCodeLookupFactory provides the necessary piece
 * which allows the StringPattern.Match class to indicate
 * how it should move forward when a match is found.
 * One will note that the ID for the Factory is the same
 * ID that is passed back from the findMatch method in the
 * StringPattern.
 */
public class WeiboUserFindFactory implements Factory
{
	public final static long		fsm_pattern_factory_id = 155394686315L;
	public final static long		fsm_pattern_instance_id = 217827014192L;
	
	public static weiboTimeLineScreen	sm_mainScreen;
			
    public WeiboUserFindFactory(weiboTimeLineScreen _mainScreen){
    	
    	sm_mainScreen = _mainScreen;
    	
    	if(RuntimeStore.getRuntimeStore().get(fsm_pattern_factory_id) == null){
    		RuntimeStore.getRuntimeStore().put( fsm_pattern_factory_id, this);
    		
    		WeiboUserFind pattern = new WeiboUserFind();
    		StringPatternRepository.addPattern( pattern );
    	}    	
    }

    /**
     * Create a new instance of the ActiveFieldCookie
     * using the information passed into this method.
     * @param initialData An ActiveFieldContext that contains
     * the necessary information from the StringPattern class.
     * @return An ActiveFieldCookie embodying the matched pattern.
     */
    public Object createInstance( Object initialData ){
    	
        if(initialData instanceof ActiveFieldContext) {
        	
            ActiveFieldContext afc = (ActiveFieldContext)initialData;
            String stringData = (String)afc.getData();
     
            return new WeiboUserFindActiveFieldCookie(stringData.substring(1));
        }
        return null;
    }
}





