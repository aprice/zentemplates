package blog;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import zenpoc.TemplateRenderer;

/**
 *
 * @author Adrian
 */
public class BlogServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(BlogServlet.class);
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
		String pathInfo = request.getPathInfo();
		LOG.debug("Path info: "+pathInfo);
		String[] pathParts = (pathInfo == null || pathInfo.isEmpty())
				? new String[]{"home"}
				: pathInfo.substring(1).split("/");
		String mode = pathParts[0];
		String page = null;
		Map<String,Object> model = new HashMap<String,Object>();

		if (mode.equals("list")) {
			// Post listing

		} else if (mode.equals("view")) {
			// Full post view
			page = "view";
			Post post = null;
			if (pathParts.length > 1) {
				String postName = pathParts[1];
				post = BlogModel.getModel().getPostForShortName(postName);
			}
			if (post != null) {
				model.put("post", post);
			} else {
				response.setStatus(404);
			}
		} else if (mode.equals("edit")) {
			// Create/edit post
			page = "edit";
			Post post = null;
			if (pathParts.length > 1) {
				String postName = pathParts[1];
				post = BlogModel.getModel().getPostForShortName(postName);
			} else if (request.getParameter("id") != null) {
				post = BlogModel.getModel().getPost(Integer.valueOf(request.getParameter("id")));
			}
			if (post != null) {
				model.put("post", post);
			} else if (pathParts.length > 1 || request.getParameter("id") != null) {
				response.setStatus(404);
			}
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

		if (response.getStatus() != 200) page = String.valueOf(response.getStatus());
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
