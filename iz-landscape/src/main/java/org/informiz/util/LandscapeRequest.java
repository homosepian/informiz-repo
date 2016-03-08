package org.informiz.util;

public class LandscapeRequest {
	int informiId;
	int limit;
	
	public LandscapeRequest(int id, int l) {
		this.informiId = id;
		this.limit = l;
	}

	public int getInformiId() {
		return informiId;
	}

	public void setInformiId(int informiId) {
		this.informiId = informiId;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
