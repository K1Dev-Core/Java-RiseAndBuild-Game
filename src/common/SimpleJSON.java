package common;

import java.util.*;

public class SimpleJSON {

    public static class JSONObject {
        private Map<String, Object> data = new HashMap<>();

        public void put(String key, Object value) {
            data.put(key, value);
        }

        public Object get(String key) {
            return data.get(key);
        }

        public int getInt(String key) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return 0;
        }

        public String getString(String key) {
            Object value = data.get(key);
            return value != null ? value.toString() : null;
        }

        public boolean getBoolean(String key) {
            Object value = data.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return false;
        }

        public JSONArray getJSONArray(String key) {
            Object value = data.get(key);
            if (value instanceof JSONArray) {
                return (JSONArray) value;
            }
            return new JSONArray();
        }

        public Set<String> keySet() {
            return data.keySet();
        }
    }

    public static class JSONArray {
        private List<Object> data = new ArrayList<>();

        public void add(Object value) {
            data.add(value);
        }

        public Object get(int index) {
            return data.get(index);
        }

        public int length() {
            return data.size();
        }

        public JSONObject getJSONObject(int index) {
            Object value = data.get(index);
            if (value instanceof JSONObject) {
                return (JSONObject) value;
            }
            return new JSONObject();
        }
    }

    public static JSONObject parseObject(String json) {
        JSONObject result = new JSONObject();
        json = json.trim();

        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        // เอา { และ } ออก
        json = json.substring(1, json.length() - 1);

        // แยก key-value pairs
        List<String> pairs = splitJsonPairs(json);

        for (String pair : pairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim().replaceAll("\"", "");
                String value = parts[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // String value
                    result.put(key, value.substring(1, value.length() - 1));
                } else if (value.equals("true")) {
                    // Boolean true
                    result.put(key, true);
                } else if (value.equals("false")) {
                    // Boolean false
                    result.put(key, false);
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    // Array value
                    result.put(key, parseArray(value));
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    // Object value
                    result.put(key, parseObject(value));
                } else {
                    // Number value
                    try {
                        int intValue = Integer.parseInt(value);
                        result.put(key, intValue);
                    } catch (NumberFormatException e) {
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }

    public static JSONArray parseArray(String json) {
        JSONArray result = new JSONArray();
        json = json.trim();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        // เอา [ และ ] ออก
        json = json.substring(1, json.length() - 1);

        // แยก elements
        List<String> elements = splitJsonElements(json);

        for (String element : elements) {
            element = element.trim();

            if (element.startsWith("\"") && element.endsWith("\"")) {
                // String element
                result.add(element.substring(1, element.length() - 1));
            } else if (element.equals("true")) {
                // Boolean true
                result.add(true);
            } else if (element.equals("false")) {
                // Boolean false
                result.add(false);
            } else if (element.startsWith("[") && element.endsWith("]")) {
                // Array element
                result.add(parseArray(element));
            } else if (element.startsWith("{") && element.endsWith("}")) {
                // Object element
                result.add(parseObject(element));
            } else {
                // Number element
                try {
                    int intValue = Integer.parseInt(element);
                    result.add(intValue);
                } catch (NumberFormatException e) {
                    result.add(element);
                }
            }
        }

        return result;
    }

    private static List<String> splitJsonPairs(String json) {
        List<String> pairs = new ArrayList<>();
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();

        for (char c : json.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }

            if (c == '"') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                } else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    pairs.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        return pairs;
    }

    private static List<String> splitJsonElements(String json) {
        List<String> elements = new ArrayList<>();
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();

        for (char c : json.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }

            if (c == '"') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                } else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    elements.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            elements.add(current.toString());
        }

        return elements;
    }
}
