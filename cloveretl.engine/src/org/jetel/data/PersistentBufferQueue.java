/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.jetel.exception.JetelRuntimeException;
import org.jetel.graph.ContextProvider;
import org.jetel.graph.runtime.IAuthorityProxy;
import org.jetel.util.bytes.CloverBuffer;

/**
 * Regular queue for CloverBuffers backed by temporary files. No data are cached in memory.
 * Only single-thread producer and single-thread consumer is supported.
 * 
 * This queue is not hard drive effective for little populated CloverBuffers, since 
 * even empty CloverBuffers occupies disk space with buffer.capacity() size.
 * 
 * CloverBuffer processed by this queue can grow in the time.
 * 
 * @author Kokon (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created 17.5.2013
 */
public class PersistentBufferQueue {
	
	//private static final Logger logger = Logger.getLogger(PersistentBufferQueue.class);
	
	private static final String TMP_FILE_PREFIX = "pbq";
	// prefix of temporary file generated by system
	private static final String TMP_FILE_SUFFIX = ".tmp";
	// suffix of temporary file generated by system
	private static final String TMP_FILE_MODE = "rw";

	private static final Logger log = Logger.getLogger(DynamicRecordBuffer.class);

    /**
     * For each buffer.capacity() different temporary file is used.
     */
    private LinkedList<TempFile> tempFiles = new LinkedList<TempFile>();

    /**
     * Writes a {@link CloverBuffer} to this queue, this operation should success all the time.
     * An exception can be thrown only if passed buffer has smaller capacity then the previous one.
     * @param buffer written CloverBuffer
     * @return true
     */
    public boolean offer(CloverBuffer buffer) {
		DiskSlot diskSlot = getDiskSlotForWrite(buffer.capacity());
		diskSlot.write(buffer);
		diskSlot.setAsFull();
		return true;
    }

	/**
	 * Find empty slot in temporary file.
	 * @param requestedSlotSize 
	 * @return
	 */
	private synchronized DiskSlot getDiskSlotForWrite(int requestedSlotSize) {
		if (!tempFiles.isEmpty()) {
			TempFile lastTempFile = tempFiles.getLast();
			int lastSlotSize = lastTempFile.getSlotSize();
			if (lastSlotSize == requestedSlotSize) {
				//capacity of new buffer is same with the previous one
				return lastTempFile.getDiskSlotForWrite();
			} else if (lastSlotSize < requestedSlotSize) {
				//new capacity is higher than the previous one
				TempFile newTempFile = createTempFile(requestedSlotSize);
				return newTempFile.getDiskSlotForWrite();
			} else {
				//new capacity cannot be lower then the previous one
				throw new IllegalStateException("requested slot size cannot be descreased");
			}
		} else {
			//first file needs to be created
			TempFile newTempFile = createTempFile(requestedSlotSize);
			return newTempFile.getDiskSlotForWrite();
		}
	}

	private TempFile createTempFile(int requestedSlotSize) {
		TempFile tempFile = new TempFile(requestedSlotSize);
		tempFile.open();
		tempFiles.addLast(tempFile);
		return tempFile;
	}

    /**
     * Tries to read a CloverBuffer from this queue.
     * @param buffer container for read data
     * @return passed buffer of null if no data are available
     */
    public CloverBuffer poll(CloverBuffer buffer) {
     	DiskSlot diskSlot = getDiskSlotForRead();
    	
        if (diskSlot != null) {
            diskSlot.read(buffer);
            diskSlot.setAsFree();
            return buffer;
        } else {
        	return null;
        }
    }

    private synchronized DiskSlot getDiskSlotForRead() {
    	if (!tempFiles.isEmpty()) {
	    	while (true) {
	    		final TempFile tempFile = tempFiles.getFirst();
	    		DiskSlot diskSlot = tempFile.getDiskSlotForRead();
	    		if (diskSlot != null) {
	    			return diskSlot;
	    		} else if (tempFiles.size() > 1) {
	    			tempFiles.removeFirst().close();
	    		} else {
	    			return null;
	    		}
	    	}
    	} else {
    		return null;
    	}
    }

