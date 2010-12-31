package com.yuchting.yuchberry.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class sendReceive extends Thread{
	
	OutputStream		m_socketOutputStream = null;
	InputStream			m_socketInputStream = null;
	
	
	private Vector		m_unsendedPackage 		= new Vector();
	private Vector		m_unprocessedPackage 	= new Vector();
	
	boolean			m_closed				= false;
		
	public sendReceive(OutputStream _socketOut,InputStream _socketIn){
		m_socketOutputStream = _socketOut;
		m_socketInputStream = _socketIn;
				
		start();
	}
		
	//! send buffer
	public synchronized void SendBufferToSvr(byte[] _write,boolean _sendImm)throws Exception{	
		m_unsendedPackage.addElement(_write);
		
		if(_sendImm){
			SendBufferToSvr_imple(PrepareOutputData());
		}
	}
	
	public void CloseSendReceive(){
		
		if(m_closed == false){
			m_closed = true;
	
			m_unsendedPackage.clear();
			m_unprocessedPackage.clear();
						
			interrupt();
			

			/* the server cant close the socket ...
			 * 
			try{
				m_socketOutputStream.close();
				m_socketInputStream.close();
			}catch(Exception _e){}
			*/
			
			while(isAlive()){
				try{
					sleep(10);
				}catch(Exception _e){};			
			}	
		}		
	}
	
	private synchronized byte[] PrepareOutputData()throws Exception{
		
		if(m_unsendedPackage.isEmpty()){
			return null;
		}
		
		ByteArrayOutputStream t_stream = new ByteArrayOutputStream();
				
		for(int i = 0;i < m_unsendedPackage.size();i++){
			byte[] t_package = (byte[])m_unsendedPackage.elementAt(i);	
			
			WriteInt(t_stream, t_package.length);
						
			t_stream.write(t_package);
		}
		
		m_unsendedPackage.removeAllElements();
		
		return t_stream.toByteArray();
	}
	
	//! send buffer implement
	private void SendBufferToSvr_imple(byte[] _write)throws Exception{
		
		if(_write == null){
			return;
		}		
		
		OutputStream os = m_socketOutputStream;
		
		ByteArrayOutputStream zos = new ByteArrayOutputStream();
		GZIPOutputStream zo = new GZIPOutputStream(zos);
		zo.write(_write);
		zo.close();	
		
		byte[] t_zipData = zos.toByteArray();
		
		if(t_zipData.length > _write.length){
			// if the ZIP data is large than original length
			// NOT convert
			//
			WriteInt(os,_write.length << 16);
			os.write(_write);
			os.flush();
			
		}else{
			WriteInt(os,(_write.length << 16) | t_zipData.length);
			os.write(t_zipData);
			os.flush();
			
		}
				
	}
	
	public void run(){
		
		try{
			
			while(!m_closed){
				SendBufferToSvr_imple(PrepareOutputData());
				sleep(500);
			}
			
		}catch(Exception _e){}
		
	}

	//! recv buffer
	public byte[] RecvBufferFromSvr()throws Exception{
		
		if(!m_unprocessedPackage.isEmpty()){
			byte[] t_ret = (byte[])m_unprocessedPackage.elementAt(0);
			m_unprocessedPackage.removeElementAt(0);
			
			return t_ret;
		}
		
		InputStream in = m_socketInputStream;

		int t_len = ReadInt(in);
		
		final int t_ziplen = t_len & 0x0000ffff;
		final int t_orglen = t_len >>> 16;
				
		byte[] t_orgdata = new byte[t_orglen];
				
		if(t_ziplen == 0){
			
			ForceReadByte(in, t_orgdata, t_orglen);
			
		}else{
			
			byte[] t_zipdata = new byte[t_ziplen];
			
			ForceReadByte(in, t_zipdata, t_ziplen);
			
			GZIPInputStream zi	= new GZIPInputStream(
										new ByteArrayInputStream(t_zipdata));

			ForceReadByte(zi,t_orgdata,t_orglen);
			
			zi.close();
		}
		
		byte[] t_ret = ParsePackage(t_orgdata);
		t_orgdata = null;
		
		return t_ret;
	}
	
	private byte[] ParsePackage(byte[] _wholePackage)throws Exception{
		
		ByteArrayInputStream t_packagein = new ByteArrayInputStream(_wholePackage);
		int t_len = ReadInt(t_packagein);
					
		byte[] t_ret = new byte[t_len];
		t_packagein.read(t_ret,0,t_len);
		
		t_len += 4;
		
		while(t_len < _wholePackage.length){
			
			final int t_packageLen = ReadInt(t_packagein); 
			
			byte[] t_package = new byte[t_packageLen];
			
			t_packagein.read(t_package,0,t_packageLen);
			t_len += t_packageLen + 4;
			
			m_unprocessedPackage.addElement(t_package);			
		}		
		
		return t_ret;		
	}
	// static function to input and output integer
	//
	static public void WriteStringVector(OutputStream _stream,Vector _vect,boolean _converToSimpleChar)throws Exception{
		
		final int t_size = _vect.size();
		WriteInt(_stream,t_size);
		
		for(int i = 0;i < t_size;i++){
			WriteString(_stream,(String)_vect.elementAt(i),_converToSimpleChar);
		}
	}
	
	static public void WriteString(OutputStream _stream,String _string,boolean _converToSimpleChar)throws Exception{
		
		final byte[] t_strByte = _converToSimpleChar?complTosimple(_string).getBytes("UTF-8"):_string.getBytes("UTF-8");
		
		WriteInt(_stream,t_strByte.length);
		if(t_strByte.length != 0){
			_stream.write(t_strByte);
		}
	}
	
		
	static public void ReadStringVector(InputStream _stream,Vector _vect)throws Exception{
		
		_vect.removeAllElements();
		
		final int t_size = ReadInt(_stream);
				
		for(int i = 0;i < t_size;i++){
			_vect.addElement(ReadString(_stream));
		}
	}
	
	static public String ReadString(InputStream _stream)throws Exception{
		
		final int len = ReadInt(_stream);
		
		if(len != 0){
			byte[] t_buffer = new byte[len];
			
			ForceReadByte(_stream,t_buffer,len);

			return new String(t_buffer,"UTF-8");
		}
		
		return new String("");
		
	}
	
	static public int ReadInt(InputStream _stream)throws Exception{
		return _stream.read() | (_stream.read() << 8) | (_stream.read() << 16) | (_stream.read() << 24);
	}
	
	static public long ReadLong(InputStream _stream)throws Exception{
		final int t_timeLow = sendReceive.ReadInt(_stream);
		final long t_timeHigh = sendReceive.ReadInt(_stream);
				
		if(t_timeLow >= 0){
			return ((t_timeHigh << 32) | (long)(t_timeLow));
		}else{
			return ((t_timeHigh << 32) | (((long)(t_timeLow & 0x7fffffff)) | 0x80000000L));
		}
	}
		
	static public void WriteLong(OutputStream _stream,long _val)throws Exception{		
		sendReceive.WriteInt(_stream,(int)_val);
		sendReceive.WriteInt(_stream,(int)(_val >>> 32));
	}

	static public void WriteInt(OutputStream _stream,int _val)throws Exception{
		_stream.write(_val);
		_stream.write(_val >>> 8 );
		_stream.write(_val >>> 16);
		_stream.write(_val >>> 24);
	}
	
	static public void ForceReadByte(InputStream _stream,byte[] _buffer,int _readLen)throws Exception{
		int t_readIndex = 0;
		int t_counter = 0;
		
		while(_readLen > t_readIndex){
			final int t_c = _stream.read(_buffer,t_readIndex,_readLen - t_readIndex);
			if(t_c > 0){
				t_readIndex += t_c;
			}else{
				t_counter++;
				
				if(t_counter > 10){
					throw new Exception("FroceReadByte failed to read " + _readLen );
				}
			}
		}
	}
	
	static public String complTosimple(String compl){
        String str="";
        for(int i=0;i<compl.length();i++){
            if(turnCompl().indexOf(compl.charAt(i))!=-1)
                str+=turnSimpe().charAt(turnCompl().indexOf(compl.charAt(i)));
            else
                str+=compl.charAt(i);
        }
        return str;
    }
    
	static public String turnSimpe(){
        return "�������������������������������������������������������������������°ðİŰưǰȰɰʰ˰̰ͰΰϰаѰҰӰ԰հְװذٰڰ۰ܰݰް߰�������������������������������������������������������������������������������������������������������������±ñıűƱǱȱɱʱ˱̱ͱαϱбѱұӱԱձֱױرٱڱ۱ܱݱޱ߱�������������������������������������������������������������������������������������������������������������²òĲŲƲǲȲɲʲ˲̲ͲβϲвѲҲӲԲղֲײزٲڲ۲ܲݲ޲߲�������������������������������������������������������������������������������������������������������������³óĳųƳǳȳɳʳ˳̳ͳγϳгѳҳӳԳճֳ׳سٳڳ۳ܳݳ޳߳�������������������������������������������������������������������������������������������������������������´ôĴŴƴǴȴɴʴ˴̴ʹδϴдѴҴӴԴմִ״شٴڴ۴ܴݴ޴ߴ�������������������������������������������������������������������������������������������������������������µõĵŵƵǵȵɵʵ˵̵͵εϵеѵҵӵԵյֵ׵صٵڵ۵ܵݵ޵ߵ�������������������������������������������������������������������������������������������������������������¶öĶŶƶǶȶɶʶ˶̶Ͷζ϶жѶҶӶԶնֶ׶ضٶڶ۶ܶݶ޶߶�������������������������������������������������������������������������������������������������������������·÷ķŷƷǷȷɷʷ˷̷ͷηϷзѷҷӷԷշַ׷طٷڷ۷ܷݷ޷߷�������������������������������������������������������������������������������������������������������������¸øĸŸƸǸȸɸʸ˸̸͸θϸиѸҸӸԸոָ׸ظٸڸ۸ܸݸ޸߸�������������������������������������������������������������������������������������������������������������¹ùĹŹƹǹȹɹʹ˹̹͹ιϹйѹҹӹԹչֹ׹عٹڹ۹ܹݹ޹߹�������������������������������������������������������������������������������������������������������������ºúĺźƺǺȺɺʺ˺̺ͺκϺкѺҺӺԺպֺ׺غٺںۺܺݺ޺ߺ�������������������������������������������������������������������������������������������������������������»ûĻŻƻǻȻɻʻ˻̻ͻλϻлѻһӻԻջֻ׻ػٻڻۻܻݻ޻߻�������������������������������������������������������������������������������������������������������������¼üļżƼǼȼɼʼ˼̼ͼμϼмѼҼӼԼռּ׼ؼټڼۼܼݼ޼߼�������������������������������������������������������������������������������������������������������������½ýĽŽƽǽȽɽʽ˽̽ͽνϽнѽҽӽԽսֽ׽ؽٽڽ۽ܽݽ޽߽�������������������������������������������������������������������������������������������������������������¾þľžƾǾȾɾʾ˾̾;ξϾоѾҾӾԾվ־׾ؾپھ۾ܾݾ޾߾�������������������������������������������������������������������������������������������������������������¿ÿĿſƿǿȿɿʿ˿̿ͿοϿпѿҿӿԿտֿ׿ؿٿڿۿܿݿ޿߿��������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿������������������������������������������������������������������������������������������������������������������������������áâãäåæçèéêëìíîïðñòóôõö÷ùúûüýþÿ������������������������������������������������������������������������������������������������������������������������������ġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿ������������������������������������������������������������������������������������������������������������������������������šŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ������������������������������������������������������������������������������������������������������������������������������ơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿ������������������������������������������������������������������������������������������������������������������������������ǡǢǣǤǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǶǷǸǹǺǻǼǽǾǿ������������������������������������������������������������������������������������������������������������������������������ȡȢȣȤȥȦȧȨȩȪȫȬȭȮȯȰȱȲȳȴȵȶȷȸȹȺȻȼȽȾȿ������������������������������������������������������������������������������������������������������������������������������ɡɢɣɤɥɦɧɨɩɪɫɬɭɮɯɰɱɲɳɴɵɶɷɸɹɺɻɼɽɾɿ����������������������������������������������������������������������������������������������������������������������������ʡʢʣʤʥʦʧʨʩʪʫʬʭʮʯʰʱʲʳʴʵʶʷʸʹʺʻʼʽʾʿ������������������������������������������������������������������������������������������������������������������������������ˡˢˣˤ˥˦˧˨˩˪˫ˬ˭ˮ˯˰˱˲˳˴˵˶˷˸˹˺˻˼˽˾˿������������������������������������������������������������������������������������������������������������������������������̴̵̶̷̸̡̢̧̨̣̤̥̦̩̫̬̭̮̯̰̱̲̳̹̺̻̼̽̾̿������������������������������������������������������������������������������������������������������������������������������ͣͤͥͦͧͨͩͪͫͬͭͮͯ͢͡ͰͱͲͳʹ͵Ͷͷ͸͹ͺͻͼͽ;Ϳ������������������������������������������������������������������������������������������������������������������������������Ρ΢ΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξο������������������������������������������������������������������������������������������������������������������������������ϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ����������������������������������������������������������������������������������������������������������������������������СТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмноп������������������������������������������������������������������������������������������������������������������������������ѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿ������������������������������������������������������������������������������������������������������������������������������ҡҢңҤҥҦҧҨҩҪҫҬҭҮүҰұҲҳҴҵҶҷҸҹҺһҼҽҾҿ������������������������������������������������������������������������������������������������������������������������������ӡӢӣӤӥӦӧӨөӪӫӬӭӮӯӰӱӲӳӴӵӶӷӸӹӺӻӼӽӾӿ������������������������������������������������������������������������������������������������������������������������������ԡԢԣԤԥԦԧԨԩԪԫԬԭԮԯ԰ԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿ������������������������������������������������������������������������������������������������������������������������������աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտ������������������������������������������������������������������������������������������������������������������������������ְֱֲֳִֵֶַָֹֺֻּֽ֢֣֤֥֦֧֪֭֮֡֨֩֫֬֯־ֿ������������������������������������������������������������������������������������������������������������������������������סעףפץצקרשת׫׬׭׮ׯװױײ׳״׵׶׷׸׹׺׻׼׽׾׿��������������������������������������������������������������������������������������������������������������������ءآأؤإئابةتثجخذرزسشصضطظعغؽؾؿ������������������������������������������������������������������������������������������������������������������١٢٤٥٦٧٨٩٪٫٬٭ٮٯٰٱٲٳٴٵٶٷٸٹٺٻټٽپٿ��������������������������������������������������������������������������������������������������������������������ڣڤڦڧڨکڪګڬڭڮگڱڲڳڴڵڶڷڸڹںڻڼڽھڿ��������������������������������������������������������������������������������������������������������������������������ۣۡۢۤۥۦۨ۩۪ۭ۫۬ۮۯ۰۱۲۳۴۵۶۷۸۹ۺۻۼ۽۾ۿ����������������������������������������������������������������������������������������������������ܡܢܣܤܥܦܧܩܪܬܭܯܱܴܷܸܹܻܼܾܰܲܵܶܺܽܿ������������������������������������������������������������������������������������������������������������������ݢݣݥݦݩݪݫݬݭݮݰݱݳݴݵݶݷݸݹݺݼݽݾݿ����������������������������������������������������������������������������������������������������������������ޡޢޣޤޥަާިީުޫެޭޮޯްޱ޲޳޴޵޶޷޸޹޺޼޽޾޿����������������������������������������������������������������������������������������������������������������������ߡߢߣߤߥߧߨߩߪ߫߬߭߮߯߱߳ߴߵ߶߷߸ߺ߼߽߾߿�������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������";
    }
    
	static public String turnCompl(){
        return "���������������}���@�����K�۰��������������������a�������������\���W�ðİŰưǰȰɰʰ˰̰ͰΰϰаѰ҉ΰ��T�ְװذٔ[�۔��ݰް߰�����C���������k�O��Ͱ��򽉰������^���r����������������������󱩱��U������������݅��ؐ�^���N��v�����������������±ñıűƱǱȱɹP�˱̱ͱή����юűӯw�]�ֱױرٱڱ۱ܱݱ�߅���H���׃����q�p��˱����M��e�T���l�I�e�P��������������K�������������������K�����������������g�������a�����������������²òĲ�ؔ�ǲȒ�ʲ˲̲ͅ��Q���M�K�N�nœ�}��زٲڲ۲ܲݎ��߂ȃԜy�Ӳ������������Ԍ����v���s�׋�p�P�a�U������L���L���c�S�������������n������������܇�������س��������m������ꐳ��r�ηQ�ǳȳɳʳ˳̑ͳ��\�г��G�ӳԳճֳ׳��t���Y�u�X�޳߳�����_��猙��ꮠ�P����I��I�����������N���z�r�������A�������|̎�������������������������J���������N�������������������b�ôĴŴ��o�ȴ��~�˴��n���[��ҏą����ִ״ش��f�۸Z�ݴ޴ߴ��������������e���_�������������������J��������������������đ�����������Q���������hʎ�n���v�����u�\�����������I�µõĵş��ǵȵɵ����̵͵εϔ��ѵҜ�Եյֵ׵صٵڵ۵��f������c���|늵�����յ�����������{����������ՙ�B�������픶��V��ӆ�G�|�������ӗ������������������������������٪��x�¶�ـ��僶Ƕȶɶʶ˶�呶Δ྄�у�ꠌ��Շ��׶��D���g�ܶݶ޶߶��Z�������艙����Z���~Ӟ�𐺶������I�����������D�����E�l�P�������y���m�����������\�C������������؜��������������������L���ŷƷǷ��w�ʷ��u�ͷΏU���M�ҷӷԷշּ����ٷڷۊ^�ݷޑ��S�S�������h�L������T�p�S���P������w������ݗ���������������������������o���������������������x�͸���������ؓ��Ӈ���D�`������ԓ�ĸ��}�w�ȸɸʸ˸̸͸��s�ж����M����䓸׸ؾV���۸ܸݸ޸߸����怸����R��������������w���t�����o�����������������������������������m��얹���ؕ���^���Ϲ�������ُ�򹼹����������¹ùĹ��M�ǹȹɹ�̹͹ιτ��ѹҹӹԹչֹ��P�ٹ��^���^�ޑT��؞��V���Ҏ���w���|܉��Ԏ��𙙹��F��݁�L��偹��������^��������������񔺨�����n�����������������������������h���������������ºú�̖�ƺǺȺɺʺ˺̺ͺκϺк��u�ӺԺպ��Q�R�ٺںۺܺݺ޺ߺ��M��a�Z����������t������������������غ������������������o���������W�A����������Ԓ�����ѻ��Ěg�h��߀���Q�����������o�»ûĻ��S�ǻȻɻʻ˻̻ͻλϻ��e�ғ]�x�ջֻ׻ؚ��ڻۻܻݻ��V�x���Z�R�M�d�Lȝ���꜆�������@����؛���������C�����e�����|�E���I�u���������O��݋�������������������D�׼����E���������������ļ�Ӌӛ�ȼ��H���^�o�μϊA�ѼҼ��v�a�Z��⛼ټڃr���{�ޚ��O�Լ�{�g�����D��}�O�z���|�|���캆�����p�]���b�`�vҊ�I������Ş���T�u�R�����������{�����Y�����v���u�����������z�������ɽ����q�C�e�_�ƽ���U�g�˽̽��I�^�нѽҽӽԽս��A�ؽٹ��۽ܽݽ޽ߝ��Y�����������]�ý��������o�\�H֔�M���x�����a���M���G���o�����L���@�������������i�o�����R���d�������Q���������m���¾þľžƎ����f�ʾ˾̾;ξϾоѾҾ��x�վ־׾��e�ھ۾ܓ��޾߾��䏾��־愡���N���������������X�Q�E�^�����x܊�������������E���������_�����P�����������������������������������������¿ÿ��w�ƚ��ȿɿʿ˿̿��n�ϿЉ����ӿԿտֿ׿ؓ��ڿۿܿݿ޿߿���ѝ������K��~�쌒������V����r̝���h�Q�������������������������U���������Ϟ�D�����R��ه�{���ڔr�@�@�m��׎���[���|���E�������������˓Ƅ������������ӝ��՘����D�������܉�����I��������������h���x�������e���Y��������������[�v�������������r�`�������zɏ�B����z�i����Ę朑ٟ����Z�����������v������Տ�����ů������|������������������ӫC���������R���[�܄C�U�����������g��������`��X�I�������������s�������������@���\��¡�Ŕn�]�Ǌ䓧�t©ª�J�R�B�]�t���u̔��´µ¶·�T¹º����¾�H���X�H���Čҿ|�]�����ʞV�G�n���\���сy���Ԓ�݆�����S�]Փ�}���_߉茻j��������j���鬔�aΛ�R�R������I���u�~�}�m�z�U�M��������֙âãäåæç؈é�^ëì�Tîïðñò�Q�Nõö÷ùú�]üý�Vÿ�����������T�������������i�͉��ϲ[���������i������Ғ�����܃����߾d�������侒��������������R����������������}�����Q�����և��ġĢģĤĥĦħĨĩĪīĬĭĮįİ�\ĲĳĴĵ��ķĸĹĺĻļĽľĿ���������ą��c���ȼ{���������������y�ғ��X���[�����H����������������M����ā������������f��������B�����������������������Q��ţŤ�o�~ē���rŪūŬŭŮůŰ��ŲųŴ�ZŶ�W�t��ź�Iż�ažſ�����������������������˱P�������������������֒����������������������r�������懊�������������������������i��������������������ơƢƣƤƥƦƧƨƩƪƫƬ�_�hƯưƱƲƳƴ�lؚƷƸƹƺ�OƼƽ�{ƿ�u�����H������������������������������������V�������ۗ������ߜD������������������Ě�R�������T���M��������������������ә��ǡǢ��Ǥ�Uǧ�w��Ǫ�tǬǭ�X�Qǰ��ǲ�\�l�qǶǷǸ����ǻǼ���N�������@���Ę��Ɔ̃S�������N���θ[�������Ӹ`�J���H�����������݌������p��A�������������Ո�c���F����������������څ�^�����|�����ȡȢ�xȤȥȦ�E��ȩȪȫȬȭȮȯ��ȱȲȳ�s�oȶ�_ȸȹȺȻȼȽȾȿ������׌���_�@�ǟ����������g���J���Ѽx�����������ؘs�������ݽq����������������������������ܛ�������J�c�����������_���w��ِ������ɢɣɤ��ɦ�}��ɩɪɫ��ɭɮɯɰ���xɳ��ɵɶɷ�Yɹɺɻɼɽ�hɿ���W���٠�������ȿ������p�������������ԟ������������۽B���d��������z��������O�������������＝���򌏋����I���B���������Kʡʢʣ���}��ʧ�{ʩ��Ԋʬʭʮʯʰ�rʲʳ�g���Rʷʸʹʺ�ʼʽʾʿ�����������ń��������m��������������ҕԇ�������؉��������ݫF�ߘ�������ݔ����������H��������������������g���������Q��������ˡˢˣˤ˥˦��˨˩˪�pˬ�lˮ˯��˱˲�˴�f�T˷�q˹˺˻˼˽˾�z�������������������Z������A�b���Ҕ\���K�������������������V�C�������m���S������q��������O�p�S�������s�����i�������������H��̧̨̣̤̥̦̩̫�B̭̮��؝�c����̴̵̶�TՄ̹̺̻̼̽�@̿�����������������������ˠC�͝��Ͻd����������ӑ�������v���`�������R���}�����w������������������������������l�������N�F���d �Nͣͤͥͦͧͨͩͪͫͬ͢͡�~ͮͯͰͱͲ�yʹ͵Ͷ�^͸͹�dͻ�Dͽ;Ϳ���������ĈF���j��͑��������������Ó�r���W�E�����������ܸD�����m�����㏝�����B���������������������f�����������W������������Ρ΢Σ�f�`Φ��ΨΩ��H�SȔήί����β��δεζηθικλμ�^ξο�l���������y�Ƿ��Ɇ������͓�΁�u�C�����P�����׆��u�����_�ݟoʏ���������������������]���F�����������`�������������������a��ϡϢϣϤϥϦϧϨϩϪϫϬϭ�uϯ��ϱϲ�ϴϵ϶��Ϲ�rϻϼݠϾ�{�b�M�B�ć��������r�w���t����e�������@�U�F�I�h���W�w�����޾���������������l����Ԕ����������������ʒ�����������N��������СТУФ�[ЦЧШЩЪϐЬ�f���yаб�{�C��ежзий�a�xмно�\�������������g���������d���������������������������ٛ����������������n�����C������̓�u����S����������������w�m܎�����������x�_ѣ�kѥѦ�WѨѩѪ��ѬѭѮԃ���ZѲѳѴӖӍ�dѸ��Ѻ�f��ѽѾѿ�����������ņ���Ӡ����鎟����}�����������������������������G�����������䏩�����V����������P��������������B�������������u���b�G�{ҦҧҨˎҪҫҬҭҮ��ҰұҲ�Ҵ�I�~ҷҸҹҺһҼ�tҾ��������U���z�ƃx��������������ρ����������ˇ�������ك|�����������������㑛�x����Ԅ�h�x�g�������[���a������������y����������[ӡӢ�ы������t��Ξ�I��ωӭ�AӯӰ�fӲӳ�ѓ�Ӷӷ�bӹӺ�xӼԁӾӿ���������ă��Ƒn�����]♪q�����������������T�����������������ݛ�������~����O��������c�Z�����Z�������������������������z���uԡԢԣ�Aԥ�S�x�YԩԪԫԬԭԮ�@�@�T�AԳԴ���hԷ�ԹԺԻ�sԽ�S������������y���E���\�N�j����������s���՞����d�����۔���ٝ�E�K��������嗗������������������؟��t���\��������ٛ��������܈��lգ��եզէը�pժ�Sլխ��կհ��ղճմ�K��ݚ��չպ���ב�վտ�`�������ď��Ɲq���Ɏ��~��Û�������������w�����������������U�H���N���@��������������ؑᘂ������\���������걠���b��������������֢���^֥֦֧֪֭֮֨֩֫֬��ֱֲֳ��ֵֶַָֹֺ�bּ��־���S�����Î������������|���̜�������������ԽK�N�[���ٱ����������a���S�����䰙�敃�E���������i�T�D����T�������������������A�T��סעף�vץצק���u�D׫ٍ׭��ׯ�b�yײ�Ѡ�׵�F׷٘���YՁ׼׽׾׿�������������Ɲ�Ɲ���Y�������������������ԝn������ۙ�ھC���v�u�����������������{��M�������������������������������ءآأؤإئا��ةتثجخذرزسشصضطظع�Gؽؾؿ�����d�Æ����ƅ��ȅ����v�I���υQ�T��ّ�������؄q�ڄ����������������������������������������������t����������������١٢٤٥٦٧٨٩٪٫٬��ٮ�zٰ������ٴٵ�Rٷٸٹٺٻټٽپٿ�����������ƃf�������˂�E�����������������������������܃L���߼e�����Z������������D�����Ж���C�L���A�������������VڤӓӏӘ֎�n�G�b�X�g�t�r�E�CԟԑԜԖԍԏՊ՟Ԃ�V�a�N�OՌՎՆ՘Ք�~�r�R�G�o�]�@�I�X�O�B�J՛փו�q�u�kֆ�v�P�S�Hח�d׏���������������������������������������������w����������ۡۢ�Pۤۥ��ۨ�i�Bۭ۫۬ۮۯ۰۱۲۳۴۵۶۷۸۹ۺ�c�J��۾ۿ���������������������^�Ј��������������ډ����݉�������������������ꈺ����N��������P��_����������ܡܢܣܤܥܦܧܩܪܬܭܯܱܴܷܸܹܻܰܲܵܶܺ�Gܾܽܿ����˞���������{�Oɐ���������r���������d���������������\��L����������ʁɜ�������������w���������C�����j�������Μ�ݢ�|�pȇݩ�P�nݬݭݮ�Wݱݳݴ�~ݶݷݸݹ�Lݼݽݾݿ���������������������������������ҿM�������������r��ʉ�����������V����������������������y��������ʚ�������`���Aޡޢޣޤޥަާިީުޫެ�Iޮޯްޱ޲޳˒޵޶޷޸޹�\޼޽޾޿���������ŊY���������������Ό��Г����������ד������������������ᓝ���������瓥���������������d���������^�������t����ߡ�X�]ߤ�xߧߨߩߪ߫߬߭߮߯�s߳�\ߵ߶߷߸ߺ�`��߾�����h���������ˇ����������Ӈ}���ׇ^���ڇ����އ������䇁����O��Z���������������K������������������������������േD�������྇��������������Ǉ����������\�����҇������������������߇���������������������������������������������Ύ�����������᪍�ᬍsᯍ�������ẎFἍ�ᾍ������������������ȎV�����͍��ύ������������������ڎp�������������������������������E���������������s��������������⣫M��⧫J���������������h��q�����A�G�N�Q�t�x�}�~���ϏT�����������������s�������ݏ[������ԑ���Y��������������������������������������Q�ÐŐ���㡐��㤑a����㪐�������㲑C�|����������㿑��������V�Z��b�h�`�Y���b���������H�D�I�R�������➖���������������윿������{�o���������T��������������䣛ќ�����䪝���䮞g�G����䴜Z���������������^���ĝ������Ȟc�ʜO�������������ҝs���������؞����۞��ݞ]�������㝧������������u�t�������񞇞z���������������������|�������������寞���������q������������������߃��ޟ��������ߊ������������������������������������������������������������������������������������������梋�����樊�檋I��歌D���泋z����������澋ȋ������������ǋ܋������̋����������������Ջ������������������z���|�A�w�~����P�U�S�K��s�\��t�~����K�J�u�q�v�w�k�������C�����U�E�I�W�{�������c�_�p�i�E�R�^�J�U�l�~�|�����D�������P���N�b�d�c�r�O�V�_�~�z�w�����i�����`�R�Q�y��������^�|����k�歇���������������������t�q�I����������������������觭a��諭���������赭���t�y�w���辘q�����������Ɨ����n�������͙��ϙ����ҙ����������ؗd�����ܙ��ޙf���ᗿ����E�昁�������u����������������������������������飘��������鬙����鳙�鶙����������������Ù��ř����ș{�����̙����������������ՙ������ٙ����ܙ_�����{�㚑�嚌���蚗�ꚛ��ܐܗ�V�_�T�W�F�]�U�Y�e�b�`�m݂�y�z�wݏ�A�O��������ꮑ�갮T��������껕�����������������������ɕ����̕��������ҕ��������������S�B�L�Oٗ�D�W�B�c�l�g�yَҗ�JҠ�]�D�M�P�U����������������������������릚��멚�������뱚��봚�뷠�������������������ȖV�F���σ�����������Ä������Ē�������T�������������������e��������Ĝ������������좚e������R�S�Z�`�j�ݞ���W���������콟����������Ɵ������ʠF��������������������������������c���������������������������������������U���������������������������������������������������������X�������������Z�a�������������˳��������Ѵ������ִ~��������������������������R�������������������������������A����������������������������������`��b��������������Q�A��{�O�S���k�j�[��^��Z��`�����X�f�g��p��C�B�G���I�D��s�B��e�t�K��z��b�A�f��x�|��P�C�|�|�@���n��H���~�S�s�Z�u�|�H��Q��K�d��U�O��|�J���}�D�U�I��k��y���\�S�M�N��O���C��h��|��j��Z�C�O�s�R��������������������������w������������F�S�d�c�����R�z�|�����v�����[�P�Z�]�O���Y�^�g�l�������_�Y�W�p�w�����������X�۰X�O����������������A����B���������������D�����������������`��a�]��d��������]���M���������������d������ў���c�O�������������������@���h�������������㰗���������������e����������������������@�R�M�W�h���D����A��A�lϊ������͘������������Ϡ�����������|����͐�u��·������ϓ�����������������������������������X��������������ϔ�������������N����������������������������������������R��������������źV�ǹa�������������������������ջe���غ`�����ܹ~����������j�D������X�뺄���������������������������f�����������[������������������Ŝ���������A���������������������U�����������u���������������������ռR����������������������������������{��������������������ڎ���������������������������������������������������������������z�O����������������ۄ�������V�����������Eۋ���]�Q�������������W�����U���������������bۘ���X�����������k�����g�������������������x���������z�����n���Z�����V���������\�����Z�e�_�f�b�l�r�p�}�w�x�������h������ׇ������Y�������������Q�|�d�V�c�T�^�n�b�q�o���\���~���������������a���N�O�E�H�K�F�T�����l�{�q�v�m�������������B�L���Z�X�V�k�����^���d�����������X�������t���y�x�������|�u����������������������������������W�����N�������������������������������t�����o���������B������";  
    }	
}