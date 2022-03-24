package moni;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import moni.BbbController.XMLParser;
import moni.Utils;

public class General extends HttpServlet {
	private String bbbServer,bbbSecret;
	private BbbController bbbController;
	private byte PRINT_RECORDINGS=0;
	public General() {
		super();		
	}
	void printHTMLHead(PrintWriter out) {
		out.println("<!DOCTYPE html>");
        out.println("<html><head>");
        out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
        out.println("<script src=\"/moni/js/base.js\"></script>");
        out.println("<link rel=\"stylesheet\" href=\"/moni/css/pure-min.css\">");
        out.println("<link rel=\"stylesheet\" href=\"/moni/css/basic.css\">");
        out.println("<title>Moni</title></head>");
        
	}
	
	protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		//request.getRequestDispatcher("index.jsp").forward(request, response);	
	}
	
	class DirectoryWithSize{
		public String dir,name,basedir, hsize;
		public Long size;
		DirectoryWithSize(String basedir, String dir, String name){
			this.size = Utils.size((new File(basedir+dir)).toPath());
			this.hsize = Utils.convertToHumanSize(size);
			this.name=name;
			this.dir = dir;
			this.basedir = basedir;
		}
	}
	String printDirSize(String dir, String tag, String name) {
		Long publishedSize = Utils.size((new File(dir)).toPath());
		return "<"+tag+">" + name + ": " + Utils.convertToHumanSize(publishedSize)+"</"+tag+">";
	}
	String printRecordingsSize() {
		return printRecordingsSize("Записи");
	}
	String printRecordingsSize(String title) {
		String out = new String();
		String dir = this.getServletContext().getInitParameter("bbbRecordingsDir");
		
		
		ArrayList<DirectoryWithSize> dirs = new ArrayList<>();
		dirs.add(new DirectoryWithSize(dir, "published","Опубликованные"));
		dirs.add(new DirectoryWithSize(dir, "deleted","Удалённые"));
		dirs.add(new DirectoryWithSize(dir, "unpublished","Скрытые"));
		dirs.add(new DirectoryWithSize(dir, "recording","В обработке и временные файлы"));
		
		out+="<div class=\"content\">";
		out+="<h3>"+title+"</h3><ul>";
		
		for (DirectoryWithSize d : dirs) {
			out+="<li class=\"smallpad\"><a class=\"pure-button\" onclick=\"loadList(this.parentNode,\'"+d.dir+"\')\")><span class=\"cspad\" name=\"" + d.dir + "\">" + d.name +"</span><span class=\"xspad\">" + d.hsize + "</span></a>";
		}
		System.out.println("test");
		/*out+="<a onclick=\"loadList(this,'published')\">" + printDirSize(dir + "published", "li", "Опубликованные</a>");
		out+=printDirSize(dir + "deleted", "li", "<a onclick=\"loadList(this,'deleted')\">В корзине</a>");
		out+=printDirSize(dir + "unpublished", "li", "<a onclick=\"loadList(this,'unpublished')\">Скрытые</a>");
		out+=printDirSize(dir + "recording", "li", "В обработке и временные файлы");*/
		out+="</ul></div>";
		return out;
	
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request,response);
		System.out.println("proc " + request.getRequestURI());
		response.setContentType("text/html; charset=UTF-8");
		
		PrintWriter out = response.getWriter();
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		
		try {
			printHTMLHead(out);
			out.println("<body>");
			long space;
			out.println("<h2>Пространство на диске</h2>");
			out.println(printRecordingsSize());
			Path drive = new File(this.getServletContext().getInitParameter("recordingDrive")).toPath();
			FileStore store = Files.getFileStore(drive);
			out.println("<h3>На диске доступно " + Utils.convertToHumanSize(store.getUsableSpace()) + " из " + Utils.convertToHumanSize(store.getTotalSpace())+"</h3>");			
			out.println("<p>Для корректной работы сервера рекомендуется не менее 4-5 Гб свободного пространства.</p>");
			
			
			
			BbbController bbbC = new BbbController(this.getServletContext().getInitParameter("bbbServer"),this.getServletContext().getInitParameter("bbbSecret"));
			ArrayList<BbbController.Meeting> meetings = bbbC.getMeetings();
			out.println("<h2>Текущие собрания " + meetings.size() + ":</h2>");
			out.println("<ul>");			
			for (BbbController.Meeting m : meetings) {
				out.println("<li>" + m.getName() + "</li>");
			}
			out.println("</ul>");
			
			
			if (getInitParameter("printRecordings").equals("true")){
			Document resp = bbbC.sendCommand("getRecordings");
			
			NodeList recs = ((Element)resp.getElementsByTagName("recordings").item(0)).getElementsByTagName("recording");		
			out.println("<h2>Recordings (" + recs.getLength() + "):</h2>");
			HashMap<Date,String[]> recsMap = new HashMap<Date,String[]>();
			out.print("<ol>");
			for (int i = 0; i<recs.getLength();i++) {
				Element rec = (Element) recs.item(i);		
				String name = rec.getElementsByTagName("name").item(0).getTextContent();
				
				Date start = new Date(Long.parseLong(rec.getElementsByTagName("startTime").item(0).getTextContent()));
				out.print("<li>" + name + " " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.forLanguageTag("ru-RU")).format(start) +"</li>");
			}
			out.print("</ol>");
			
			/*out.println("<ol>");
			for (int i=0; i<recordings.getLength(); i++) {
				Node rec = recordings.item(i);
				out.println("<li><ul>");
				NodeList props = rec.getChildNodes();
				boolean[] flags = new boolean[2];
				String[] rI = new String[]{"","","",""};
				for (int i2=0; i2<props.getLength();i2++) {
					if (flags[0]&&flags[1]) break;
					Node prop = props.item(i2);
					
					switch (prop.getNodeName()){
					case "name":
						rI[0]=prop.getTextContent();
						flags[0]=true;
						continue;
					case "playback":
						NodeList formats = prop.getChildNodes();
						for (int i3=0;i3<formats.getLength();i3++) {
							Node format = formats.item(i3);
							rI[1] += " <a href=\"" + format.getChildNodes().item(1).getTextContent() + "\">" + format.getChildNodes().item(0).getTextContent() + "</a>";
						}
						flags[1]=true;
						continue;
					case "startTime":
						rI[2] = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.forLanguageTag("ru-RU")).format(new Date( Long.parseLong(prop.getTextContent()))) + " ";
					}
					
				}
				out.println("<li>" +rI[2] + rI[0] + rI[1] + "</li>");
				out.println("</ul></li>");
			}*/
			out.println("</ol>");
			}
			
			
			
			out.println("</body></html>");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.close();
		}
	}

}
