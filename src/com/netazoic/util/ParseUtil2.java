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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.pegdown.PegDownProcessor;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.netazoic.ent.ServENT.ENT_Param;



public class ParseUtil2 {
	TemplateLoader loader = null;
	Handlebars handlebars = null;

	public ParseUtil2(){

	}

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


	public void initHBS(String tPath){
		appRootPath = appRootPath.replaceAll("\\\\", "/");
		appRootPath = appRootPath.replaceAll("\\/\\/", "/");
		String fullPath = appRootPath + templatePath;
		fullPath = fullPath.replaceAll("\\/\\/", "/");
		//fullPath = fullPath.replaceAll("\\/", File.separator);
		loader = new FileTemplateLoader(fullPath);
		loader.setPrefix(fullPath);
		loader.setSuffix("");
		handlebars = new Handlebars(loader);
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

	public void parseOutput(Map<String, Object> map, String vPath) throws IOException {
		HttpServletResponse response = (HttpServletResponse) map.get(ENT_Param.response.name());
		boolean isJSP = vPath.toLowerCase().indexOf(".jsp") >= 0;
		if(isJSP) {
			parseJSP(map,vPath);
		}
		else {
			parseOutput(map,vPath,response.getWriter());
		}
	}

	public void parseOutput(Map<String, Object> map, String tPath, PrintWriter printWriter) throws IOException{
		if(handlebars == null) initHBS(tPath);
		Template template = handlebars.compile(tPath);
		String parseString =  (template.apply(map));
		printWriter.print(parseString);
	}

	public void parseJSP(Map<String,Object> map, String view) throws IOException {
		try {
			HttpServletRequest request = (HttpServletRequest) map.get(ENT_Param.request.name());
			HttpServletResponse response = (HttpServletResponse) map.get(ENT_Param.response.name());
			ServletContext context = (ServletContext) map.get(ENT_Param.context.name());
			RequestDispatcher dispatcher = context.getRequestDispatcher(view);
			dispatcher.forward(request,response);
		}catch(ServletException ex) {
			throw new IOException(ex);
		}
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

