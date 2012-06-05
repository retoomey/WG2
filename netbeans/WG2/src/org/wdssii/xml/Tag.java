package org.wdssii.xml;

import java.io.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.file.FileURLConnection;

/**
 * Tag is an html Tag object that handles a Stax parsing stream. Even though we
 * create a tree, it's our tree so we have some perks like being able to
 * directly stores values as proper field types, etc. Also I'm hoping to
 * eventually have the rest of display doing stuff like creating radials or
 * contours AS THEY LOAD...lol. This basically halves the memory usage vs using
 * a full Sax document instead.
 *
 * FIXME: bug where if duplicate tag occurs within an unhandled child then it
 * will overwrite the tag.
 *
 * This isn't as fast as raw STAX, the advantage is that we can define new tags
 * very quickly and easily.
 *
 * @author Robert Toomey
 */
public abstract class Tag {

	private static Logger log = LoggerFactory.getLogger(Tag.class);
	private String cacheTagName;
	private boolean haveTag = false;
	public static final String prefix = "";
	/**
	 * Set to true to process children of this tag
	 */
	private boolean processChildren = true;
	/**
	 * Set to true iff tag was found and processed
	 */
	private boolean processedTag = false;
	/**
	 * The text between tags, if any... <tag>the text</tag>
	 */
	private String text = ""; // Important to be "" since we append to it
	/**
	 * ArrayLists are done by reflection in the form: <tag> <item> <item>
	 * </tag> with a field of the form: public ArrayList<Tag_item> items;
	 * Typically these repeat in a block, so we cache the array to speed up
	 * the reflection. So if you have ArrayLists of subtags, you want to
	 * group them in the xml file as much as possible for speed.
	 */
	private ArrayList<Object> lastArray;
	private String lastArrayName;
	private Class<? extends Tag> lastClass;
	private int lastCounter = 0;
	Field[] fields;

	public String getText() {
		return text;
	}

	/**
	 * Release text. This is done by default by validateTag to save memory,
	 * I typically don't use text between tags. If you do, override
	 * validateTag for tags you want to keep text for.
	 */
	public void releaseText() {
		text = null;
	}

	/*
	 * Default tag method returns the part of the classname without the
	 * "Tag_" part. This is why this class is abstract.
	 */
	public final String tag() {
		if (!haveTag) {
			Class<?> c = this.getClass();
			String s = c.getSimpleName();
			s = s.replaceAll("Tag_", "");
			// FIXME: how to check for errors here?
			cacheTagName = s;
			haveTag = true;
		}
		return cacheTagName;
	}

	/**
	 * Return true iff tag was read from xml
	 */
	public boolean wasRead() {
		return processedTag;
	}

	public void setProcessChildren(boolean flag) {
		processChildren = flag;
	}

