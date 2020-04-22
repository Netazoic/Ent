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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

public class ParseUtil {
	public ParseUtil(){}
	public static String templatePath;
	public static String appRootPath;
	Handlebars handlebars;
	
	public enum EXTENSION{
		HBS(".hbs"),
		MD(".md"),
		HTM(".htm"),
		HTML(".html"),
		HTMLS(".htmls"),
		JS(".js"),
		JSX(".jsx"),
		PDF(".pdf"),
		JPG(".jpg"),
		GIF(".gif"),
		PNG(".png"),
		SQL(".sql");

		String ext;
		EXTENSION(String e){
			ext = e;
		}
		public static EXTENSION geEXT(String tgtExt){
			for(EXTENSION Ext : EXTENSION.values()) {
				if (Ext.ext.equals(tgtExt)) return Ext;
			}
			throw new Error("Template extension not recognized: " + tgtExt);
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
		Object valObj;
		String extS;
		String fullPath;
		String tmp = parseFileMulti(tPath,settings);
		//Convert Markdown to html?

//		if(ext.equals(EXTENSION.MD.ext)){
//			PegDownProcessor pd = new PegDownProcessor();
//			tmp = pd.markdownToHtml(tmp);
//		}
		pw.print(tmp);
	}
	private static String parseFileMulti( String tPath, Map<String, Object> settings) throws Exception {
		Object valObj;
		String extS;
		String fullPath;
		String tmp = null;
		try {
			EXTENSION ext;
			fullPath = getFilePath(tPath);
			fullPath = fullPath.replaceAll("\\\\", "/");
			extS = tPath.substring(tPath.lastIndexOf("."));
			ext = EXTENSION.geEXT(extS);
			if(ext.equals(EXTENSION.SQL)) {
				// Assume sql files are hbs files
					ext = EXTENSION.HBS;
			}
			switch(ext) {
			case HBS:
				// parse handlebars
				//				TemplateLoader loader = new ClassPathTemplateLoader();
				TemplateLoader loader = new FileTemplateLoader("/templates");
				Handlebars handlebars;
				Template template = null;
				loader.setSuffix(extS);
				// Strip the extension @#$#@#
				tPath = tPath.substring(0,tPath.lastIndexOf("."));
				appRootPath = appRootPath.replaceAll("\\\\","/");
				String[] tPaths = templatePath.split(";");
				for(String tp : tPaths){
					try {
						loader.setPrefix(appRootPath + tp);
						handlebars = new Handlebars(loader);
						template = handlebars.compile(tPath);
						//If we get this far we have found our template
						break;
					}catch(IOException ex) {
						// Not found
					}
				}
				if(template!=null) {
					tmp = template.apply(settings);
				}else throw new Exception("Could not parse template: " + tPath);
				break;
			default:
				// manual parse
				byte[] encoded = Files.readAllBytes(Paths.get(fullPath));
				tmp =  StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
				String key, val,token;
				for(Map.Entry<String,Object> entry: settings.entrySet()){
					key = entry.getKey();
					valObj = entry.getValue();
					val = valObj==null?"null":valObj.toString();
					token = "\\{\\{" + key + "\\}\\}";
					tmp = tmp.replaceAll(token, val);
				}
			}		
		} catch (Exception ex) {
			throw ex;
		}
		return tmp;
	}

	public String parseHBSQuery(String path, Map<String,Object> settings) throws Exception{
		return parseFileMulti(path,settings);
	}
	
//	public String parseQueryFile(String path, Map<String,Object> settings) throws Exception{
//		File rootPath = new File(".");
//		path = getFilePath(path);
//		String q = readFile(path);
//		return parseQuery(settings, q);
//	}

	public static String parseQuery( String path,Map<String,Object> settings) throws Exception{
		return parseFileMulti(path,settings);
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
