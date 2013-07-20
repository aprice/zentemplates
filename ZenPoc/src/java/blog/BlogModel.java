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
				LOG.info("Added post " + p.title);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
	}

	public Post getPost(Integer id) {
		Post ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM posts WHERE id = ?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = rsToPost(rs);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
	}

	public Post getPostForShortName(String shortName) {
		Post ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM posts WHERE \"shortName\" = ?");
			ps.setString(1, shortName);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = rsToPost(rs);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
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
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
	}

	public User getUser(int userId) {
		User ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id = ?");
			ps.setInt(1, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = rsToUser(rs);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
	}

	public User getUser(String username) {
		User ret = null;
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE userName = ?");
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = rsToUser(rs);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
	}

	public void savePost(Post post) {
		try {
			if (post.id == null) {
				insertPost(post);
			} else {
				updatePost(post);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
	}

	private void insertPost(Post post) throws SQLException {
		PreparedStatement ps = db.prepareStatement(
				"INSERT INTO posts("
				+ " title, author, \"postedDate\", \"shortName\", intro, body)"
				+ " VALUES (?, ?, ?, ?, ?, ?)");
		ps.setString(1, post.title);
		ps.setInt(2, post.authorId);
		ps.setDate(3, new java.sql.Date(post.postedDate.getTime()));
		ps.setString(4, post.shortName);
		ps.setString(5, post.intro);
		ps.setString(6, post.body);
		ps.execute();
		post.id = getPostForShortName(post.shortName).id;
	}

	private void updatePost(Post post) throws SQLException {
		PreparedStatement ps = db.prepareStatement(
				"UPDATE posts"
				+ " SET title=?, author=?, \"postedDate\"=?, \"shortName\"=?, intro=?,  body=?"
				+ " WHERE id = ?");
		ps.setString(1, post.title);
		ps.setInt(2, post.authorId);
		ps.setDate(3, new java.sql.Date(post.postedDate.getTime()));
		ps.setString(4, post.shortName);
		ps.setString(5, post.intro);
		ps.setString(6, post.body);
		ps.setInt(7, post.id);
		ps.execute();
	}

	public void saveUser(User user) {
		try {
			if (user.id == null) {
				insertUser(user);
			} else {
				updateUser(user);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
	}

	private void insertUser(User user) throws SQLException {
		PreparedStatement ps = db.prepareStatement(
				"INSERT INTO users(\n"
				+ " \"userName\", password, \"fullName\")\n"
				+ " VALUES (?, ?, ?)");
		ps.setString(1, user.userName);
		ps.setString(2, user.password);
		ps.setString(3, user.fullName);
		ps.execute();
		user.id = getUser(user.userName).id;
	}

	private void updateUser(User user) throws SQLException {
		PreparedStatement ps = db.prepareStatement(
				"UPDATE users\n"
				+ " SET \"userName\"=?, password=?, \"fullName\"=?\n"
				+ " WHERE id=?");
		ps.setString(1, user.userName);
		ps.setString(2, user.password);
		ps.setString(3, user.fullName);
		ps.setInt(4, user.id);
		ps.execute();
	}

	public void saveComment(Comment comment) {
		try {
			PreparedStatement ps = db.prepareStatement(
					"INSERT INTO comments(\n"
					+ " title, body, author, post, \"postedDate\")\n"
					+ " VALUES (?, ?, ?, ?, ?, ?)");
			ps.setString(1, comment.title);
			ps.setString(1, comment.body);
			ps.setString(1, comment.author);
			ps.setInt(1, comment.post);
			ps.setDate(1, new java.sql.Date(comment.postedDate.getTime()));
			ps.execute();
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
	}

	public List<Comment> getComments(int postId) {
		List<Comment> ret = new ArrayList<Comment>();
		try {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM comments WHERE post = ? ORDER BY \"postedDate\" DESC");
			ps.setInt(1, postId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Comment c = rsToComment(rs);
				ret.add(c);
				LOG.info("Added comment " + c.title);
			}
		} catch (SQLException ex) {
			LOG.error("Failed to execute query: " + ex.getMessage(), ex);
		}
		return ret;
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

	private Comment rsToComment(ResultSet rs) throws SQLException {
		Comment ret = new Comment();
		ret.id = rs.getInt("id");
		ret.title = rs.getString("title");
		ret.body = rs.getString("body");
		ret.author = rs.getString("author");
		ret.post = rs.getInt("post");
		ret.postedDate = rs.getDate("postedDate");
		return ret;
	}
}
