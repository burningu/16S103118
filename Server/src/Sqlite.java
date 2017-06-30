import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;

public class Sqlite {
	
	private HashSet<String[]> db;
	
	public Sqlite() {
		
		sqlite_clear();
	}
	
	private void sqlite_clear() {
		
		try {
			File f = new File("files.txt");
			FileWriter fw =  new FileWriter(f);
			fw.write("");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private HashSet<String[]> sqlite_readdb() {
		
		HashSet<String[]> res = new HashSet<String[]>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("files.txt"))));
			String line = null;
			while ((line = br.readLine()) != null) {
				res.add(line.split("\t"));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	private void sqlite_writedb(HashSet<String[]> db) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("files.txt"))));
			for (String[] fileinfo : db) {
				bw.write(fileinfo[0] + "\t" + fileinfo[1] + "\t" + fileinfo[2] + "\t" + fileinfo[3] + "\n");
			}
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int sqlite_delete(String filename, String filehash, String peeraddr) {
		
		try {
			db = sqlite_readdb();
			String[] removeItem = null;
			for (String[] fileinfo : db) {
				if (fileinfo[0].equals(filename) && fileinfo[1].equals(filehash) && fileinfo[3].equals(peeraddr)) {
					removeItem = fileinfo;
				}
			}
			if (removeItem != null) {
				db.remove(removeItem);
				sqlite_writedb(db);
				return 1;
			}
			return 0;
		} catch (Exception e) {
			return 0;
		}
	}
	
	public int sqlite_delete(String peeraddr) {
		
		try {
			db = sqlite_readdb();
			HashSet<String[]> removeItem = new HashSet<String[]>();
			for (String[] fileinfo : db) {
				if (fileinfo[3].equals(peeraddr)) {
					removeItem.add(fileinfo);
				}
			}
			if (removeItem.size() != 0) {
				for (String[] item : removeItem) {
					db.remove(item);
				}
				sqlite_writedb(db);
				return 1;
			}
			return 0;
		} catch (Exception e) {
			return 0;
		}
	}
		
	public int sqlite_insert(String filename, String filehash, String f_size, String peeraddr) {
		
		try {
			String[] insertItem = {filename, filehash, f_size, peeraddr};
			db = sqlite_readdb();
			db.add(insertItem);
			sqlite_writedb(db);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}
	
	public String[] sqlite_select(String filename, String ip) {
		
		String[] res = null;
		db = sqlite_readdb();
		for (String[] fileinfo : db) {
			if (fileinfo[0].equals(filename) && !fileinfo[3].equals(ip)) {
				String[] temp = {fileinfo[3], fileinfo[2]};
				res = temp;
			}
		}
		return res;
	}
	
	public HashSet<String[]> sqlite_list(String ip) {
		
		HashSet<String[]> res = new HashSet<String[]>();
		db = sqlite_readdb();
		for (String[] fileinfo : db) {
			if (!fileinfo[3].equals(ip)) {
				String[] temp = {fileinfo[0], fileinfo[2]};
				res.add(temp);
			}
		}
		return res;
	}
}
