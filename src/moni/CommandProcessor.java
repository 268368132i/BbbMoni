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
			if (resp.getElementsByTagName("returncode").getLength()>0) ret = resp.getElementsByTagName("returncode").item(0).getTextContent();
			else ret="failed";
			ret=("{\"result\":\""+ret+"\"}");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret="{\"result\":\"internal error\"}";
		}
		return ret;
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.setHeader(response);
		if (request.getParameter("comm")!=null) {
			//Remote commands
			ArrayList<String> remoteCmds = new ArrayList<String>();
			remoteCmds.add("publish");
			remoteCmds.add("unpublish");
			remoteCmds.add("delete");
			
			//Local commands
			ArrayList<String> localCmds = new ArrayList<String>();
			localCmds.add("erase");
			if (remoteCmds.contains(request.getParameter("comm"))) {
				this.sendRemoteCommand(request.getParameterMap());
			} else if (localCmds.contains(request.getParameter("comm"))) {
				response.getWriter().append(
						CmdUtils.procLocalCommand(
								request.getParameterMap(), 
								this.getServletContext().getInitParameter("bbbRecordingsDir"))
						.toJson());
			}
			
		}
		response.getWriter().flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
