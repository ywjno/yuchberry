package com.yuchting.yuchberry.server.frame;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import com.yuchting.yuchberry.server.cryptPassword;

public class cryptPassTool_c {
	
	public cryptPassTool_c()throws Exception{
		
		System.out.println("crypt password tool(加密密码工具)");
		
		System.out.print("Input crypt key(请输入加密算子): ");
		BufferedReader bufin = new BufferedReader(new   InputStreamReader(System.in)); 
		String key = bufin.readLine();
				
		System.out.print("Input Password(请输入明文密码): ");
		String pass = bufin.readLine();
		
		cryptPassword t_crypt = new cryptPassword(cryptPassword.md5(key));
		String cryptPass = t_crypt.encrypt(pass);
		
		
		final String t_storeFile = "cryptPass.txt";
		System.out.println("Crypted Password(加密过后的密码)：" + cryptPass);
		System.out.println("Has written it to "+t_storeFile +" (已经保存到 "+t_storeFile +" 文件中)");
		
		FileOutputStream t_fileWrite = new FileOutputStream(t_storeFile);
		t_fileWrite.write(cryptPass.getBytes("UTF-8"));
		t_fileWrite.close();				
	}
	
	public static void main(String[] _arg)throws Exception{
		new cryptPassTool_c();
	}
}
