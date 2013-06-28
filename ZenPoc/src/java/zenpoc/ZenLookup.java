package zenpoc;

import org.apache.commons.lang3.text.StrLookup;

/**
 *
 * @author Adrian
 */
public abstract class ZenLookup extends StrLookup<String> {
	public abstract boolean lookupBoolean(String key);
}
