package zenpoc;

/**
 *
 * @author Adrian
 */
public class ContextualLookup extends ZenLookup {
	DocumentContext context;

	public ContextualLookup(DocumentContext context) {
		this.context = context;
	}

	public void setContext(DocumentContext context) {
		this.context = context;
	}

	@Override
	public String lookup(String key) {
		Object prop = context.getProperty(key);

		return (prop == null) ? "" : prop.toString();
	}

	@Override
	public boolean lookupBoolean(String key) {
		return context.lookupBoolean(key);
	}
}
