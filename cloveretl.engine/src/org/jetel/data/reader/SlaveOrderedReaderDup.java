package org.jetel.data.reader;

import org.jetel.data.DataRecord;
import org.jetel.data.RecordKey;
import org.jetel.data.RecordOrderedKey;
import org.jetel.graph.InputPort;

public class SlaveOrderedReaderDup extends SlaveReaderDup {

	public SlaveOrderedReaderDup(InputPort inPort, RecordKey key) {
		super(inPort, key);
	}

	public int compare(InputReader other) {
		DataRecord rec1 = getSample();
		DataRecord rec2 = other.getSample();
		if (rec1 == null) {
			return 1;	
		} else if (rec2 == null) {
			return -1;
		}
		return ((RecordOrderedKey)key).compare((RecordOrderedKey)other.getKey(), rec1, rec2);
	}

	
}