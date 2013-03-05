/*
 * We handle more stuff in string than standard JAXB.
 * For example, we allow 0x in integer strings to represent hex,
 * however you have to black box it seems for this extra stuff...
 * FIXME: Anyway to bind adapter to int, float not Integer, Float?
 */
@javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters
({
    @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value=IntegerHexAdapter.class,type=Integer.class),
    @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(value=FloatAdapter.class,type=Float.class)
})
package org.wdssii.xml;

import org.wdssii.xml.Util.FloatAdapter;
import org.wdssii.xml.Util.IntegerHexAdapter;

