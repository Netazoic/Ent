package com.netazoic.ent;

import com.netazoic.covid.ent.ifDataType;
import com.netazoic.ent.rdENT.DataFmt;

public interface ifDataSrc {
	
	public String getURL();

	public DataFmt getFormat();
	
	public ifDataType getDataType();
	
	public String getSrcCode();
	

}
