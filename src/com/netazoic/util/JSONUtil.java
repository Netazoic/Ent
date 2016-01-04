package com.netazoic.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netazoic.ent.ENTException;

public class JSONUtil {
	public static String toJSON(Object obj) throws  ENTException{
		String json = null;
		//Jackson
		ObjectMapper jackson = new ObjectMapper();
		jackson.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		try {
			//json = jackson.writerWithView(JView.R_Std.class).writeValueAsString(obj);
			json = jackson.writer().writeValueAsString(obj);
			//json = jackson.writeValueAsString(this);
		} catch (JsonGenerationException e) {
			throw new ENTException(e);
		} catch (JsonMappingException e) {
			throw new ENTException(e);
		} catch (IOException e) {
			throw new ENTException(e);
		}
		return json;
	}
}
