package aditazz.poc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {
	public static void createFile() throws IOException {
		 File f=new File("/abc.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(f)))
		{
		    writer.write("Hello World !!");
		    System.out.println(f.getAbsolutePath());
		}catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}
}
