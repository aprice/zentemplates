
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;


/**
 *
 * @author Adrian
 */
public class ModelContext {
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

	Object getProperty(String key) {
		String[] parts = key.split("\\.");
		Object target = currentNode;
		boolean firstPart = true;
		for (String part : parts) {
			if (firstPart) {
				firstPart = false;
				if (currentNode != null && hasField(currentNode, part)) {
					target = getFieldValue(currentNode, part);
				} else if (parentContext != null && parentContext.hasProperty(key)) {
					target = parentContext.getProperty(part);
				} else {
					target = null;
				}
			} else if (target == null) {
				break;
			} else if (target instanceof Map) {
				if (!((Map)target).containsKey(part)) {
					LOG.debug("target does not have element "+part);
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
					LOG.debug("Invalid list index: "+index);
					target = null;
					break;
				} else {
					target = targetList.get(index);
				}
			} else if (hasField(target, part)) {
				target = getFieldValue(target, part);
			} else {
				LOG.warn(target.getClass()+" does not have property "+part);
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

	private boolean hasField(Object obj, String name) {
		if (obj instanceof Map) {
			if (((Map) obj).containsKey(name)) return true;
		}

		Field f = null;
		try {
			f = obj.getClass().getDeclaredField(name);
		} catch (Exception e) {
			// Ignore
		}

		return (f != null);
	}

	private Object getFieldValue(Object obj, String name) {
		if (obj instanceof Map && ((Map) obj).containsKey(name)) {
			return ((Map) obj).get(name);
		}

		Object ret = null;
		try {
			ret = obj.getClass().getDeclaredField(name).get(obj);
		} catch (Exception e) {
			// Ignore
		}

		return ret;
	}
}