package com.webbuilder.tool;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;

public class StringComparator implements Comparator<Object> {
	Collator collator = Collator.getInstance();

	public int compare(Object o1, Object o2) {
		CollationKey key1 = collator.getCollationKey(o1.toString()
				.toLowerCase());
		CollationKey key2 = collator.getCollationKey(o2.toString()
				.toLowerCase());
		return key1.compareTo(key2);
	}
}
