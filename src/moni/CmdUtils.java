package moni;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import moni.List.RecordingClass;

public class CmdUtils {
	public static String getResponseJson(Map<String,String> params) {
		String resp="{";
		for (String key : params.keySet()) {
			if (resp.length()>1) resp+=",";
			resp+="\""+key+"\":\""+params.get(key)+"\"";
		}
		resp+="}";
		return resp;
	}
	public static String getCommand(Map<String,String[]> params) {
		String cmd = params.get("comm")[0];
		System.out.println("getCommand cmd=" + cmd);
		String command;
		switch(cmd) {
		case "publish":
			command = "publishRecordings?recordID="+params.get("recordings")[0]+"&publish=true";
			break;
		case "unpublish":
			command = "publishRecordings?recordID="+params.get("recordings")[0]+"&publish=false";
			break;
		case "kill":
			command = "end?meetingID="+params.get("id")[0].replace(" ", "%20")+"&password="+params.get("pwd")[0].replace(" ", "%20");
			break;
		case "delete":
			command = "deleteRecordings?recordID="+params.get("recordings")[0];
			break;
		default:
			command=null;
		}
		System.out.println("Got command:" + command);
		return command;
	}
	
	public static CommandReturn procLocalCommand(Map<String,String[]> params, String recDir) {
		String resp="";
		String cmd = params.get("comm")[0];
		boolean status=true;
		switch(cmd) {
		case "erase":
			String[] recordings = params.get("recordings")[0].split(",");
			for(String recording : recordings) {
				boolean lstat = false;
				File dir = new File(recDir + "deleted/presentation/" + recording);
				try {
					lstat = Utils.deleteNIO2(dir.toPath());
				} catch (IOException e) {
					e.printStackTrace();
					status = false;
				}
				if (!lstat) {
					resp+="Failed to delete " + recording;
					status=false;
				} else {
					resp+="Successfully deleted " + recording;
				}
			}
			return new CommandReturn(status,resp);
		case "recover":
			String[] recordings1 = params.get("recordings")[0].split(",");
			for(String recording : recordings1) {
				boolean lstat = false;
				
				File recMeta = new File(recDir + "deleted"+File.separator+"presentation" + File.separator + recording + File.separator + "metadata.xml");
				try {
					Recording rec = new Recording(recMeta);
					HashMap<String,String> mod = new HashMap<>();
					System.out.println("Backing up recording " + rec.getId());
					rec.backupMeta();
					mod.put("/recording/state[1]", "published");
					mod.put("/recording/published[1]","true");
					System.out.println("Updating parameters");
					rec.updateMetadataParamsXPath(mod);
					System.out.println("Moving recording to " + Utils.getRecPath(RecordingClass.PUBLISHED));
					rec.moveRecording(rec.getBaseDir().getAbsoluteFile() + File.separator + Utils.getRecPath(RecordingClass.PUBLISHED) + File.separator + "presentation");
					return new CommandReturn(true,"SUCCESS");
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			}
		default:
			return new CommandReturn(false,"Unknown operation");
		}	
	}

}

 abstract class Command {
	Map params;
	Command(Map<String,String> params){
		this.params = params;
	}
	abstract boolean routine();
}
 
 class CommandReturn{
	 String info;
	 boolean status;
	 CommandReturn(boolean status, String info){
		 this.status = status;
		 this.info = info;
	 }
	 CommandReturn(boolean status){
		 this.info = "N/A";
		 this.status = status;
	 }
	 public boolean getStatus() {
		 return status;
	 }
	 public String getInfo() {
		 return this.info;
	 }
	 public void setStatus(boolean st) {
		 this.status = st;
	 }
	 public void setInfo(String inf) {
		 this.info = inf;
	 }
	 public String toJson() {
		 return "{\"status\":\""+this.status +"\", \"result\":\""+this.info+"\"}";
	 }
 }
