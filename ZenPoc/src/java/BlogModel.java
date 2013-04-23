
import com.mongodb.Mongo;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Adrian
 */
public class BlogModel {

	public static BlogModel getModel() {
		return model;
	}
	private static final BlogModel model = new BlogModel();

	private static final Logger LOG = Logger.getLogger(BlogModel.class);
	private static final Mongo DB;
	static {
		try {
			DB = new Mongo();
		} catch (UnknownHostException ex) {
			LOG.fatal(ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	private BlogModel() {
	}

	public List<Post> getRecentPosts(int max) {
		throw new UnsupportedOperationException();
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


	public class Post {

		public String title, intro, body, shortName;
		public String author;		// username
		public Date postedDate;
	}

	public class Comment {

		public String title, body, author;
		public Date postedDate;
	}

	public class User {

		public String userName, fullName, password;
	}
}
