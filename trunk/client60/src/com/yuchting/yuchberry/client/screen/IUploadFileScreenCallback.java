package com.yuchting.yuchberry.client.screen;

public interface IUploadFileScreenCallback {
	public boolean clickOK(String _filename,int _size);
	public void clickDel(String _filename);
}