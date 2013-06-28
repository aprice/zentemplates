package zenpoc;


import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;


/**
 *
 * @author Adrian
 */
public class ModelContext implements LookupContext {
	private static final Logger LOG = Logger.getLogger(ModelContext.class);
	ModelContext parentContext = null;
	String modelPath = "";
	Object currentNode;

	public ModelContext(ModelContext parent, String path) {
		parentContext = parent;
		modelPath = parent.modelPath.isEmpty() ? path : parent.modelPath + '.' + path;
		currentNode = parent.getProperty(path);
	}

	public ModelContext(Object rootNode) {
		currentNode = rootNode;
	}

	@Override
	public Object getProperty(String key) {
		String[] parts = key.split("\\.");
		LOG.trace(key + " has " + parts.length + " parts");
		Object target = currentNode;
		boolean firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
			}
			if (firstPart) {
				firstPart = false;
				if (currentNode != null && hasField(currentNode, part)) {
					target = getFieldValue(currentNode, part);
				} else if (parentContext != null && parentContext.hasProperty(key)) {
					target = parentContext.getProperty(part);
				} else {
					LOG.info("Could not locate "+part);
					target = null;
				}
			} else if (target == null) {
				LOG.info("Could not locate "+part);
				break;
			} else if (target instanceof Map) {
				if (!((Map)target).containsKey(part)) {
					LOG.info("target does not have element "+part);
				}
				target = ((Map)target).get(part);
			} else if (target instanceof List) {
				List targetList = (List) target;
				Integer index = null;
				try {
					index = Integer.parseInt(part);
				} catch (NumberFormatException numberFormatException) {
				}

				if (index == null || index < 0 || index > targetList.size() - 1) {
					LOG.info("Invalid list index: "+index);
					target = null;
					break;
				} else {
					target = targetList.get(index);
				}
			} else if (hasField(target, part)) {
				target = getFieldValue(target, part);
			} else {
				LOG.info(target.getClass()+" does not have property "+part);
			}
		}

		return target;
	}

	@Override
	public boolean hasProperty(String key) {
		return hasProperty(key, true);
	}

	@Override
	public boolean hasProperty(String key, boolean checkRoot) {
		String[] parts = key.split("\\.");
		Object target = null;
		boolean exists = true, firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				if (currentNode != null && hasField(currentNode, part)) {
					target = getFieldValue(currentNode, part);
				} else if (checkRoot && parentContext != null && parentContext.hasProperty(key)) {
					target = parentContext.getProperty(part);
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

	@Override
	public boolean lookupBoolean(String key) {
		// TODO: Inversion
		// TODO: Comparison operators
		boolean invert = key.charAt(0) == '!';
		boolean ret;

		Object target = getProperty(key);
		if (target == null) {
			ret = false;
		} else if (target instanceof String) {
			ret = ((String)target).isEmpty() || target.equals("false");
		} else if (target instanceof Number) {
			ret = target.equals(0);
		} else if (target instanceof Boolean) {
			ret = (Boolean)target;
		} else {
			ret = true;
		}

		return ret ^ invert;
	}

	private boolean hasField(Object obj, String name) {
		if (obj instanceof Map) {
			if (((Map) obj).containsKey(name)) return true;
		}

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
		if (obj instanceof Map && ((Map) obj).containsKey(name)) {
			return ((Map) obj).get(name);
		}

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
}