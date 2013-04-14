
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.StrLookup;


/**
 *
 * @author Adrian
 */
public class PropertyLookup extends StrLookup<String> {
	private final Map<String, Object> model = new HashMap<String,Object>();
	boolean clean = false, paragraph = false;

	public PropertyLookup() {
	}

	public PropertyLookup(Map<String,Object> model) {
		this.model.putAll(model);
	}

	public void addProperty(String key, Object value) {
		model.put(key, value);
	}

	public void addProperties(Map model) {
		this.model.putAll(model);
	}

	public Collection<String> getPropertyNames() {
		return model.keySet();
	}

	@Override
	public String lookup(String key) {
		Object value = lookupProperty(key);
		String ret = (value == null) ? "" : value.toString();
		if (clean) {
			ret = cleanHtml(ret);
		}
		if (paragraph) {
			ret = newlineToParagraph(ret);
		}
		return ret;
	}

	public Object lookupProperty(String key) {
		String[] parts = key.split("\\.");
		Object target = null;
		boolean firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				target = model.get(part);
			} else if (target == null) {
				break;
			} else if (target instanceof Map) {
				target = ((Map)target).get(part);
			} else {
				target = getFieldValue(target, part);
			}
		}

		return target;
	}

	public boolean hasProperty(String key) {
		String[] parts = key.split("\\.");
		Object target = null;
		boolean exists = true, firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				if (model.containsKey(part)) {
					target = model.get(part);
				} else {
					exists = false;
					break;
				}
			} else if (target == null) {
				exists = false;
				break;
			} else if (target instanceof Map) {
				Map targetMap = ((Map) target);
				if (targetMap.containsKey(part)) {
					target = targetMap.get(part);
				} else {
					exists = false;
					break;
				}
			} else {
				if (hasField(target, part)) {
					target = getFieldValue(target, part);
				} else {
					exists = false;
					break;
				}
			}
		}

		return exists;
	}

	private boolean hasField(Object obj, String name) {
		Field f = null;
		try {
			f = obj.getClass().getDeclaredField(name);
		} catch (Exception e) {
			// Ignore
		}

		return (f == null);
	}

	private Object getFieldValue(Object obj, String name) {
		Object ret = null;
		try {
			ret = obj.getClass().getDeclaredField(name).get(obj);
		} catch (Exception e) {
			// Ignore
		}

		return ret;
	}

	private String cleanHtml(String html) {
		String out = html.trim().replaceAll("[ \\t]+", " ");
		return StringEscapeUtils.escapeXml(out);
	}

	private String newlineToParagraph(String text) {
		if (text.matches("[\\n\\r]")) {
			return text.replaceAll("(?!$|[\\n\\r])+([^\\n\\r]+)(?![\\n\\r]+|^)", "<p>$1</p>");
		} else {
			return text;
		}
	}
}