	/**
	 * Utility function to check for a new start tag
	 */
	protected static String haveStartTag(XMLStreamReader p) {
		String startTag = null;
		if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
			startTag = p.getLocalName();
		}
		return startTag;
	}

	/**
	 * Utility function to check for a new start tag
	 */
	protected boolean isStartTag(XMLStreamReader p) {
		boolean haveStart = false;
		if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
			haveStart = true;
		}
		return haveStart;
	}

	protected boolean atStart(XMLStreamReader p) {
		boolean atStart = false;
		if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
			String startTag = p.getLocalName();
			if (startTag.equals(tag())) {
				atStart = true;
			}
		}
		return atStart;
	}

	protected static boolean atStart(XMLStreamReader p, String tag) {
		boolean atStart = false;
		if (p.getEventType() == XMLStreamConstants.START_ELEMENT) {
			String startTag = p.getLocalName();
			if (startTag.equals(tag)) {
				atStart = true;
			}
		}
		return atStart;
	}

	protected boolean atEnd(XMLStreamReader p) {
		boolean atEnd = false;
		if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
			String endTag = p.getLocalName();
			if (endTag.equals(tag())) {
				atEnd = true;
			}
		}
		return atEnd;
	}

	/**
	 * Utility function to check for end tag
	 */
	protected static boolean isEndTag(XMLStreamReader p, String end) {
		boolean isEndTag = false;
		if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
			String name = p.getLocalName();
			if (end.equals(name)) {
				isEndTag = true;
			}
		}
		return isEndTag;
	}

	protected boolean nextNotEnd(XMLStreamReader p) {

		// Move forward a tag....
		boolean end = false;
		try {
			if (p.hasNext()) {  // If we still more stuff...
				p.next();   // Move forward

				if (p.getEventType() == XMLStreamConstants.END_ELEMENT) {
					String endTag = p.getLocalName();
					if (endTag.equals(tag())) {
						end = true;
					}
				}

			}
		} catch (XMLStreamException ex) {
			// what to do?, just end so we don't loop forever
			end = true;
		}
		return !end;

	}

	/**
	 * Holder class for unit/value attributes
	 */
	public static class UnitValuePair {

		public String unit;
		public String value;
	}

	protected static void readUnitValue(XMLStreamReader p, UnitValuePair buffer) {
		int count = p.getAttributeCount();
		buffer.unit = null;
		buffer.value = null;
		for (int i = 0; i < count; i++) {
			QName attribute = p.getAttributeName(i);
			String name = attribute.toString();
			String value = p.getAttributeValue(i);
			if ("units".equals(name)) {
				buffer.unit = value;
			} else if ("value".equals(name)) {
				buffer.value = value;
			}
		}
	}

	protected static void processAttributes(XMLStreamReader p, Map<String, String> buffer) {
		int count = p.getAttributeCount();
		for (int i = 0; i < count; i++) {
			QName attribute = p.getAttributeName(i);
			String name = attribute.toString();
			String value = p.getAttributeValue(i);
			buffer.put(name, value);
		}
	}

	/**
	 * In the current stream and tag, find any attribute pairs and keep them
	 */
	public void handleAttributes(XMLStreamReader p) {
		int count = p.getAttributeCount();
		for (int i = 0; i < count; i++) {
			QName attribute = p.getAttributeName(i);
			String name = attribute.toString();
			String value = p.getAttributeValue(i);
			handleAttribute(name, value);
		}
	}

	/**
	 * Process our root tag returned by tag()
	 */
	public boolean processTag(XMLStreamReader p) {

		boolean foundIt = false;
		if (atStart(p)) {  // We have to have our root tag
			foundIt = true;
			handleAttributes(p);

			// Note that processChildren false means only the start tag and
			// attributes are processed, any text, etc. will be ignored.
			// <item t=1> ...... rest ignored--> hello there</item>
			if (processChildren) {
				while (nextNotEnd(p)) { // While not at end tag

					// Check for text data...note since this is a stream parser
					// we will get ANY text between our tags if no child tag
					// handles it.
					if (p.getEventType() == XMLStreamConstants.CHARACTERS) {
						int start = p.getTextStart();
						int length = p.getTextLength();
						text += new String(p.getTextCharacters(),
							start,
							length);
					}
					processChildren(p);
				}
			}
			processedTag = true;
			validateTag();
			if (lastCounter > 0) {
				log.debug("Tag " + this.tag() + " array " + lastArrayName + " had " + lastCounter + " hits************");
			}
			cleanUp();
		}
		return foundIt;
	}

	public void validateTag() {
		releaseText();
	}

	/**
	 * Free up memory since we might hold onto tags for a while
	 */
	public void cleanUp() {
		lastArrayName = null;
		lastArray = null;
		lastClass = null;
		fields = null;
	}

	/**
	 * Process this tag as a document root. Basically skip any information
	 * until we get to our tag. In STAX, the first event is not a start tag
	 * typically.
	 *
	 * @param p the stream to read from
	 * @return true if tag was found and processed
	 */
	public boolean processAsRoot(XMLStreamReader p) {
		boolean found = false;
		try {
			while (p.hasNext()) {
				int event = p.next();
				switch (event) {
					case XMLStreamConstants.START_ELEMENT: {
						found = processTag(p);
						break;
					}
				}
			}
		} catch (XMLStreamException ex) {
		}
		return found;
	}

	/**
	 * Process just this tag and STOP. Normally don't do this. GUI uses this
	 * to process a 'header' of a URL/file to gather info. The tag must
	 * match the given.
	 */
	public boolean processOneAndStop(XMLStreamReader p) {
		boolean found = false;
		boolean done = false;
		try {
			while (!done && p.hasNext()) {
				int event = p.next();
				switch (event) {
					case XMLStreamConstants.START_ELEMENT: {
						found = processTag(p);
						done = true;// even if not the tag we wanted....
						break;
					}
				}
			}
		} catch (XMLStreamException ex) {
		}
		return found;
	}

	/**
	 * Process document root from a given File
	 */
	public boolean processAsRoot(File f) {
		boolean success;
		try {
			URL fURL = f.toURI().toURL();
			success = processAsRoot(fURL);
		} catch (MalformedURLException ex) {
			return false;
		}
		return success;
	}

	/**
	 * Process document root from a given URL
	 */
	public boolean processAsRoot(URL aURL) {
		boolean success = false;
		if (aURL != null) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			try {
				URLConnection urlConnection = aURL.openConnection();
				InputStream is = urlConnection.getInputStream();
				if (aURL.toString().contains(".gz")) {  // simple hack
					is = new GZIPInputStream(is);
				}
				BufferedReader in = new BufferedReader(
					new InputStreamReader(
					is));
				XMLStreamReader p = factory.createXMLStreamReader(in);
				success = processAsRoot(p);


			} catch (Exception ex) {
			}
		}
		return success;

	}

	/**
	 * Process all child tabs within our tag
	 */
	public void processChildren(XMLStreamReader p) {

		// Default snags Tags and ArrayList<Tags>
		fillTagFieldsFromReflection(p);
		fillArrayListFieldsFromReflection(p);
	}

	/**
	 * A getDeclaredField that caches the fields of our tag, this is faster
	 * than the internal methods that copy all fields each time
	 * getDeclaredField is called
	 *
	 * @param name
	 * @return
	 */
	private Field getDeclaredField(String name) {

		// Reflection COPIES fields on getDeclaredField, so we cache them
		// for speed.
		if (fields == null) {
			Class<?> c = this.getClass();
			fields = c.getFields();
		}

		// Copied from Field private method for searching.
		// Not sure if intern does more harm than good here....
		//String internedName = name.intern();
		for (int i = 0; i < fields.length; i++) {
			//if (fields[i].getName() == internedName) {
			if (fields[i].getName().equals(name)) {
				return fields[i];
			}
		}
		return null;
	}

	/**
	 * Handle attributes by reflection. It looks for a matching field name
	 * exactly matching the xml attribute tag. The type of the field is used
	 * to parse the xml string.
	 *
	 * @param n
	 * @param value
	 */
	public void handleAttribute(String name, String value) {

		Field f = getDeclaredField(name);
		if (f != null) {
			parseFieldString(f, value);
		}
	}

	/**
	 * Parse a field and value from reflection. Return true if handled.
	 * Subclasses can override to add more types if needed. We handle int,
	 * boolean, float and string by default
	 *
	 * @param f theField we are to set
	 * @param value the string of the xml text to parse
	 * @return true if we handled it
	 */
	public boolean parseFieldString(Field f, String value) {
		boolean handled = false;
		try {
			String theType = f.getType().getName();

			// ---------------------------------------------------------------
			// Handle 'boolean' field type
			// <tag fieldBoolean={yes, no, 1, no }
			if (theType.equals("boolean")) {
				boolean flag = false;
				if (value.equalsIgnoreCase("yes")) {
					flag = true;
				}
				if (value.equals("1")) {
					flag = true;
				}
				f.setBoolean(this, flag);
				handled = true;
				// ---------------------------------------------------------------
				// Handle 'int' field type
				// <tag fieldInteger={0xHex, number }
			} else if (theType.equals("int")) {
				try {
					int anInt = 0;
					// Handle '0x' as hex number....
					if (value.toLowerCase().startsWith("0x")) {
						value = value.substring(2);
						anInt = Integer.parseInt(value, 16);
					} else {
						anInt = Integer.parseInt(value);
					}
					f.setInt(this, anInt);
					handled = true;
				} catch (NumberFormatException e) {
					// Could warn....
				}

				// ---------------------------------------------------------------
				// Handle 'float' field type
				// <tag fieldInteger={+-infinity, +-inf, float
			} else if (theType.equals("float")) {

				try {
					float aFloat = Float.NaN;
					if (value.equalsIgnoreCase("infinity")) {
						aFloat = Float.POSITIVE_INFINITY;
					} else if (value.equalsIgnoreCase("-infinity")) {
						aFloat = Float.NEGATIVE_INFINITY;
					} else {
						aFloat = Float.parseFloat(value);
					}
					f.setFloat(this, aFloat);
					handled = true;
				} catch (NumberFormatException e) {
					// Could warn....
				}

				// ---------------------------------------------------------------
				// Handle 'double' field type
				// <tag fieldInteger={+-infinity, +-inf, float
			} else if (theType.equals("double")) {
				try {
					double aDouble = Double.NaN;
					if (value.equalsIgnoreCase("infinity")) {
						aDouble = Double.POSITIVE_INFINITY;
					} else if (value.equalsIgnoreCase("-infinity")) {
						aDouble = Double.NEGATIVE_INFINITY;
					} else {
						aDouble = Double.parseDouble(value);
					}
					f.setDouble(this, aDouble);
					handled = true;
				} catch (NumberFormatException e) {
					// Could warn....
				}
				// ---------------------------------------------------------------
				// Handle 'string' field type (which is just the xml text)
				// <tag fieldInteger=xmltext
			} else {
				f.set(this, value);
				handled = true;
			}

		} catch (IllegalAccessException e) {
			// FIXME: notify programmer of bad access
		}
		return handled;
	}

	/**
	 * Throws everything but kitchen sink....create a sub tag from given
	 * class and call processTag on it
	 *
	 * @param p the stream
	 * @param m the class maker
	 * @return new Tag
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	public Tag createAndProcessSubTag(XMLStreamReader p, Class<? extends Tag> m)
		throws InstantiationException, IllegalAccessException,
		NoSuchMethodException, InvocationTargetException {
		Class<?>[] argTypes = new Class[]{XMLStreamReader.class};
		Object[] args = new Object[]{p}; // Actual args
		Tag classInstance = m.newInstance();
		Method aMethod = m.getMethod("processTag", argTypes);
		Object result = aMethod.invoke(classInstance, args);
		if ((Boolean) (result) == true) {
			return (Tag) classInstance;
		} else {
			return null;
		}
	}

	/**
	 * Fill in ArrayList fields from reflection. For example: in xml we have
	 * "<color " tag. This will look for public ArrayList<Tag_color> colors;
	 * and add by reflection each Tag_color
	 *
	 * @param p
	 */
	public void fillArrayListFieldsFromReflection(XMLStreamReader p) {

		String tag = null;
		if ((tag = haveStartTag(p)) != null) {

			try {
				final String tags = tag + "s";

				// Cache hit the last array, this should speed up our
				// reflection for large arrays
				if (tags.equals(lastArrayName)) {

					lastCounter++;
					Tag subTag = createAndProcessSubTag(p, lastClass);
					if (subTag != null) {
						lastArray.add(subTag);
					}
					return;
				}

				// ---------------------------------------------------------------------------
				// Only allow ArrayList for now....
				//Field f = c.getDeclaredField(tags);
				Field f = getDeclaredField(tags);
				if (f == null) {
					return;
				}
				String theType = f.getType().getName();
				if (theType.equals("java.util.ArrayList")) {

					// Anything of form ClassType<Class, Class, ...>
					Type type = f.getGenericType();
					if (type instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) (type);

						// We just want one thing inside the <>...
						Type[] types = pt.getActualTypeArguments();
						if (types.length == 1) {
							Type insideType = types[0];
							// We know this is unchecked...will cause exception
							@SuppressWarnings("unchecked")
							Class<? extends Tag> theClass = (Class<? extends Tag>) (insideType);

							Tag subTag = createAndProcessSubTag(p, theClass);
							if (subTag != null) {
								// We know this is unchecked...will cause exception
								@SuppressWarnings("unchecked")
								ArrayList<Object> currentArray = (ArrayList<Object>) (f.get(this));
								//currentArray.add(classInstance);
								currentArray.add(subTag);
								// Cache it (because it probably repeats)
								lastArrayName = tags;
								lastArray = currentArray;
								lastClass = theClass;
							}
						}
					}
				}
			} catch (Exception e) {
				// We don't know how to handle this tag...ignore it...
			} finally {
			}
		}
	}

	/**
	 * Fill in Tag_ fields from reflection. For example: in xml we have
	 * "<color " tag. This will look for public Tag_Color color; and add by
	 * reflection
	 *
	 *
	 *
	 *
	 *

	 *
	 * @param p
	 */
	public void fillTagFieldsFromReflection(XMLStreamReader p) {

		String tag = null;
		if ((tag = haveStartTag(p)) != null) {

			try {
				Field f = getDeclaredField(tag);
				if (f == null) {
					return;
				}
				Type t = f.getType();

				// We know this is unchecked...will cause exception
				@SuppressWarnings("unchecked")
				Class<? extends Tag> toMake = (Class<? extends Tag>) (t);

				Tag subTag = createAndProcessSubTag(p, toMake);
				if (subTag != null) {
					f.set(this, subTag);
				}
				// }

			} catch (Exception e) {
				// We don't know how to handle this tag...ignore it...
			} finally {
			}
		}
	}

	// Write routines....
	/**
	 * Write document root to given URL, we are the starting tag...
	 */
	public boolean writeAsRoot(URL aURL) {
		boolean success = false;
		if (aURL != null) {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			try {
				// LOL...java FileURLConnection doesn't support write
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4191800
				URLConnection urlConnection = aURL.openConnection();
				log.info("URL IS " + aURL.toString());
				OutputStream is;
				if (urlConnection instanceof FileURLConnection) {
					File f;
					// Proper way to convert URL to a File
					try {
						f = new File(aURL.toURI());
					} catch (URISyntaxException e) {
						f = new File(aURL.getPath());
					}
					is = new FileOutputStream(f);
				} else {
					is = urlConnection.getOutputStream();
				}
				if (aURL.toString().contains(".gz")) {  // simple hack
					is = new GZIPOutputStream(is);
				}
				BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(
					is));
				XMLStreamWriter p = factory.createXMLStreamWriter(out);

				// indent for readablity...internal class though.
				p = new mIndentingXMLStreamWriter(p);

				success = writeAsRoot(p);


			} catch (Exception ex) {
				log.error("URL EXCEPTION ON WRITE " + ex.toString());
			}
		}
		return success;
	}

	/**
	 * Write this tag as a document root.
	 *
	 * @param p the stream to write to
	 */
	public boolean writeAsRoot(XMLStreamWriter p) {
		try {
			// Write our tag...start..
			p.writeStartDocument();
			p.setPrefix(prefix, prefix);
			writeTag(p);
			p.writeEndDocument();
			p.flush();
		} catch (XMLStreamException ex) {
			log.debug("FAILED TO WRITE " + ex.toString());
		}

		return true;
	}

	public boolean writeTag(XMLStreamWriter p) throws XMLStreamException {
		p.writeStartElement(prefix, tag());

		// Reflection COPIES fields on getDeclaredField, so we cache them
		// for speed.
		if (fields == null) {
			Class<?> c = this.getClass();
			fields = c.getFields();
		}

		ArrayList<Tag> subtags = new ArrayList<Tag>();
		boolean handled = false;
		// Have to write all qualifying fields out as tags....
		int counter = 0;
		for (Field f : fields) {
			counter++;
			String theType = f.getType().getName();
			String name = f.getName();

			// FIXME: check access and be more restrictive on names I think...

			// ---------------------------------------------------------------
			// Handle 'boolean' field type
			// <tag fieldBoolean={yes, no, 1, no }
			if (theType.equals("boolean")) {
				try {
					Boolean aB;
					aB = f.getBoolean(this);
					String out = "0";
					if (aB.booleanValue()) {
						out = "1";
					}
					p.writeAttribute(name, out);
					handled = true;
				} catch (Exception ex) {
					log.error("write boolean tag field " + ex.toString());
				}
				// ---------------------------------------------------------------
				// Handle 'int' field type
				// <tag fieldInteger={0xHex, number }
			} else if (theType.equals("int")) {
				try {
					Integer aI;
					aI = f.getInt(this);
					String out;
					out = aI.toString();
					p.writeAttribute(name, out);
					handled = true;
				} catch (Exception ex) {
					log.error("write integer tag field " + ex.toString());
				}

				// ---------------------------------------------------------------
				// Handle 'float' field type
				// <tag fieldInteger={+-infinity, +-inf, float
			} else if (theType.equals("float")) {

				try {
					Float aF;
					aF = f.getFloat(this);
					String out;
					if (aF == Float.POSITIVE_INFINITY) {
						out = "infinity";
					} else if (aF == Float.NEGATIVE_INFINITY) {
						out = "-infinity";
					} else {
						out = aF.toString();
					}
					p.writeAttribute(name, out);
					handled = true;
				} catch (Exception ex) {
					log.error("write float tag field " + ex.toString());
				}

				// ---------------------------------------------------------------
				// Handle 'double' field type
				// <tag fieldInteger={+-infinity, +-inf, float
			} else if (theType.equals("double")) {
				try {
					Double aD;
					aD = f.getDouble(this);
					String out;
					if (aD == Double.POSITIVE_INFINITY) {
						out = "infinity";
					} else if (aD == Double.NEGATIVE_INFINITY) {
						out = "-infinity";
					} else {
						out = aD.toString();
					}
					p.writeAttribute(name, out);
					handled = true;
				} catch (Exception ex) {
					log.error("write double tag field " + ex.toString());
				}

				// ---------------------------------------------------------------
				// Handle 'string' field type (which is just the xml text)
				// <tag fieldInteger=xmltext
			} else if (theType.equals("java.lang.String")) {
				try {
					String out;
					out = (String) f.get(this);
					if (out == null) {
						out = "";
					}
					p.writeAttribute(name, out);
					handled = true;
				} catch (Exception ex) {
					log.error("write string tag field " + ex.toString());
				}
			} else if (theType.equals("java.util.ArrayList")) {

				try{
					ArrayList<? extends Tag> a = (ArrayList<? extends Tag>)(f.get(this));
					for(Tag t:a){
				            // process these later...
				            // FIXME: might be faster to keep a list of tag arraylists, to avoid
					    // arraylist copying.
                                            subtags.add(t);
					}
				}catch(Exception ex){
					// ArrayList that's not a Tag, what to do?
				}
			} else {

				// Snag the object..see if it's a Tag
				// we can only write it if it's not null
				// Since this is a stream we have to write
				// all our attributes first, so we store
				// to do later
				try {
					Object o = f.get(this);
					if (o != null) {
						if (o instanceof Tag) {

							Tag t = (Tag) (o);
							subtags.add(t);
						}
					}
				} catch (Exception e) {
					// What to do? 
				}
			}

		}

		// Now do the subtags and their attributes, writeAttribute
		// only works for current tag
		for(Tag t:subtags){
			t.writeTag(p);
		}
		p.writeEndElement();
		log.debug("There were a total of " + counter + " fields within tag ");
		return true;
	}
}