package pacman.game.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class IO {
	public static final String DIRECTORY = "myData/";

	public static boolean saveFile(String fileName, String data, boolean append) {
		try {
			FileOutputStream outS = new FileOutputStream(DIRECTORY + fileName, append);
			PrintWriter pw = new PrintWriter(outS);

			pw.println(data);
			pw.flush();
			outS.close();

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static String loadFile(String fileName) {
		StringBuffer data = new StringBuffer();

		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(DIRECTORY + fileName)));
			String input = br.readLine();

			while (input != null) {
				if (!input.equals(""))
					data.append(input + "\n");

				input = br.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return data.toString();
	}
}