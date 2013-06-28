package zenpoc;

/**
 *
 * @author Adrian
 */
public interface LookupContext {
	Object getProperty(String key);
	boolean hasProperty(String key);
	boolean hasProperty(String key, boolean checkRoot);
	boolean lookupBoolean(String key);
}
