
import org.jsoup.nodes.Element;


/**
 *
 * @author Adrian
 */
public class DocumentContext {
	DocumentContext parentContext;
	Element currentNode;
	ModelContext modelContext;

	public DocumentContext(Element node, ModelContext model) {
		currentNode = node;
		modelContext = model;
		parentContext = null;
	}

	public DocumentContext(DocumentContext parent, Element node, ModelContext model) {
		this(node, model);
		parentContext = parent;
	}
}