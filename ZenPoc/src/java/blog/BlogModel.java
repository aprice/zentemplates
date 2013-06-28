package blog;


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
		return getRecentPosts(0, max);
	}

	public List<Post> getRecentPosts(int start, int max) {
		List<Post> ret = new ArrayList<Post>(max);
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM posts ORDER BY \"postedDate\" DESC OFFSET ? LIMIT ?");
			ps.setInt(1, start);
			ps.setInt(2, max);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Post p = rsToPost(rs);
				ret.add(p);
				LOG.info("Added post "+p.title);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: "+ex.getMessage(), ex);
		}
		return ret;
	}

	public Post getPostForShortName(String shortName) {
		Post ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM posts WHERE shortName = ?");
			ps.setString(1, shortName);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) ret = rsToPost(rs);
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: "+ex.getMessage(), ex);
		}
		return ret;
	}

	public int countPosts() {
		int ret = 0;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT COUNT(*) FROM posts");
			ResultSet rs = ps.executeQuery();
			rs.next();
			ret = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: "+ex.getMessage(), ex);
		}
		return ret;
	}

	public User getUser(int userId) {
		User ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id = ?");
			ps.setInt(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) ret = rsToUser(rs);
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: "+ex.getMessage(), ex);
		}
		return ret;
	}

	public User getUser(String username) {
		User ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE userName = ?");
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) ret = rsToUser(rs);
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: "+ex.getMessage(), ex);
		}
		return ret;
	}

	public void savePost(Post post) {

	}

	public void saveUser(User user) {

	}

	private Post rsToPost(ResultSet rs) throws SQLException {
		Post p = new Post();
		p.authorId = rs.getInt("author");
		p.body = rs.getString("body");
		p.id = rs.getInt("id");
		p.intro = rs.getString("intro");
		p.postedDate = rs.getDate("postedDate");
		p.shortName = rs.getString("shortName");
		p.title = rs.getString("title");
		return p;
	}

	private User rsToUser(ResultSet rs) throws SQLException {
		User ret = new User();
		ret.id = rs.getInt("id");
		ret.userName = rs.getString("userName");
		ret.password = rs.getString("password");
		ret.fullName = rs.getString("fullName");
		return ret;
	}
}
