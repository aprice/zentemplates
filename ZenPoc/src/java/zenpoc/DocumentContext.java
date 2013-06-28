package zenpoc;


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