    public void close() {
    	for (TempFile tempFile : tempFiles) {
    		tempFile.close();
    	}
    	tempFiles.clear();
    }
    
    private static class TempFile {
    	private File tempFile;
    	private FileChannel tempFileChannel;
        private final int slotSize;
    	private LinkedList<DiskSlot> emptyFileBuffers;
        private LinkedList<DiskSlot> fullFileBuffers;
        private int lastSlot;

		public TempFile(int slotSize) {
	        emptyFileBuffers = new LinkedList<DiskSlot>();
	        fullFileBuffers=new LinkedList<DiskSlot>();
	        lastSlot = -1;
	        this.slotSize = slotSize;
		}
		
		private void open() {
			try {
				//graph id is part of temp file - just a temporary solution for finding leaking graph
				String graphId = ContextProvider.getGraph().getId();
				tempFile = IAuthorityProxy.getAuthorityProxy(ContextProvider.getGraph()).newTempFile(TMP_FILE_PREFIX + "_" + graphId + "_", TMP_FILE_SUFFIX, -1);
				tempFileChannel = new RandomAccessFile(tempFile, TMP_FILE_MODE).getChannel();
			} catch (Exception e) {
				throw new JetelRuntimeException("Can't open TMP file in", e);
			}
		}

		public void close() {
			try {
				fullFileBuffers = null;
		        emptyFileBuffers = null;
				tempFileChannel.close();
			} catch (IOException e) {
				throw new JetelRuntimeException("TempFile in PersistentBufferQueue cannot be closed.", e);
			} finally {
		        if (!tempFile.delete()) {
		        	log.warn("Failed to delete temp file: " + tempFile.getAbsolutePath());
		        }
			}
		}

		public void reset() {
		    emptyFileBuffers.addAll(fullFileBuffers);
	        fullFileBuffers.clear();
		}

		public final int getSlotSize() {
			return slotSize;
		}
		
		public void write(CloverBuffer cloverBuffer, long position) {
			try {
				tempFileChannel.write(cloverBuffer.buf(), position);
			} catch (IOException e) {
				throw new JetelRuntimeException(e);
			}
		}

		public void read(CloverBuffer cloverBuffer, long position) {
			try {
				tempFileChannel.read(cloverBuffer.buf(), position);
			} catch (IOException e) {
				throw new JetelRuntimeException(e);
			}
		}

		public synchronized DiskSlot getDiskSlotForWrite() {
			DiskSlot diskSlot;
			
			if (emptyFileBuffers.size() > 0) {
				diskSlot = emptyFileBuffers.removeFirst();
			} else {
				diskSlot = new DiskSlot(this, (long) (++lastSlot) * slotSize);
			}
			
			return diskSlot;
		}

		public synchronized void setDiskSlotAsFull(DiskSlot diskSlot) {
			fullFileBuffers.addLast(diskSlot);
		}
		
		public synchronized DiskSlot getDiskSlotForRead() {
			if (!fullFileBuffers.isEmpty()) {
				DiskSlot diskSlot = fullFileBuffers.removeFirst();
				return diskSlot;
			}
			
			return null;
		}
		
		public synchronized void setDiskSlotAsFree(DiskSlot diskSlot) {
			emptyFileBuffers.addFirst(diskSlot);
		}

		public boolean hasData() {
			return !fullFileBuffers.isEmpty();
		}
    }
    
    private static class DiskSlot {
    	final TempFile tempFile;
    	final long offset;
        int usedBytes;
        
        DiskSlot(final TempFile tempFile, long offset) {
        	this.tempFile = tempFile;
        	this.offset = offset;
        }

        public void write(CloverBuffer cloverBuffer) {
			usedBytes = cloverBuffer.limit();
        	tempFile.write(cloverBuffer, offset);
        }

        public void read(CloverBuffer cloverBuffer) {
        	cloverBuffer.clear();
        	cloverBuffer.limit(usedBytes);
        	tempFile.read(cloverBuffer, offset);
        	cloverBuffer.flip();
        }
        
        public void setAsFree() {
        	tempFile.setDiskSlotAsFree(this);
        }
        
        public void setAsFull() {
        	tempFile.setDiskSlotAsFull(this);
        }
    }

}
