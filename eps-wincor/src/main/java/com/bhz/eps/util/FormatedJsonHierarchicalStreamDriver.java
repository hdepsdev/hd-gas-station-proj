package com.bhz.eps.util;

import java.io.Writer;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

public class FormatedJsonHierarchicalStreamDriver extends JsonHierarchicalStreamDriver {
	@Override
	public HierarchicalStreamWriter createWriter(Writer out) {
		return new JsonWriter(out, new JsonWriter.Format(new char[]{}, new char[]{},JsonWriter.Format.COMPACT_EMPTY_ELEMENT));
	}
}
