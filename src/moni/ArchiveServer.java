package moni;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ArchiveServer
 */
@WebServlet("/get/*")
public class ArchiveServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ArchiveServer() {
        super();
        // TODO Auto-generated constructor stub
    }
    

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		if (request.getParameter("recording")==null || request.getParameter("path")==null) {
			response.getWriter().flush();
			return;
		}
		String dir = request.getParameter("recording");
		String medpath = Utils.getMedDir(request.getParameter("path"));
		String baseDir = this.getServletContext().getInitParameter("bbbRecordingsDir") + "/"+medpath+"/presentation/";
		File file = new File(baseDir + dir);
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\""+dir+".zip\"");
		ServletOutputStream out = response.getOutputStream();
		//Utils.zipFile(file, file.getName(), new ZipOutputStream(out));
		Utils.pack(file.getAbsolutePath(), out);
		out.flush();
		out.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
