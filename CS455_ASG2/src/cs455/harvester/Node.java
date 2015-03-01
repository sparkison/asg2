package cs455.harvester;

import cs455.wireformats.Event;

public interface Node {

	public void onEvent(Event e);
	
}
