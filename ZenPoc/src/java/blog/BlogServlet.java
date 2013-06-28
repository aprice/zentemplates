package blog;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import zenpoc.TemplateRenderer;

/**
 *
 * @author Adrian
 */
public class BlogServlet extends HttpServlet {

	/**
	 * Processes requests for both HTTP
	 * <code>GET</code> and
	 * <code>POST</code> methods.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String mode = request.getPathInfo();
		mode = (mode == null || mode.isEmpty()) ? "home" : mode.split("/")[0];
		String page = mode;
		Map<String,Object> model = new HashMap<String,Object>();

		if (mode.equals("list")) {
			// Post listing

		} else if (mode.equals("view")) {
			// Full post view

		} else if (mode.equals("edit")) {
			// Create/edit post

		} else if (mode.equals("comment")) {
			// Add comment

		} else if (mode.equals("login")) {
			// Admin authentication

		} else {
			// Homepage
			page = "home";

			// Get list of homepage articles
			BlogModel blogMo = BlogModel.getModel();
			model.put("posts", blogMo.getRecentPosts(5));
		}

		TemplateRenderer r = new TemplateRenderer("WEB-INF/blog/"+page+".html", getServletContext());
		r.addProperties(model);
		r.render(response);
	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP
	 * <code>GET</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP
	 * <code>POST</code> method.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>
}
