package com.netazoic.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {

	public static File WriteInputStreamToFile(BufferedInputStream is, String fileName) throws IOException {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(fileName);

			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
			File f = new File(fileName);
			return f;
		} catch (IOException e) {
			// handle exception
			throw e;
		}
	}

}
