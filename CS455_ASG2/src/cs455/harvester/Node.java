package cs455.harvester;

import cs455.harvester.wireformats.Event;

public interface Node {

	public void onEvent(Event e, int id);
	
}
