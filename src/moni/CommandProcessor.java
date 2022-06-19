package moni;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

/**
 * Servlet implementation class CommandProcessor
 */
@WebServlet("/cmd/*")
public class CommandProcessor extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CommandProcessor() {
        super();
    }
    
    public void setHeader(HttpServletResponse response) throws IOException {
    	PrintWriter out = response.getWriter();
    	response.setContentType("application/json");
    	response.setCharacterEncoding("UTF-8");
    }
    
    public String sendRemoteCommand(Map params) {
    	String ret;
		BbbController bbbctl = new BbbController(this.getServletContext().getInitParameter("bbbServer"),this.getServletContext().getInitParameter("bbbSecret"));
		try {
			Document resp = bbbctl.sendCommand(CmdUtils.getCommand(params));
			System.out.println(resp.toString() +":"+resp.getElementsByTagName("returncode").item(0).getTextContent());
			if (resp.getElementsByTagName("returncode").getLength()>0) ret = resp.getElementsByTagName("returncode").item(0).getTextContent();
			else ret="failed";
			ret=("{\"result\":\""+ret+"\"}");
		} catch (Exception e) {
			e.printStackTrace();
			ret="{\"result\":\"internal error\"}";
		}
		return ret;
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.setHeader(response);
		System.out.println("CommandProcessor" + request.getParameter("comm"));
		//Return status json
		String retJson="{\"result\":\"invalid parameter\"}";
		if (request.getParameter("comm")!=null) {
			//Remote commands
			ArrayList<String> remoteCmds = new ArrayList<String>();
			remoteCmds.add("publish");
			remoteCmds.add("unpublish");
			remoteCmds.add("delete");
			remoteCmds.add("kill");
			//Local commands
			ArrayList<String> localCmds = new ArrayList<String>();
			localCmds.add("erase");
			localCmds.add("recover");
			if (remoteCmds.contains(request.getParameter("comm"))) {
				System.out.println("Sending remote command");
				retJson=this.sendRemoteCommand(request.getParameterMap());
			} else if (localCmds.contains(request.getParameter("comm"))) {
				retJson=CmdUtils.procLocalCommand(request.getParameterMap(), 
								this.getServletContext().getInitParameter("bbbRecordingsDir"))
						.toJson();
			}
			
		}
		response.getWriter().append(retJson);
		response.getWriter().flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
