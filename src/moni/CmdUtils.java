package moni;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

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
		String command;
		switch(cmd) {
		case "publish":
			command = "publishRecordings?recordID="+params.get("recordings")[0]+"&publish=true";
			break;
		case "unpublish":
			command = "publishRecordings?recordID="+params.get("recordings")[0]+"&publish=false";
			break;			
		default:
			command=null;
		}
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
