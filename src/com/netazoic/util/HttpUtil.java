package com.netazoic.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtil {

	public static HttpURLConnection getRemoteHTTPConn(String remoteURL,   Boolean flgDebug)
			throws Exception, MalformedURLException, IOException,
			ProtocolException {
		boolean flgHTTPS = false;
		//String[] urlParts = remoteURL.split("\\?");
		//remoteURL = urlParts[0] + "?" + encodeValue(urlParts[1]);
		remoteURL = remoteURL.trim();
		remoteURL = remoteURL.replaceAll("\\n", "");
		remoteURL = remoteURL.replaceAll("\\r", "");
		remoteURL = remoteURL.replace("&pun=", "&pUserName=");
		remoteURL = remoteURL.replace("&ppw=", "&pPassword=");

		URL url = new URL(remoteURL);
		HttpURLConnection http = null;
		if(remoteURL.indexOf("https") >= 0){
			flgHTTPS = true;
		}
		if(flgHTTPS){
			http = (HttpsURLConnection) url.openConnection();
			if(flgDebug) disableSSLVerification();
		}
		else http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("GET");
		http.setDoOutput(true);
		if(flgDebug){

			//System.out.print(getResponseString(http));
		}
		//Check the return status
		int responseCode = http.getResponseCode();
		if(responseCode == 301 || responseCode == 302){
			//Got a redirect.  Try the https version of this url
			if(!flgHTTPS){
				remoteURL = remoteURL.replace("http", "https");
				url = new URL(remoteURL);
				http = (HttpsURLConnection) url.openConnection();
				responseCode = http.getResponseCode();
			}
			if(responseCode!=200){
				throw new Exception("Could not connect to remote server to retrieve remote query\nResponse Code: " + responseCode);
			}
		}
		//InputStream is = http.getInputStream();
		//return is;
		return http;
	}

	public static void disableSSLVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {

				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				@Override
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] certs, String authType)
								throws CertificateException {
					// TODO Auto-generated method stub

				}
				@Override
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] certs, String authType)
								throws CertificateException {
					// TODO Auto-generated method stub

				}
			}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}

			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

	public static String getResponseString(HttpURLConnection conn)
			throws IOException {
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		// retrieve the response from server
		InputStream is = null;
		try {
			is = conn.getInputStream();
			int ch;
			StringBuffer sb = new StringBuffer();
			while ((ch = is.read()) != -1) {
				sb.append((char) ch);
			}
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public static String getResponseString(InputStream is)
			throws IOException {
		// retrieve the response from server
		try {
			int ch;
			StringBuffer sb = new StringBuffer();
			while ((ch = is.read()) != -1) {
				sb.append((char) ch);
			}
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {

		}
	}


}
