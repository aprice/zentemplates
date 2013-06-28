package blog;


import java.util.Date;
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

	public User getAuthor() {
		if (author == null && authorId != null) {
			Logger.getLogger(Post.class).trace("Lazy loading author "+authorId+" not found.");
			author = BlogModel.getModel().getUser(authorId);
			if (author == null) Logger.getLogger(Post.class).debug("Author ID "+authorId+" not found.");
		}

		return author;
	}
}
