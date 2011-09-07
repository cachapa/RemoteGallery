package net.cachapa.remotegallery.util;

import java.util.Comparator;

public class DirEntryComparator implements Comparator<DirEntry> {
		private boolean reverseDirOrder;
		
		public DirEntryComparator(boolean reverseDirOrder) {
			this.reverseDirOrder = reverseDirOrder;
		}
		
		@Override
		public int compare(DirEntry entry1, DirEntry entry2) {
			if (entry1.isDirectory && !entry2.isDirectory) {
				return -1;
			}
			if (!entry1.isDirectory && entry2.isDirectory) {
				return 1;
			}
			if (entry1.isDirectory && entry2.isDirectory && reverseDirOrder) {
				return entry2.name.compareTo(entry1.name);
			}
			return entry1.name.toLowerCase().compareTo(entry2.name.toLowerCase());
		}
	}