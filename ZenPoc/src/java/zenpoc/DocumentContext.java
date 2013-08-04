package zenpoc;


import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Element;


/**
 *
 * @author Adrian
 */
public class DocumentContext implements LookupContext {
	DocumentContext parentContext;
	Element currentNode;
	ModelContext modelContext;

	@Override
	public Object getProperty(String key) {
		return modelContext.getProperty(key);
	}

	@Override
	public boolean hasProperty(String key) {
		return modelContext.hasProperty(key);
	}

	@Override
	public boolean hasProperty(String key, boolean checkRoot) {
		return modelContext.hasProperty(key, checkRoot);
	}

	@Override
	public boolean lookupBoolean(String key) {
		return modelContext.lookupBoolean(key);
	}

	public String getElementPath() {
		Element element = currentNode;
		List<String> levels = new ArrayList<String>();
		do {
			levels.add(element.tagName() + '(' + element.elementSiblingIndex() + ')');
		} while ((element = element.parent()) != null);

		StringBuilder b = new StringBuilder();
		for (int i = levels.size() - 1; i >= 0; i--) {
			b.append(levels.get(i));
			if (i > 0) {
				b.append('/');
			}
		}

		return b.toString();
	}

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