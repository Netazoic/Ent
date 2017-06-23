package com.netazoic.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.pegdown.PegDownProcessor;

public class ParseUtil {
	public ParseUtil(){}
	public static String templatePath;
	public static String appRootPath;
	
	public enum EXTENSION{
		MD(".md"),
		HTM(".htm"),
		HTML(".html"),
		HTMLS(".htmls"),
		JS(".js"),
		JSX(".jsx"),
		PDF(".pdf"),
		JPG(".jpg"),
		GIF(".gif"),
		PNG(".png");
		
		String ext;
		EXTENSION(String e){
			ext = e;
		}
	}


	private static String getFilePath(String path) throws Exception{
		String[] tPaths = null;
		String tempPath = null;
		File f;
		boolean flgFoundIt = false;
		if(templatePath.contains(";")){
			tPaths = templatePath.split(";");
		}else{
			tPaths = new String[1];
			tPaths[0] = templatePath;
		}
		//Check for a file in the templates directories.  These may be either fully specified: c:\blh\foo\hmm
		// or relative paths:  /www/WEB-INF/templates
		try{
			if(appRootPath!=null){
				for(String tp : tPaths){
					
					String lastChar = tp.substring(tp.length()-1);
					boolean endsWith = (lastChar.equals(File.separator)) || (lastChar.equals("/"));
					tempPath = appRootPath + tp;
					if(!endsWith) tempPath +=  File.separator;
					tempPath += path;
					f = new File(tempPath);
					if(f.exists()){
						flgFoundIt=true;
						break;
					}
				}
			}
			if(!flgFoundIt){ //Try without the appRootPath -- fully specified paths in the init config
				for(String tp : tPaths){
					tempPath =  tp +  File.separator + path;
					f = new File(tempPath);
					if(f.exists()){
						flgFoundIt=true;
						break;
					}
				}
			}
		}catch(Exception ex){

		}finally{
			f = null;
		}
		if(!flgFoundIt) throw new Exception ("Could not find a template matching filename: " + path);
		return tempPath;
	}
	public static String parseFile( String path,Map<String,Object> settings) throws Exception{
		path = appRootPath + templatePath +  File.separator + path;
		String q = readFile(path);
		return parseQuery(settings, q);
	}

	public static void parseOutput(Map<String,Object> settings, String tPath, PrintWriter pw) throws Exception{
		String tmp = null;
		Object valObj;
		try {

			tPath = getFilePath(tPath);

			byte[] encoded = Files.readAllBytes(Paths.get(tPath));
			tmp =  StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
			String key, val,token;
			for(Map.Entry<String,Object> entry: settings.entrySet()){
				key = entry.getKey();
				valObj = entry.getValue();
				val = valObj==null?"null":valObj.toString();
				token = "\\{\\{" + key + "\\}\\}";
				tmp = tmp.replaceAll(token, val);
			}
		} catch (Exception ex) {
			throw ex;
		}
		//Convert Markdown to html?
		String ext = tPath.substring(tPath.lastIndexOf("."));
		if(ext.equals(EXTENSION.MD.ext)){
			PegDownProcessor pd = new PegDownProcessor();
			tmp = pd.markdownToHtml(tmp);
		}
		pw.print(tmp);
	}
	
	public String parseQueryFile(Map<String,Object> settings, String path) throws Exception{
		File rootPath = new File(".");
		path = getFilePath(path);
		String q = readFile(path);
		return parseQuery(settings, q);
	}

	public static String parseQuery( String path,Map<String,Object> settings) throws Exception{
		File rootPath = new File(".");
		path = getFilePath(path);
		String q = readFile(path);
		return parseQuery(settings, q);
	}

	public static String parseQuery(Map<String,Object> settings, String q) throws Exception{

		try {
			String key, val,token;
			Object valObj;
			for(Map.Entry<String,Object> entry: settings.entrySet()){
				key = entry.getKey();
				valObj = entry.getValue();
				val = valObj==null?"null":valObj.toString();
				val = "'" + val + "'";
				token = "\\$\\{" + key + "\\}";
				q = q.replaceAll(token, val);
			}
		} catch (Exception ex) {
			throw ex;
		}
		return q;
	}

	static String readFile(String path) throws IOException{
		Charset charset = StandardCharsets.UTF_8;
		return readFile(path,charset);
	}

	static String readFile(String path, Charset encoding) 
			throws IOException 
			{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
			}

	public static String readLargeFile(File file) throws IOException{
		return FileUtils.readFileToString(file);
	}
}
