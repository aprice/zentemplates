package blog;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jboss.logging.Logger;


/**
 *
 * @author Adrian
 */
public class Post {
	public Integer id;
	public Integer authorId;
	public Date postedDate;
	public String shortName;
	public String title;
	public String intro;
	public String body;

	private User author;
	private List<Comment> comments;

	public User getAuthor() {
		if (author == null && authorId != null) {
			Logger.getLogger(Post.class).trace("Lazy loading author "+authorId+" not found.");
			author = BlogModel.getModel().getUser(authorId);
			if (author == null) Logger.getLogger(Post.class).debug("Author ID "+authorId+" not found.");
		}

		return author;
	}

	public List<Comment> getComments() {
		List<Comment> ret = new ArrayList<Comment>();

		return ret;
	}
}
