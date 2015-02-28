package cs455.harvester.wireformats;

public class EventFactory {
	// Singleton instance
	private static EventFactory instance = null;

	// Exists only to defeat instantiation
	protected EventFactory() {
	}

	// Get instance of TaskFactory
	public static EventFactory getInstance() {
		if (instance == null) {
			instance = new EventFactory();
		}
		return instance;
	}
}
