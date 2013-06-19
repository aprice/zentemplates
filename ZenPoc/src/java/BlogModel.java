
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Adrian
 */
public class BlogModel {
	private static final Logger LOG = Logger.getLogger(BlogModel.class);

	public static BlogModel getModel() {
		return model;
	}
	private static final BlogModel model = new BlogModel();

	private static final String DSN = "jdbc:postgresql:blog?user=blog&password=blog";
	private static final String USER = "blog";
	private static final String PASS = "blog";

	private Connection db;

	private BlogModel() {
		try {
			Class.forName("org.postgresql.Driver");
			db = DriverManager.getConnection(DSN);//, USER, PASS);
		} catch (ClassNotFoundException ex) {
			LOG.error("Unable to load PostgreSQL driver.", ex);
		} catch (SQLException ex) {
			LOG.error("Unable to connect to database.", ex);
		}
	}

	public List<Post> getRecentPosts(int max) {
		List<Post> ret = new ArrayList<Post>(max);
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM posts ORDER BY \"postedDate\" DESC LIMIT ?");
			ps.setInt(1, max);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Post p = new Post();
				p.author = rs.getInt("author");
				p.body = rs.getString("body");
				p.id = rs.getInt("id");
				p.intro = rs.getString("intro");
				p.postedDate = rs.getDate("postedDate");
				p.shortName = rs.getString("shortName");
				p.title = rs.getString("title");
				ret.add(p);
				LOG.info("Added post "+p.title);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query.", ex);
		}
		return ret;
	}

	public List<Post> getRecentPosts(int start, int max) {
		throw new UnsupportedOperationException();
	}

	public int countPosts() {
		throw new UnsupportedOperationException();
	}

	public User getUser(String username) {
		throw new UnsupportedOperationException();
	}

	public void savePost(Post post) {

	}

	public void saveUser(User user) {

	}
}
