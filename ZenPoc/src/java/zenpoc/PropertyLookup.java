package zenpoc;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.logging.MDC;

/**
 *
 * @author Adrian
 */
public class PropertyLookup extends ZenLookup {

	private static final Logger LOG = Logger.getLogger(PropertyLookup.class);
	public final Map<String, Object> model = new HashMap<String, Object>();
	boolean clean = false, paragraph = false;
	Deque<String> scopeStack = new LinkedList<String>();
	Object scopeRoot = null;

	public PropertyLookup() {
		MDC.put("scope", ".");
	}

	public PropertyLookup(Map<String, Object> model) {
		this.model.putAll(model);
	}

	public void addProperty(String key, Object value) {
		model.put(key, value);
	}

	public void addProperties(Map model) {
		this.model.putAll(model);
	}

	public void setScope(String scopeRoot) {
		scopeStack.clear();
		scopeStack.addAll(Arrays.asList(scopeRoot.split(".")));
		scopeChanged();
	}

	private void scopeChanged() {
		MDC.put("scope", getScope());
		this.scopeRoot = scopeStack.isEmpty() ? null : lookupProperty(getScope());
	}

	public String getScope() {
		return StringUtils.join(scopeStack, ".");
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

	@Override
	public boolean lookupBoolean(String key) {
		String target = lookup(key);
		return target != null
				&& !target.isEmpty()
				&& !target.equals("0")
				&& !target.toLowerCase().equals("false");
	}

	public Object lookupProperty(String key) {
		String[] parts = key.split("\\.");
		Object target = null;
		boolean firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				if (hasProperty(getScope() + "." + key, false)) {
					target = lookupProperty(getScope() + "." + key);
					break;
				} else {
					target = model.get(part);
				}
			} else if (target == null) {
				break;
			} else if (target instanceof Map) {
				if (!((Map) target).containsKey(part)) {
					LOG.debug("target does not have element " + part);
				}
				target = ((Map) target).get(part);
			} else if (target instanceof List) {
				List targetList = (List) target;
				Integer index = null;
				try {
					index = Integer.parseInt(part);
				} catch (NumberFormatException numberFormatException) {
				}

				if (index == null || index < 0 || index > targetList.size() - 1) {
					LOG.debug("Invalid list index: " + index);
					target = null;
					break;
				} else {
					target = targetList.get(index);
				}
			} else if (hasField(target, part)) {
				target = getFieldValue(target, part);
			} else {
				LOG.warn(target.getClass() + " does not have property " + part);
			}
		}

		return target;
	}

	public boolean hasProperty(String key) {
		return hasProperty(key, true);
	}

	public boolean hasProperty(String key, boolean checkRoot) {
		String[] parts = key.split("\\.");
		Object target = null;
		boolean exists = true, firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				if (checkRoot && scopeRoot != null && hasField(scopeRoot, part)) {
					target = getFieldValue(scopeRoot, part);
				} else if (model.containsKey(part)) {
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
			} else if (target instanceof List) {
				List targetList = (List) target;
				Integer index = null;
				try {
					index = Integer.parseInt(part);
				} catch (NumberFormatException numberFormatException) {
				}

				if (index == null || index < 0 || index >= targetList.size()) {
					exists = false;
					break;
				} else {
					target = targetList.get(index);
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
		boolean hasField = false;
		try {
			obj.getClass().getDeclaredField(name);
			hasField = true;
		} catch (Exception e) {
			// Ignore
		}

		if (!hasField) {
			String getName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
			try {
				obj.getClass().getDeclaredMethod(getName).invoke(obj);
				hasField = true;
			} catch (Exception e) {
				// Ignore
			}
		}

		return hasField;
	}

	private Object getFieldValue(Object obj, String name) {
		Object ret = null;
		String getName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
		try {
			ret = obj.getClass().getDeclaredMethod(getName).invoke(obj);
		} catch (IllegalAccessException ex) {
		} catch (IllegalArgumentException ex) {
		} catch (InvocationTargetException ex) {
		} catch (NoSuchMethodException ex) {
		} catch (SecurityException ex) {
		}

		if (ret == null) {
			try {
				ret = obj.getClass().getDeclaredField(name).get(obj);
			} catch (Exception e) {
				// Ignore
			}
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
