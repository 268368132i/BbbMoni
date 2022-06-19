package moni;

import javax.servlet.http.HttpServlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import moni.Utils.*;

/**
 * Servlet implementation class List
 */
@WebServlet("/List/*")
public class List extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static enum RecordingClass {PUBLISHED, UNPUBLISHED, DELETED};
	private static final String postfix = "presentation";
	//String dir = getInitParameter("bbbRecordingsDir");
	//String dir = "/home/ilya/ap/bigbluebutton/";
	String dir;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public List() {
        super();
        //this.getServletContext();
        //this.dir = getServletContext().getInitParameter("bbbRecordingsDir");
    }
    private RecordingClass getRecClass(String className) {
    	switch(className){
    		case "published":return RecordingClass.PUBLISHED;
    		case "unpublished": return RecordingClass.UNPUBLISHED;
    		case "deleted":return RecordingClass.DELETED;
    		default: return null;
    	}
    }

    void printRecordings(PrintWriter w, RecordingClass rc) throws ParserConfigurationException, SAXException, IOException {
    	//We need to pass recording state manually since new versions of BBB don't update it after (un)publishing
    	ArrayList<Recording> recs = Utils.getRecordings(Utils.listDirs(this.getServletContext().getInitParameter("bbbRecordingsDir"),rc,(String)null),rc);
    	int counter=0;
    	//for (Recording rec : recs) {
    	for (int i=0; i<recs.size();i++) {
    		w.print(Utils.printRecording(recs.get(i)));
    		counter++;
    	}
    	System.out.println("Printed recordings: " + counter);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.dir = this.getServletContext().getInitParameter("bbbRecordingsDir");
		// TODO Auto-generated method stub
		response.setContentType("text/html;charset=UTF-8");
		
		RecordingClass rc;
		String rcn;
		if ((rcn=request.getParameter("recs"))!=null) {
			rc=this.getRecClass(rcn);
			try {
				this.printRecordings(response.getWriter(), rc);
			} catch (ParserConfigurationException | SAXException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
