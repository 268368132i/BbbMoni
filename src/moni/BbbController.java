package moni;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.apache.commons.codec.digest.DigestUtils;

public class BbbController {
	public static String lastXML,defaultCharset;
	public static final boolean DEBUG_BUILD = false;
	private String server,secret;
	/**
	 * 
	 * @param bbbServer
	 * @param bbbSecret
	 */
	BbbController(String bbbServer, String bbbSecret){
		server=bbbServer;
		secret=bbbSecret;
	}


	public class HttpConnect  {
		private final String USER_AGENT ="Mozilla/5.0";
		
		public String sendReq (String request) throws IOException {
			if (DEBUG_BUILD) System.out.println("Creating url object");
			URL urlObj = new URL (request);
			if (DEBUG_BUILD) System.out.println("Opening a connection");
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);
			if (DEBUG_BUILD) System.out.println("\nSending 'GET' request to URL : " + request);
			int responseCode = con.getResponseCode();
			
			if (DEBUG_BUILD) System.out.println("Response Code : " + responseCode);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = in.readLine())!= null) {
				response.append(line);
			}
			in.close();
			if (DEBUG_BUILD) System.out.println(response.toString());
			return response.toString();
		}
	}
	
	/**
	 * 
	 * @param inputData
	 * @param salt
	 * @return
	 */
	
	 public String generateRequest (String inputData, String salt) { 		
		 String[] data = inputData.split("\\?");
		 
		 String preProc = data[0] +  (data.length>1 ? data[1]  : "") + salt;
		 if (DEBUG_BUILD) System.out.println("String to process: " + preProc);
		/* try {
			byte[] bytes = preProc.getBytes("UTF-8");
			String checksum = DigestUtils.sha1Hex(bytes);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		 return inputData + (data.length>1 ? "&" : "?") + "checksum=" + DigestUtils.sha1Hex(data[0] + (data.length>1 ? data[1] : "") + salt);
	 }
	 class XMLParser {
		 private String xml;
		 XMLParser (String XMLInput){
			 xml = XMLInput;
		 }
		 public Document getFromXML () throws Exception {
			 if (DEBUG_BUILD) System.out.println("XML response:" + xml);
			 DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			 DocumentBuilder builder = factory.newDocumentBuilder();
			 InputSource is = new InputSource(new StringReader(xml));
			 Document doc = builder.parse(is);
			 doc.normalize();
			 return doc;
		 }
		 public String[] getMeetings() throws Exception {
			 
			 NodeList meetingsNodes = this.getFromXML().getElementsByTagName("meetings").item(0).getChildNodes();
			 int meetingsNum = meetingsNodes.getLength();
			 String[] meetings = new String[meetingsNum];
			 for (int i = 0 ; i < meetingsNum; i++) {
				 NodeList properties = meetingsNodes.item(i).getChildNodes();
				 for (int i2 = 0 ; i2 < properties.getLength(); i2++) {
					 if (properties.item(i2).getNodeName().equals("meetingID")) {
						 meetings[i] = properties.item(i2).getTextContent();
					 }
				 }
			 }
			 return meetings;
		 }
		 public String getModeratorPassword(String meetingID) throws Exception {
			 NodeList meetings = this.getFromXML().getElementsByTagName("meetings").item(0).getChildNodes();
			 
			 String moderatorPassword="";
			 String checkMeeting="";
			 for (int i = 0; i< meetings.getLength(); i++) {
				 
				 NodeList props = meetings.item(i).getChildNodes();
				 
				 for (int i2 = 0; i2 < props.getLength(); i2++) {
					if (props.item(i2).getNodeName().equals("meetingID")) checkMeeting = props.item(i2).getTextContent();
					if (props.item(i2).getNodeName().equals("moderatorPW")) moderatorPassword = props.item(i2).getTextContent();					
				 }
				 if (DEBUG_BUILD) System.out.println("MeetingID:" + checkMeeting + "\t mod pass:" + moderatorPassword);
				 if (checkMeeting.equals(meetingID)) break;
				 
			 }
			 return moderatorPassword;
		 }
	 }
	 public boolean endMeeting(String server, String meetingID, String moderatorPassword, String salt)  {
		meetingID = meetingID.replaceAll(" ", "%20");
		moderatorPassword = moderatorPassword.replaceAll(" ", "%20");
		String request = server + "/bigbluebutton/api/" + generateRequest("end?meetingID="+meetingID+"&password="+moderatorPassword,salt);
		if (DEBUG_BUILD) System.out.println("End conf request:" + request);
		HttpConnect connect = new HttpConnect();
		try {
		String response = connect.sendReq(request);
		boolean success=false, commandMatch=false;
		XMLParser parser = new XMLParser(response);
		NodeList statuses = parser.getFromXML().getElementsByTagName("response").item(0).getChildNodes();
		for (int i = 0 ; i < statuses.getLength(); i++) {			
			if (statuses.item(i).getNodeName().equals("returncode") && statuses.item(i).getTextContent().equals("SUCCESS")) success = true;
			if ((statuses.item(i).getNodeName().equals("messageKey")) && (statuses.item(i).getTextContent().equals("sentEndMeetingRequest"))) commandMatch = true;
						
		}
		if (DEBUG_BUILD) System.out.println("success " + success + ",commandMatch " + commandMatch);
		if (success && commandMatch) {
			if (DEBUG_BUILD) System.out.println("endMeeting: meeting successefully ended");
			return true;
		}
		if (DEBUG_BUILD) System.out.println("endMeeting: can't end meeting");
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return false;
		
	}
	 public String generateJoinRequest(Meeting meeting, String serverURL, String salt, String fullName, boolean isModerator){		 
			String meetingID,password,convertedName;
			convertedName="";			
			meetingID = meeting.getID();
			try {
				fullName = URLEncoder.encode(fullName, "UTF-8");
				meetingID = URLEncoder.encode(meetingID, "UTF-8");
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			}
			if (isModerator) password=meeting.getModeratorPassword();
			else password=meeting.getAttendeePassword();
			String request = generateRequest("join?fullName="+fullName+"&meetingID="+meetingID+"&password="+password,salt);
			return serverURL + "/bigbluebutton/api/" + request;
	 }
	 public String generateJoinRequest(Meeting meeting, String serverURL, String salt, String fullName){
		 return this.generateJoinRequest(meeting, serverURL, salt, fullName, true);
	 }
	 
	 class Meeting {
		 String ID,name,modPassword,attendeePassword,mPassword, aPassword;
		 Node meetingXMLNode;
		 Meeting (String setID, String setName){
			 ID = setID;
			 name = setName;
		 }
		 Meeting (Node XMLNode) {
			 meetingXMLNode = XMLNode;
			 NodeList props = meetingXMLNode.getChildNodes();
			 for (int i=0; i<props.getLength(); i++){
				 Node property = props.item(i);
				 String propertyName = property.getNodeName();
				 if (propertyName.equals("meetingID")) ID = property.getTextContent();
				 if (propertyName.equals("meetingName")) name = property.getTextContent();
				 if (propertyName.equals("moderatorPW")) mPassword = property.getTextContent();
				 if (propertyName.equals("attendeePW")) aPassword = property.getTextContent();
			 }
		 }
		 public void setID (String meetingID) {
			 ID = meetingID;
		 }
		 public void setName (String meetingName) {
			 name = meetingName;
		 }
		 public void setModearatorPassword(String moderatorPassword) {
			 mPassword = moderatorPassword;
		 }
		 public void setAttendeePassword (String attendeePassword) {
			 aPassword = attendeePassword;
		 }
		 public String getID(){
			 return ID;
		 }
		 public String getName(){
			 return name;			
		 }
		 public String getModeratorPassword(){
			 return mPassword;
		 }
		 public String getAttendeePassword(){
			 return aPassword;
		 }
		 public String getProperty(String propertyName) throws Exception {
			 NodeList props = this.meetingXMLNode.getChildNodes();
			 String availPropName, propertyText;
			 
			 for (int i = 0; i < props.getLength(); i++){
				 availPropName = props.item(i).getNodeName();
				 if (availPropName.equals(propertyName)) {
					 return props.item(i).getTextContent();
				} 				 
			 }
			 throw (new Exception("property not found"));			
		 }
		 public final String toString() {
			 return this.getName();
		 }
	 }
	 class Recording implements Comparable<Recording>{
		 Date startTime,endTime;
		 String name;
		 ArrayList<String[]> playbackFormats;
		 Recording (Node recordingNode){			 
			 NodeList props = recordingNode.getChildNodes();
			 //boolean[] flags = new boolean[2];
			 String[] rI = new String[]{"","","",""};
			 for (int i2=0; i2<props.getLength();i2++) {
				 //if (flags[0]&&flags[1]) break;
				 Node prop = props.item(i2);					
				 switch (prop.getNodeName()){
				 case "name":
					 this.name = prop.getTextContent();
					 //rI[0]=prop.getTextContent();
					 //flags[0]=true;
					 continue;
				 case "playback":
					 NodeList formats = prop.getChildNodes();
					 for (int i3=0;i3<formats.getLength();i3++) {
						 Node format = formats.item(i3);
						 playbackFormats.add(new String[] {format.getChildNodes().item(0).getTextContent(),format.getChildNodes().item(1).getTextContent()});
						 //rI[1] += " <a href=\"" + format.getChildNodes().item(1).getTextContent() + "\">" + format.getChildNodes().item(0).getTextContent() + "</a>";
					 }
					 //flags[1]=true;
					 continue;
				 case "startTime":
					this.startTime = new Date( Long.parseLong(prop.getTextContent()));
					 //rI[2] = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.forLanguageTag("ru-RU")).format(new Date( Long.parseLong(prop.getTextContent()))) + " ";
					continue;
				 case "endTime":
					 this.endTime = new Date( Long.parseLong(prop.getTextContent()));

				 }
			 }
		 }
		@Override
		public int compareTo(Recording o) {
			if (this.startTime.after(o.startTime)) return 1;
			if (this.startTime.before(o.startTime))return -1;
			return 0;
		}
		 
	 }
	Document sendCommand(String command) throws Exception { 
		 
			 HttpConnect connection = new HttpConnect();
			 //BbbController controller = new BbbController();
			 if (DEBUG_BUILD) System.out.println("actionCommand: "+ command+ "\"");
			 String request = this.server + "/api/" + this.generateRequest(command, this.secret);
			 
			 try {
				 lastXML = connection.sendReq(request);					 
				 //XMLParser parser = new XMLParser(lastXML);
				 
			 } catch (Exception exception) {
				 exception.printStackTrace();

			 }
			 XMLParser parser = new XMLParser(lastXML);
			 return parser.getFromXML();
	}
	
	/*NodeList getNodes(String xml) throws Exception {
		XMLParser parser = new XMLParser(xml);
		Document data = parser.getFromXML();
		return data.getChildNodes();		
	}*/
	
	
	ArrayList<Meeting> getMeetings() throws Exception {	
		
		/*XMLParser parser = new XMLParser(sendCommand("getMeetings?bbb"));
		Document data = parser.getFromXML();
		NodeList meetingsNodes = data.getElementsByTagName("meetings").item(0).getChildNodes();	*/
		NodeList meetingsNodes = sendCommand("getMeetings?bbb").getElementsByTagName("meetings").item(0).getChildNodes();
		//NodeList meetingsNodes = sendCommand("getMeetings?bbb").item(0).getChildNodes().
		ArrayList<Meeting> meetings = new ArrayList<Meeting>();
		for (int i=0; i<meetingsNodes.getLength(); i++){
			meetings.add(new Meeting(meetingsNodes.item(i)));			 
		}
		return meetings;
	}

}
